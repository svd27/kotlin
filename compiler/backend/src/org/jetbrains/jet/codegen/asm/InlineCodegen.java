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

package org.jetbrains.jet.codegen.asm;

import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.MethodInsnNode;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.asm4.tree.VarInsnNode;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.NamespaceContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.getMethodAsmFlags;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFQName;

public class InlineCodegen implements ParentCodegenAware, Inliner {

    private final static int API = Opcodes.ASM4;

    public static final String INLINE_RUNTIME = "jet/InlineRuntime";

    public static final String INVOKE = "invoke";

    private final ExpressionCodegen codegen;

    private final boolean notSeparateInline;

    private final GenerationState state;

    private final boolean disabled;

    private final SimpleFunctionDescriptor functionDescriptor;

    private final List<Type> tempTypes = new ArrayList<Type>();

    private final List<ClosureUsage> closures = new ArrayList<ClosureUsage>();

    private final Map<Integer, ClosureInfo> expressionMap = new HashMap<Integer, ClosureInfo>();

    private final JetTypeMapper typeMapper;

    private final BindingContext bindingContext;

    private final MethodContext context;

    private final FrameMap originalFunctionFrame;

    private final int initialFrameSize;

    @Nullable
    public static MethodNode getMethodNode(
            InputStream classData,
            final String methodName,
            final String methodDescriptor
    ) throws ClassNotFoundException, IOException {
        ClassReader cr = new ClassReader(classData);
        final MethodNode[] methodNode = new MethodNode[1];
        cr.accept(new ClassVisitor(API) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (methodName.equals(name) && methodDescriptor.equals(desc)) {
                    return methodNode[0] = new MethodNode(access, name, desc, signature, exceptions);
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return methodNode[0];
    }

    public InlineCodegen(ExpressionCodegen codegen, boolean notSeparateInline, GenerationState state, boolean disabled, SimpleFunctionDescriptor functionDescriptor) {
        this.codegen = codegen;
        this.notSeparateInline = notSeparateInline;
        this.state = state;
        this.disabled = disabled;
        this.functionDescriptor = functionDescriptor;
        typeMapper = codegen.getTypeMapper();
        bindingContext = codegen.getBindingContext();
        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        context = (MethodContext) getContext(functionDescriptor, state);
        originalFunctionFrame = new FrameMap();
    }


    @Override
    public void inlineCall(CallableMethod callableMethod, ClassVisitor visitor) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, functionDescriptor);
        VirtualFile file = null;
        if (element == null) {
            file = findVirtualFileContainingDescriptor(state.getProject(), functionDescriptor);
        } else {
            file = element.getContainingFile().getVirtualFile();
        }

        boolean isSources = !file.getExtension().equalsIgnoreCase("class");

        MethodNode node = null;

        try {
            if (isSources) {
                JvmMethodSignature jvmSignature = typeMapper.mapSignature(functionDescriptor, true, context.getContextKind());
                Method asmMethod = jvmSignature.getAsmMethod();
                node = new MethodNode(Opcodes.ASM4,
                                               getMethodAsmFlags(functionDescriptor, context.getContextKind()),
                                               asmMethod.getName(),
                                               asmMethod.getDescriptor(),
                                               jvmSignature.getGenericsSignature(),
                                               null);

                FunctionCodegen functionCodegen = new FunctionCodegen(context, null, state, getParentCodegen());
                functionCodegen.generateMethodBody(node, functionDescriptor, context.getParentContext().intoFunction(functionDescriptor),
                                                   jvmSignature,
                                                   new FunctionGenerationStrategy.FunctionDefault(state,
                                                                                                  functionDescriptor,
                                                                                                  (JetDeclarationWithBody) element));
            }
            else {

                    node = getMethodNode(file.getInputStream(), functionDescriptor.getName().asString(),
                                         callableMethod.getSignature().getAsmMethod().getDescriptor());

            }
        }
        catch (Exception e) {
            throw new RuntimeException("Coudn't inline method call " +
                                       functionDescriptor.getName() +
                                       " in " + codegen.getContext().getCallableDescriptorWithReceiver().getName() +
                                       " cause: " +
                                       e.getMessage(), e);
        }
        inlineCall(node, true);
    }

