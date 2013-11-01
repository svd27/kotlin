/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.descriptors.serialization.descriptors;

import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.NullableLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.*;

import static org.jetbrains.jet.descriptors.serialization.TypeDeserializer.TypeParameterResolver.NONE;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;

public class DeserializedClassDescriptor extends AbstractClassDescriptor implements ClassDescriptor {

    private final ClassId classId;
    private final ProtoBuf.Class classProto;
    private final StorageManager storageManager;
    private final TypeDeserializer typeDeserializer;
    private final DescriptorDeserializer deserializer;
    private final DeserializedMemberScope memberScope;
    private final ReceiverParameterDescriptor thisAsReceiverParameter;

    private final NullableLazyValue<ConstructorDescriptor> primaryConstructor;

    private final AnnotationDeserializer annotationDeserializer;
    private final NotNullLazyValue<List<AnnotationDescriptor>> annotations;

    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final NestedClassDescriptors nestedClasses;

    private final NotNullLazyValue<DeclarationDescriptor> containingDeclaration;
    private final DeserializedClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;
    private final InnerClassesScopeWrapper innerClassesScope;
    private final DescriptorFinder descriptorFinder;

    public DeserializedClassDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull AnnotationDeserializer annotationResolver,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull NameResolver nameResolver,
            @NotNull ProtoBuf.Class classProto
    ) {
        super(nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName());
        this.classProto = classProto;
        this.classId = nameResolver.getClassId(classProto.getFqName());
        this.storageManager = storageManager;
        this.descriptorFinder = descriptorFinder;

        TypeDeserializer notNullTypeDeserializer = new TypeDeserializer(storageManager, null, nameResolver,
                                                                        descriptorFinder, "Deserializer for class " + name, NONE);
        DescriptorDeserializer outerDeserializer = DescriptorDeserializer.create(storageManager, notNullTypeDeserializer,
                                                                                 this, nameResolver, annotationResolver);
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(classProto.getTypeParameterCount());
        this.deserializer = outerDeserializer.createChildDeserializer(this, classProto.getTypeParameterList(), typeParameters);
        this.typeDeserializer = deserializer.getTypeDeserializer();

        this.containingDeclaration = storageManager.createLazyValue(new Function0<DeclarationDescriptor>() {
            @Override
            public DeclarationDescriptor invoke() {
                return computeContainingDeclaration();
            }
        });

        this.typeConstructor = new DeserializedClassTypeConstructor(typeParameters);
        this.memberScope = new DeserializedClassMemberScope(storageManager, this);
        this.innerClassesScope = new InnerClassesScopeWrapper(memberScope);
        this.thisAsReceiverParameter = new LazyClassReceiverParameterDescriptor();

        int flags = classProto.getFlags();
        this.modality = DescriptorDeserializer.modality(Flags.MODALITY.get(flags));
        this.visibility = DescriptorDeserializer.visibility(Flags.VISIBILITY.get(flags));
        this.kind = DescriptorDeserializer.classKind(Flags.CLASS_KIND.get(flags));
        this.isInner = Flags.INNER.get(flags);

        this.annotationDeserializer = annotationResolver;
        this.annotations = storageManager.createLazyValue(new Function0<List<AnnotationDescriptor>>() {
            @Override
            public List<AnnotationDescriptor> invoke() {
                return computeAnnotations();
            }
        });

        this.primaryConstructor = storageManager.createNullableLazyValue(new Function0<ConstructorDescriptor>() {
            @Override
            public ConstructorDescriptor invoke() {
                return computePrimaryConstructor();
            }
        });

        this.classObjectDescriptor = storageManager.createNullableLazyValue(new Function0<ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke() {
                return computeClassObjectDescriptor();
            }
        });

        this.nestedClasses = new NestedClassDescriptors();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration.invoke();
    }

    @NotNull
    private DeclarationDescriptor computeContainingDeclaration() {
        ClassOrNamespaceDescriptor result = classId.isTopLevelClass() ?
                                            descriptorFinder.findPackage(classId.getPackageFqName()) :
                                            descriptorFinder.findClass(classId.getOuterClassId());
        return result != null ? result : ErrorUtils.getErrorModule();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }


    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    private List<AnnotationDescriptor> computeAnnotations() {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
            return Collections.emptyList();
        }
        return annotationDeserializer.loadClassAnnotations(this, classProto);
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations.invoke();
    }

    @Override
    protected JetScope getScopeForMemberLookup() {
        return memberScope;
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return innerClassesScope;
    }

    @Nullable
    private ConstructorDescriptor computePrimaryConstructor() {
        if (!classProto.hasPrimaryConstructor()) return null;

        ProtoBuf.Class.PrimaryConstructor constructorProto = classProto.getPrimaryConstructor();
        if (!constructorProto.hasData()) {
            ConstructorDescriptorImpl descriptor = DescriptorFactory.createPrimaryConstructorForObject(this);
            descriptor.setReturnType(getDefaultType());
            return descriptor;
        }

        return (ConstructorDescriptor) deserializer.loadCallable(constructorProto.getData());
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor.invoke();
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        ConstructorDescriptor constructor = getUnsubstitutedPrimaryConstructor();
        if (constructor == null) {
            return Collections.emptyList();
        }
        // TODO: other constructors
        return Collections.singletonList(constructor);
    }

    @Nullable
    @Override
    public JetType getClassObjectType() {
        ClassDescriptor classObjectDescriptor = getClassObjectDescriptor();
        return classObjectDescriptor == null ? null : classObjectDescriptor.getDefaultType();
    }

    @Nullable
    private ClassDescriptor computeClassObjectDescriptor() {
        if (!classProto.hasClassObject()) {
            return null;
        }

        if (getKind() == ClassKind.ENUM_CLASS) {
            return createEnumClassObject();
        }

        if (getKind() == ClassKind.OBJECT || getKind() == ClassKind.ENUM_ENTRY) {
            ProtoBuf.Class.ClassObject classObjectProto = classProto.getClassObject();
            if (!classObjectProto.hasData()) {
                throw new IllegalStateException("Object or enum entry should have a serialized class object: " + classId);
            }

            return new DeserializedClassDescriptor(storageManager, annotationDeserializer, descriptorFinder, deserializer.getNameResolver(),
                                                   classObjectProto.getData());
        }

        return descriptorFinder.findClass(classId.createNestedClassId(getClassObjectName(getName())));
    }

    @NotNull
    private MutableClassDescriptor createEnumClassObject() {
        MutableClassDescriptor classObject = new MutableClassDescriptor(this, getScopeForMemberLookup(), ClassKind.CLASS_OBJECT,
                                                                        false, getClassObjectName(getName()));
        classObject.setModality(Modality.FINAL);
        classObject.setVisibility(getVisibility());
        classObject.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classObject.createTypeConstructor();

        ConstructorDescriptorImpl primaryConstructor = DescriptorFactory.createPrimaryConstructorForObject(classObject);
        primaryConstructor.setReturnType(classObject.getDefaultType());
        classObject.setPrimaryConstructor(primaryConstructor);

        JetType defaultType = getDefaultType();
        JetType defaultTypeArray = KotlinBuiltIns.getInstance().getArrayType(defaultType);
        classObject.getBuilder().addFunctionDescriptor(
                DescriptorFactory.createEnumClassObjectValuesMethod(classObject, defaultTypeArray));
        classObject.getBuilder().addFunctionDescriptor(
                DescriptorFactory.createEnumClassObjectValueOfMethod(classObject, defaultType));

        return classObject;
    }

    @Nullable
    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor.invoke();
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter;
    }

    private Collection<JetType> computeSuperTypes() {
        List<JetType> supertypes = new ArrayList<JetType>(classProto.getSupertypeCount());
        for (ProtoBuf.Type supertype : classProto.getSupertypeList()) {
            supertypes.add(typeDeserializer.type(supertype));
        }
        return supertypes;
    }

    @Override
    public String toString() {
        return "deserialized class " + getName().toString();
    }

    private class DeserializedClassTypeConstructor implements TypeConstructor {
        private final Collection<JetType> supertypes = computeSuperTypes();
        private final List<TypeParameterDescriptor> parameters;

        public DeserializedClassTypeConstructor(@NotNull List<TypeParameterDescriptor> typeParameters) {
            parameters = typeParameters;
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters;
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes;
        }

        @Override
        public boolean isFinal() {
            return !getModality().isOverridable();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Nullable
        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return DeserializedClassDescriptor.this;
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList(); // TODO
        }

        @Override
        public String toString() {
            return getName().toString();
        }
    }

    private static class DeserializedClassMemberScope extends DeserializedMemberScope {
        private final DeserializedClassDescriptor classDescriptor;

        public DeserializedClassMemberScope(@NotNull StorageManager storageManager, @NotNull DeserializedClassDescriptor classDescriptor) {
            super(storageManager, classDescriptor, classDescriptor.deserializer, classDescriptor.classProto.getMemberList());
            this.classDescriptor = classDescriptor;
        }

        @Override
        protected void computeNonDeclaredFunctions(
                @NotNull Name name, @NotNull Collection<FunctionDescriptor> functions
        ) {
            Collection<FunctionDescriptor> fromSupertypes = new ArrayList<FunctionDescriptor>();
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name));
            }
            generateFakeOverrides(name, fromSupertypes, functions);
        }

        @Override
        protected void computeNonDeclaredProperties(
                @NotNull Name name, @NotNull Collection<PropertyDescriptor> property
        ) {
            Collection<PropertyDescriptor> fromSupertypes = new ArrayList<PropertyDescriptor>();
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                //noinspection unchecked
                fromSupertypes.addAll((Collection) supertype.getMemberScope().getProperties(name));
            }
            generateFakeOverrides(name, fromSupertypes, property);
        }

        private <D extends CallableMemberDescriptor> void generateFakeOverrides(
                @NotNull Name name,
                @NotNull Collection<D> fromSupertypes,
                @NotNull final Collection<D> result
        ) {
            List<CallableMemberDescriptor> fromCurrent = new ArrayList<CallableMemberDescriptor>(result);
            OverridingUtil.generateOverridesInFunctionGroup(
                    name,
                    fromSupertypes,
                    fromCurrent,
                    classDescriptor,
                    new OverridingUtil.DescriptorSink() {
                        @Override
                        public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new OverridingUtil.NotInferredVisibilitySink() {
                                @Override
                                public void cannotInferVisibility(@NotNull CallableMemberDescriptor descriptor) {
                                    // Do nothing
                                    // TODO: do something
                                }
                            });
                            //noinspection unchecked
                            result.add((D) fakeOverride);
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            // TODO report conflicts
                        }
                    }
            );
        }

        @Override
        protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor instanceof FunctionDescriptor) {
                        result.addAll(getFunctions(descriptor.getName()));
                    }
                    else if (descriptor instanceof PropertyDescriptor) {
                        result.addAll(getProperties(descriptor.getName()));
                    }
                    // Nothing else is inherited
                }
            }
        }

        @Nullable
        @Override
        protected ReceiverParameterDescriptor getImplicitReceiver() {
            return classDescriptor.getThisAsReceiverParameter();
        }

        @Nullable
        @Override
        protected ClassifierDescriptor getClassDescriptor(@NotNull Name name) {
            return classDescriptor.nestedClasses.findClass.invoke(name);
        }

        @Override
        protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
            result.addAll(classDescriptor.nestedClasses.getAllDescriptors());
        }
    }

    private class NestedClassDescriptors {
        private final Set<Name> nestedClassNames;
        private final Map<Name, NotNullLazyValue<ClassDescriptor>> enumEntries;
        private final MemoizedFunctionToNullable<Name, ClassDescriptor> findClass;

        public NestedClassDescriptors() {
            this.nestedClassNames = new HashSet<Name>();
            for (Integer index : classProto.getNestedClassNameList()) {
                this.nestedClassNames.add(deserializer.getNameResolver().getName(index));
            }

            this.enumEntries = computeEnumEntries();

            this.findClass = storageManager.createMemoizedFunctionWithNullableValues(new Function1<Name, ClassDescriptor>() {
                @Override
                public ClassDescriptor invoke(Name name) {
                    if (nestedClassNames.contains(name)) {
                        return descriptorFinder.findClass(classId.createNestedClassId(name));
                    }
                    NotNullLazyValue<ClassDescriptor> enumEntry = enumEntries.get(name);
                    if (enumEntry != null) {
                        return enumEntry.invoke();
                    }
                    return null;
                }
            });
        }

        @NotNull
        private Map<Name, NotNullLazyValue<ClassDescriptor>> computeEnumEntries() {
            if (classProto.getEnumEntryCount() == 0) {
                return Collections.emptyMap();
            }

            Map<Name, NotNullLazyValue<ClassDescriptor>> enumEntries = new LinkedHashMap<Name, NotNullLazyValue<ClassDescriptor>>();
            for (ProtoBuf.Class.EnumEntry enumEntryProto : classProto.getEnumEntryList()) {
                final Name entryName = deserializer.getNameResolver().getName(enumEntryProto.getName());

                NotNullLazyValue<ClassDescriptor> lazyValue;
                if (enumEntryProto.hasData()) {
                    // This is a trivial enum entry and its serialized data is right here
                    final ProtoBuf.Class enumEntryData = enumEntryProto.getData();
                    lazyValue = storageManager.createLazyValue(new Function0<ClassDescriptor>() {
                        @Override
                        public ClassDescriptor invoke() {
                            return new DeserializedClassDescriptor(storageManager, annotationDeserializer, descriptorFinder,
                                                                   deserializer.getNameResolver(), enumEntryData);
                        }
                    });
                }
                else {
                    // This is a non-trivial enum entry and its data is in a separate class, searchable via DescriptorFinder
                    lazyValue = storageManager.createLazyValue(new Function0<ClassDescriptor>() {
                        @Override
                        public ClassDescriptor invoke() {
                            return descriptorFinder.findClass(classId.createNestedClassId(entryName));
                        }
                    });
                }

                enumEntries.put(entryName, lazyValue);
            }

            return enumEntries;
        }

        @NotNull
        public Collection<ClassDescriptor> getAllDescriptors() {
            Collection<ClassDescriptor> result = new ArrayList<ClassDescriptor>(nestedClassNames.size() + enumEntries.size());
            for (Name name : nestedClassNames) {
                ClassDescriptor descriptor = findClass.invoke(name);
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
            for (Name enumEntry : enumEntries.keySet()) {
                ClassDescriptor descriptor = findClass.invoke(enumEntry);
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
            return result;
        }
    }

    private class LazyClassReceiverParameterDescriptor extends AbstractReceiverParameterDescriptor {
        private final ClassReceiver classReceiver = new ClassReceiver(DeserializedClassDescriptor.this);

        @NotNull
        @Override
        public JetType getType() {
            return getDefaultType();
        }

        @NotNull
        @Override
        public ReceiverValue getValue() {
            return classReceiver;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return DeserializedClassDescriptor.this;
        }
    }
}

