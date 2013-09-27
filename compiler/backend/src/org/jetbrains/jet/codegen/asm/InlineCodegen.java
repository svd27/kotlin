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
import org.jetbrains.asm4.tree.analysis.*;
import org.jetbrains.asm4.util.Textifier;
import org.jetbrains.asm4.util.TraceMethodVisitor;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.NamespaceContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
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
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.*;
import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.*;
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

    private final List<ParameterInfo> tempTypes = new ArrayList<ParameterInfo>();

    private final List<ClosureUsage> closures = new ArrayList<ClosureUsage>();

    private final Map<Integer, ClosureInfo> expressionMap = new HashMap<Integer, ClosureInfo>();

    private final JetTypeMapper typeMapper;

    private final BindingContext bindingContext;

    private final MethodContext context;

    private final FrameMap originalFunctionFrame;

    private final int initialFrameSize;

    private final JvmMethodSignature jvmSignature;

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
        this.functionDescriptor = functionDescriptor.getOriginal();
        typeMapper = codegen.getTypeMapper();
        bindingContext = codegen.getBindingContext();
        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        context = (MethodContext) getContext(functionDescriptor, state);
        originalFunctionFrame = context.prepareFrame(typeMapper);
        jvmSignature = typeMapper.mapSignature(functionDescriptor, true, context.getContextKind());
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
                node.visitMaxs(10, 10);
                node.visitEnd();
            }
            else {
                node = getMethodNode(file.getInputStream(), functionDescriptor.getName().asString(),
                                     callableMethod.getSignature().getAsmMethod().getDescriptor());
            }
        }
        catch (Exception e) {
            StringWriter sw = null;
            if (node != null) {
                Textifier p = new Textifier();
                node.accept(new TraceMethodVisitor(p));
                sw = new StringWriter();
                p.print(new PrintWriter(sw));
                sw.flush();
            }
            throw new RuntimeException("Coudn't inline method call '" +
                                       functionDescriptor.getName() +
                                       "' into \n" + BindingContextUtils.descriptorToDeclaration(bindingContext, codegen.getContext().getContextDescriptor()).getText() +
                                       "\ncause: " +
                                       sw.getBuffer().toString(), e);
        }
        inlineCall(node, true);
    }

    private void inlineCall(MethodNode node, boolean inlineClosures) {
        if (inlineClosures) {
            removeClosureAssertions(node);
            try {
                prepareInlinedMethod2(node);
            }
            catch (AnalyzerException e) {
                throw new RuntimeException(e);
            }
            //prepareInlinedMethod(node);
        }

        int valueParamSize = originalFunctionFrame.getCurrentSize();
        int originalSize = codegen.getFrameMap().getCurrentSize();
        generateClosuresBodies();
        putClosureParametersOnStack();
        int additionalParams = codegen.getFrameMap().getCurrentSize() - originalSize;
        //VarRemapper remapper = new VarRemapper.DeltaRemapper(initialFrameSize, valueParamSize, additionalParams, getClosureIndexes());
        VarRemapper remapper = new VarRemapper.ParamRemapper(initialFrameSize, valueParamSize, additionalParams, tempTypes);

        doInline(node.access, node.desc, codegen.getMethodVisitor(), node, remapper.doRemap(valueParamSize + additionalParams), inlineClosures, remapper);
    }

    private void prepareInlinedMethod(MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                //TODO check closure
                if (methodInsnNode.name.equals(INVOKE) && methodInsnNode.owner.equals(INLINE_RUNTIME)) {
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
            }
            cur = next;
        }
    }

    private void removeClosureAssertions(MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
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

    private void prepareInlinedMethod2(MethodNode node) throws AnalyzerException {

        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter());
        Frame<SourceValue>[] sources = analyzer.analyze("fake", node);

        AbstractInsnNode cur = node.instructions.getFirst();
        int index = 0;
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) cur;
                //TODO check closure
                if (methodInsnNode.name.equals(INVOKE) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                    Frame<SourceValue> frame = sources[index];
                    SourceValue sourceValue = frame.getStack(frame.getStackSize() - Type.getArgumentTypes(methodInsnNode.desc).length - 1);
                    assert sourceValue.insns.size() == 1;

                    AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                    assert insnNode.getType() == AbstractInsnNode.VAR_INSN && insnNode.getOpcode() == Opcodes.ALOAD;
                    int varIndex = ((VarInsnNode) insnNode).var;
                    ClosureInfo closureInfo = expressionMap.get(varIndex);
                    if (closureInfo != null) { //TODO: maybe add separate map for noninlinable closures
                        closures.add(new ClosureUsage(varIndex, true));
                        node.instructions.remove(insnNode);
                    } else {
                        closures.add(new ClosureUsage(varIndex, false));
                    }
                }
            }
            cur = cur.getNext();
            index++;
        }
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
                if (inlineClosures && /*INLINE_RUNTIME.equals(owner) &&*/ INVOKE.equals(name)) { //TODO add method
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
                    putStackValuesIntoLocals(info.getParamsWithoutCapturedValOrVar(), valueParamShift, this, desc);
                    Label closureEnd = new Label();
                    InliningAdapter closureInliner = new InliningAdapter(mv, Opcodes.ASM4, desc, closureEnd, getNextLocalIndex(),
                                                              new VarRemapper.ClosureRemapper(info, valueParamShift, tempTypes));
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

        MethodContext parentContext = codegen.getContext();

        FunctionCodegen functionCodegen = new FunctionCodegen(parentContext, null, codegen.getState(), codegen.getParentCodegen());
        MethodContext context = parentContext.intoClosure(descriptor, codegen, typeMapper).intoFunction(descriptor);

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
                    throw new UnsupportedOperationException("Coudn't find field " +
                                                            owner +
                                                            "." +
                                                            name +
                                                            " (" +
                                                            desc +
                                                            ") in captured vars of " +
                                                            info.getFunctionLiteral().getText());
                }

                super.visitFieldInsn(opcode, owner, name, desc);
            }
        };
        node.accept(transformedNode);

        return transformedNode;
    }

    @Override
    public void putInLocal(Type type, StackValue stackValue) {
        if (!disabled && notSeparateInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutValue(type, stackValue, codegen.getContext());
            int remappedIndex = couldBeRemapped ? ((StackValue.Local) stackValue).getIndex() : -1;

            ParameterInfo info = new ParameterInfo(type, false, remappedIndex, couldBeRemapped ? -1 : codegen.getFrameMap().enterTemp(type));

            doWithParameter(info);
        }
    }

    @Override
    public boolean shouldPutValue(Type type, StackValue stackValue, MethodContext context) {
        if (stackValue != null && context.isInlineFunction() && stackValue instanceof StackValue.Local) {
            if (type.getClassName().contains("Function")) {
                //TODO remap only inlinable closure => otherwise we could get a lot of problem
                return false; //TODO check annotations
            }
        }
        return true;
    }

    private void doWithParameter(ParameterInfo info) {
        recordParamInfo(info, true);
        putParameterOnStack(info);
    }

    private int recordParamInfo(ParameterInfo info, boolean addToFrame) {
        Type type = info.type;
        tempTypes.add(info);
        if (info.getType().getSize() == 2) {
            tempTypes.add(ParameterInfo.STUB);
        }
        if (addToFrame) {
            return originalFunctionFrame.enterTemp(type);
        }
        return -1;
    }

    private void putParameterOnStack(ParameterInfo info) {
        if (!info.isSkippedOrRemapped()) {
            int index = info.index;
            Type type = info.type;
            StackValue.local(index, type).store(type, codegen.getInstructionAdapter());
        }
    }

    @Override
    public void putHiddenParams() {
        List<JvmMethodParameterSignature> types = jvmSignature.getKotlinParameterTypes();

        if (!isStaticMethod(functionDescriptor, context)) {
            Type type = AsmTypeConstants.OBJECT_TYPE;
            ParameterInfo info = new ParameterInfo(type, false, -1, codegen.getFrameMap().enterTemp(type));
            recordParamInfo(info, false);
        }

        for (int i = 0; i < types.size(); i++) {
            JvmMethodParameterSignature param = types.get(i);
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            Type type = param.getAsmType();
            ParameterInfo info = new ParameterInfo(type, false, -1, codegen.getFrameMap().enterTemp(type));
            recordParamInfo(info, false);
        }

        for (ListIterator<ParameterInfo> iterator = tempTypes.listIterator(tempTypes.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            putParameterOnStack(param);
        }
    }

    @Override
    public void leaveTemps() {
        FrameMap frameMap = codegen.getFrameMap();
        for (ListIterator<ParameterInfo> iterator = tempTypes.listIterator(tempTypes.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            if (!param.isSkippedOrRemapped()) {
                frameMap.leaveTemp(param.type);
            }
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    private static void putStackValuesIntoLocals(List<Type> directOrder, int shift, InstructionAdapter mv, String descriptor) {
        Type [] actualParams = Type.getArgumentTypes(descriptor); //last param is closure itself
        assert actualParams.length == directOrder.size() : "Number of expected and actual params should be equals!";

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
        ParameterInfo closureInfo = new ParameterInfo(type, true, -1, -1);
        int index = recordParamInfo(closureInfo, true);

        ClosureInfo info = new ClosureInfo(expression, typeMapper);
        expressionMap.put(index, info);
    }

    private void putClosureParametersOnStack() {
        //SORT
        for (Iterator<ClosureInfo> iterator = expressionMap.values().iterator(); iterator.hasNext(); ) {
            ClosureInfo next = iterator.next();
            if (next.closure != null) {
                int size = tempTypes.size();
                next.setParamOffset(size);
                codegen.pushClosureOnStack(next.closure, false, this);
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

    @NotNull
    private static VarRemapper createRemapper(
            @NotNull JetTypeMapper typeMapper,
            @NotNull Frame<SourceValue> frame,
            @NotNull FunctionDescriptor descriptor,
            @NotNull MethodContext context,
            boolean includeThis
    ) {
        int types = 0;
        if (includeThis && !isStaticMethod(descriptor, context)) {
            types++;
        }

        JvmMethodSignature signature = typeMapper.mapSignature(descriptor, true, context.getContextKind());
        types += signature.getKotlinParameterTypes().size();

        int stackSize = frame.getStackSize();
        VarInsnNode [] varInsnNodes = new VarInsnNode[types];
        for (int i = 0; i < types; i++) {
            SourceValue sourceValue = frame.getStack(stackSize - types);
            if (sourceValue.insns.size() == 1) {
                AbstractInsnNode node = sourceValue.insns.iterator().next();
                if (node.getType() == AbstractInsnNode.VAR_INSN) {
                    varInsnNodes[types - i] = (VarInsnNode) node;
                }
            }
        }
        return null;
    }

    private static boolean isStaticMethod(FunctionDescriptor functionDescriptor, MethodContext context) {
        return (getMethodAsmFlags(functionDescriptor, context.getContextKind()) & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }

    static class ParameterInfo {

        private final int index;

        final Type type;

        final boolean isSkipped;

        final int remapIndex;

        public static final ParameterInfo STUB = new ParameterInfo(AsmTypeConstants.OBJECT_TYPE, true, -1, -1);

        ParameterInfo(Type type, boolean skipped, int remapIndex, int index) {
            this.type = type;
            this.isSkipped = skipped;
            this.remapIndex = remapIndex;
            this.index = index;
        }

        public boolean isSkippedOrRemapped() {
            return isSkipped || remapIndex != -1;
        }

        public int getInlinedIndex() {
            return remapIndex != -1 ? remapIndex : index;
        }

        public boolean isSkipped() {
            return isSkipped;
        }

        public Type getType() {
            return type;
        }
    }
}
