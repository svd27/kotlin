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

package org.jetbrains.jet.lang.resolve.lazy.data;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collections;
import java.util.List;

public class EmptyClassLikeInfo implements JetClassLikeInfo {
    private final ClassKind kind;
    private final FqName containingPackageFqName;
    private final PsiElement scopeAnchor;

    public EmptyClassLikeInfo(@NotNull ClassKind kind, @NotNull FqName containingPackageFqName, @NotNull PsiElement scopeAnchor) {
        this.kind = kind;
        this.containingPackageFqName = containingPackageFqName;
        this.scopeAnchor = scopeAnchor;
    }

    @NotNull
    @Override
    public FqName getContainingPackageFqName() {
        return containingPackageFqName;
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return null;
    }

    @Override
    @Nullable
    public JetClassOrObject getClassObject() {
        return null;
    }

    @Override
    @NotNull
    public PsiElement getScopeAnchor() {
        return scopeAnchor;
    }

    @Override
    @Nullable
    public JetClassOrObject getCorrespondingClassOrObject() {
        return null;
    }

    @Override
    @NotNull
    public List<JetTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public List<? extends JetParameter> getPrimaryConstructorParameters() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public ClassKind getClassKind() {
        return kind;
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "empty info for " + kind + " in " + containingPackageFqName;
    }
}