    private void inlineCall(MethodNode node, boolean inlineClosures) {
        if (inlineClosures) {
            AbstractInsnNode cur = node.instructions.getFirst();
            while (cur != null && cur.getNext() != null) {
                AbstractInsnNode next = cur.getNext();
                if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                    //TODO check closure
                    if (methodInsnNode.name.equals(INVOKE) && methodInsnNode.owner.equals(INLINE_RUNTIME)) {
                        //cur is closure aload
                        assert cur.getType() == AbstractInsnNode.VAR_INSN && cur.getOpcode() == Opcodes.ALOAD;
                        int varIndex = ((VarInsnNode) cur).var;
                        ClosureInfo closureInfo = expressionMap.get(varIndex);
                        if (closureInfo != null) { //TODO: maybe add separate map for noninlinable closures
                            closures.add(new ClosureUsage(varIndex, true));
                            node.instructions.remove(cur);
                        } else {
                            closures.add(new ClosureUsage(varIndex, false));
                        }
                    }
                    if (methodInsnNode.name.equals("checkParameterIsNotNull") && methodInsnNode.owner.equals("jet/runtime/Intrinsics")) {
                        AbstractInsnNode prev = cur.getPrevious();
                        assert prev.getType() == AbstractInsnNode.VAR_INSN && prev.getOpcode() == Opcodes.ALOAD;
                        int varIndex = ((VarInsnNode) prev).var;
                        ClosureInfo closure = expressionMap.get(varIndex);
                        if (closure != null) {
                            node.instructions.remove(prev);
                            node.instructions.remove(cur);
                            cur = next.getNext();
                            node.instructions.remove(next);
                            next = cur;
                        }
                    }
                }
                cur = next;
            }
        }

        int valueParamSize = originalFunctionFrame.getCurrentSize();
        int originalSize = codegen.getFrameMap().getCurrentSize();
        generateClosuresBodies();
        putClosureParametersOnStack();
        int additionalParams = codegen.getFrameMap().getCurrentSize() - originalSize;
        VarRemapper remapper = new VarRemapper.DeltaRemapper(initialFrameSize, valueParamSize, additionalParams, getClosureIndexes());

