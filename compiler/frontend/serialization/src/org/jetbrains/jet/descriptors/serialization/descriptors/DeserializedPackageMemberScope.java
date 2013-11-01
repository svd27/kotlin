package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;

public class DeserializedPackageMemberScope extends DeserializedMemberScope {
    private final DescriptorFinder descriptorFinder;

    private final FqName packageFqName;

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull ProtoBuf.Package proto,
            @NotNull NameResolver nameResolver
    ) {
        super(storageManager, packageDescriptor,
              DescriptorDeserializer.create(storageManager, packageDescriptor, nameResolver, descriptorFinder, annotationDeserializer),
              proto.getMemberList());
        this.descriptorFinder = descriptorFinder;
        this.packageFqName = DescriptorUtils.getFQName(packageDescriptor).toSafe();
    }

    public DeserializedPackageMemberScope(
            @NotNull StorageManager storageManager,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull AnnotationDeserializer annotationDeserializer,
            @NotNull DescriptorFinder descriptorFinder,
            @NotNull PackageData packageData
    ) {
        this(storageManager, packageDescriptor, annotationDeserializer, descriptorFinder, packageData.getPackageProto(),
             packageData.getNameResolver());
    }

    @Nullable
    @Override
    protected ClassifierDescriptor getClassDescriptor(@NotNull Name name) {
        return findClassDescriptor(name);
    }

    @Nullable
    private ClassDescriptor findClassDescriptor(@NotNull Name name) {
        return descriptorFinder.findClass(new ClassId(packageFqName, FqNameUnsafe.topLevel(name)));
    }

    @Override
    protected void addAllClassDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        for (Name className : descriptorFinder.getClassNames(packageFqName)) {
            ClassDescriptor classDescriptor = findClassDescriptor(className);
            if (classDescriptor != null) {
                result.add(classDescriptor);
            }
        }
    }

    @Override
    protected void addNonDeclaredDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // Do nothing
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return descriptorFinder.findPackage(packageFqName.child(name));
    }

    @Nullable
    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return null;
    }
}
