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

import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.commons.LocalVariablesSorter;
import org.jetbrains.asm4.tree.MethodNode;

import java.util.*;

//http://asm.ow2.org/current/asm-transformations.pdf
public class ByteCodeInliner extends LocalVariablesSorter {
    private final int access;
    private final String desc;
    private final MethodVisitor mv;
    private final MethodNode methodNode;
    private final int frameSize;
    private List blocks = new ArrayList();
    private boolean inlining;

    public ByteCodeInliner(
            int access,
            String desc,
            MethodVisitor mv,
            MethodNode methodNode,
            int frameSize
    ) {
        super(access, desc, mv);
        this.access = access;
        this.desc = desc;
        this.mv = mv;
        this.methodNode = methodNode;
        this.frameSize = frameSize;
    }

    public void visitMethodInsn(
            int opcode,
            String owner, String name, String desc
    ) {
        Label end = new Label();
        inlining = true;
        methodNode.instructions.resetLabels();
        methodNode.accept(new InliningAdapter(this, opcode, desc, end, frameSize, new VarRemapper.ShiftRemapper(-1)));
        inlining = false;
        super.visitLabel(end);
    }

    public void start() {
        Label end = new Label();
        methodNode.instructions.resetLabels();
        methodNode.accept(new InliningAdapter(mv, access, desc, end, frameSize, new VarRemapper.ShiftRemapper(-1)));
        mv.visitLabel(end);
    }
}