        doInline(node.access, node.desc, codegen.getMethodVisitor(), node, remapper.doRemap(initialFrameSize + valueParamSize + additionalParams), inlineClosures, remapper);
    }

    private void doInline(
            int access,
            String desc,
            MethodVisitor mv,
            MethodNode methodNode,
            final int frameSize,
            final boolean inlineClosures,
            VarRemapper remapper
    ) {

        Label end = new Label();

        final LinkedList<ClosureUsage> infos = new LinkedList<ClosureUsage>(closures);
        methodNode.instructions.resetLabels();
        final MethodVisitor methodVisitor = codegen.getMethodVisitor();

        InliningAdapter inliner = new InliningAdapter(methodVisitor, Opcodes.ASM4, desc, end, frameSize, remapper) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                if (inlineClosures && INLINE_RUNTIME.equals(owner) && INVOKE.equals(name)) { //TODO add method
                    assert !infos.isEmpty();
                    ClosureUsage closureUsage = infos.remove();
                    ClosureInfo info = expressionMap.get(closureUsage.index);

                    if (!closureUsage.isInlinable()) {
                        //noninlinable closure
                        super.visitMethodInsn(opcode, owner, name, desc);
                        return;
                    }

                    //TODO replace with codegen
                    int valueParamShift = getNextLocalIndex();
                    remapper.setNestedRemap(true);
                    putStackValuesIntoLocals(info.getValuesParams(), valueParamShift, this, desc);
                    Label closureEnd = new Label();
                    InliningAdapter closureInliner = new InliningAdapter(mv, Opcodes.ASM4, desc, closureEnd, getNextLocalIndex(),
                                                              new VarRemapper.ClosureRemapper(info, valueParamShift));
                    info.getNode().instructions.accept(closureInliner); //TODO
                    remapper.setNestedRemap(false);
                    mv.visitLabel(closureEnd);

                    Method bridge = typeMapper.mapSignature(ClosureCodegen.getInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), closureInliner);
                }
                else {
                    super.visitMethodInsn(opcode, owner, name, desc);
                }
            }
        };



        methodNode.instructions.accept(inliner);

        methodVisitor.visitLabel(end);
    }

    private Collection<Integer> getClosureIndexes() {
        return expressionMap.keySet();
    }

    private void generateClosuresBodies() {
        for (Iterator<ClosureInfo> iterator = expressionMap.values().iterator(); iterator.hasNext(); ) {
            ClosureInfo info = iterator.next();
            info.setNode(generateClosureBody(info));
        }
    }

    private MethodNode generateClosureBody(ClosureInfo info) {
        JetFunctionLiteral declaration = info.getFunctionLiteral();
        FunctionDescriptor descriptor = info.getFunctionDescriptor();

        FunctionCodegen functionCodegen = new FunctionCodegen(codegen.getContext(), null, codegen.getState(), codegen.getParentCodegen());

        MethodContext context = codegen.getContext().intoClosure(descriptor, codegen, typeMapper).intoFunction(descriptor);

        JvmMethodSignature jvmMethodSignature = typeMapper.mapSignature(descriptor);
        Method asmMethod = jvmMethodSignature.getAsmMethod();
        MethodNode methodNode = new MethodNode(Opcodes.ASM4, getMethodAsmFlags(descriptor, context.getContextKind()), asmMethod.getName(), asmMethod.getDescriptor(), jvmMethodSignature.getGenericsSignature(), null);

        functionCodegen.generateMethodBody(methodNode, descriptor, context, jvmMethodSignature, new FunctionGenerationStrategy.FunctionDefault(state, descriptor, declaration) {
            @Override
            public boolean generateLocalVarTable() {
                return false;
            }
        });

        return transformClosure(methodNode, info);
    }

    private MethodNode transformClosure(@NotNull MethodNode node, @NotNull final ClosureInfo info) {
        //remove all this and shift all variables to captured ones size
        final int localVarSHift = info.getCapturedVarsSize();
        MethodNode transformedNode = new MethodNode(node.access, node.name, node.desc, node.signature, null) {

            private boolean remappingCaptured = false;
            @Override
            public void visitVarInsn(int opcode, int var) {
                if (!remappingCaptured && var == 0) {
                    return; //skip this - there is no object
                }
                super.visitVarInsn(opcode, var + (remappingCaptured ?  0 : localVarSHift - 1/*remove this*/));
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (info.getClosureClassType().getInternalName().equals(owner)) {
                    assert opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD;

                    Collection<EnclosedValueDescriptor> vars = info.getCapturedVars();
                    int index = 0;
                    for (Iterator<EnclosedValueDescriptor> iterator = vars.iterator(); iterator.hasNext(); ) {
                        EnclosedValueDescriptor valueDescriptor = iterator.next();
                        Type type = valueDescriptor.getType();
                        if (valueDescriptor.getFieldName().equals(name)) {
                            remappingCaptured = true;
                            visitVarInsn(opcode == Opcodes.GETFIELD ?  type.getOpcode(Opcodes.ILOAD) : type.getOpcode(Opcodes.ISTORE), index);
                            remappingCaptured = false;
                            return;
                        }
                        index += type.getSize();
                    }
                    throw new UnsupportedOperationException();
                }

                super.visitFieldInsn(opcode, owner, name, desc);
            }
        };
        node.accept(transformedNode);

        return transformedNode;
    }

    @Override
    public void putInLocal(Type type) {
        if (!disabled && notSeparateInline && Type.VOID_TYPE != type) {
            int index = codegen.getFrameMap().enterTemp(type);
            StackValue.local(index, type).store(type, codegen.getInstructionAdapter());
            tempTypes.add(type);
            originalFunctionFrame.enterTemp(type);
        }
    }

    @Override
    public void leaveTemps() {
        FrameMap frameMap = codegen.getFrameMap();
        for (ListIterator<Type> iterator = tempTypes.listIterator(tempTypes.size()); iterator.hasPrevious(); ) {
            Type type = iterator.previous();
            frameMap.leaveTemp(type);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    private static void putStackValuesIntoLocals(List<Type> directOrder, int shift, InstructionAdapter mv, String descriptor) {
        Type [] actualParams = Type.getArgumentTypes(descriptor); //last param is closure itself
        assert actualParams.length - 1 == directOrder.size() : "Number of expected and actual params should be equals!";

        int size = 0;
        for (Iterator<Type> iterator = directOrder.iterator(); iterator.hasNext(); ) {
            Type next = iterator.next();
            size += next.getSize();
        }

        shift += size;
        int index = directOrder.size();

        for (Iterator<Type> iterator = Lists.reverse(directOrder).iterator(); iterator.hasNext(); ) {
            Type next = iterator.next();
            shift -= next.getSize();
            Type typeOnStack = actualParams[--index];
            if (!typeOnStack.equals(next)) {
                StackValue.onStack(typeOnStack).put(next, mv);
            }
            mv.visitVarInsn(next.getOpcode(Opcodes.ISTORE), shift);
        }
    }

    @Override
    public boolean isInliningClosure(JetExpression expression) {
        return !disabled && expression instanceof JetFunctionLiteralExpression;
    }

    @Override
    public void rememberClosure(JetFunctionLiteralExpression expression, Type type) {
        int index = originalFunctionFrame.enterTemp(type);
        ClosureInfo info = new ClosureInfo(expression, typeMapper);
        expressionMap.put(index, info);
    }

    private void putClosureParametersOnStack() {
        //SORT
        for (Iterator<ClosureInfo> iterator = expressionMap.values().iterator(); iterator.hasNext(); ) {
            ClosureInfo next = iterator.next();
            if (next.closure != null) {
                next.setCapturedVarsOffset(codegen.getFrameMap().getCurrentSize());
                codegen.pushClosureOnStack(next.closure, true, this);
            }
        }
    }

    @Nullable
    @Override
    public MemberCodegen getParentCodegen() {
        return codegen.getParentCodegen();
    }

    private static class ClosureUsage {

        private final int index;

        private final boolean inlinable;

        private ClosureUsage(int index, boolean isInlinable) {
            this.index = index;
            inlinable = isInlinable;
        }

        public boolean isInlinable() {
            return inlinable;
        }
    }

    @Nullable
    private static VirtualFile findVirtualFileContainingDescriptor(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        FqName containerFqName = getContainerFqName(referencedDescriptor);
        if (containerFqName == null) {
            return null;
        }
        VirtualFileFinder fileFinder = ServiceManager.getService(project, VirtualFileFinder.class);
        VirtualFile virtualFile = fileFinder.find(containerFqName);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile;
    }

    //TODO: navigate to inner classes
    @Nullable
    private static FqName getContainerFqName(@NotNull DeclarationDescriptor referencedDescriptor) {
        ClassOrNamespaceDescriptor
                containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrNamespaceDescriptor.class, false);
        if (containerDescriptor instanceof NamespaceDescriptor) {
            return PackageClassUtils.getPackageClassFqName(getFQName(containerDescriptor).toSafe());
        }
        if (containerDescriptor instanceof ClassDescriptor) {
            ClassKind classKind = ((ClassDescriptor) containerDescriptor).getKind();
            if (classKind == ClassKind.CLASS_OBJECT || classKind == ClassKind.ENUM_ENTRY) {
                return getContainerFqName(containerDescriptor.getContainingDeclaration());
            }
            return getFQName(containerDescriptor).toSafe();
        }
        return null;
    }

    public CodegenContext getContext(DeclarationDescriptor descriptor, GenerationState state) {

        if (descriptor instanceof NamespaceDescriptor) {
            return new NamespaceContext((NamespaceDescriptor) descriptor, null);
        }

        CodegenContext parent = getContext(descriptor.getContainingDeclaration(), state);

        if (descriptor instanceof ClassDescriptor) {
            return parent.intoClass((ClassDescriptor) descriptor, OwnerKind.IMPLEMENTATION, state);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return parent.intoFunction((FunctionDescriptor) descriptor);
        }

        return null;
    }
}
