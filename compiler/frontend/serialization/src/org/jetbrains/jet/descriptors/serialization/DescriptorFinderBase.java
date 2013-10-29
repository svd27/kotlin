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

package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;

import static org.jetbrains.jet.lang.resolve.name.SpecialNames.isClassObjectName;

public abstract class DescriptorFinderBase implements DescriptorFinder {
    @Override
    @Nullable
    public final ClassDescriptor findClass(@NotNull ClassId classId) {
        ClassDescriptor found = findClassImpl(classId);
        if (found != null) {
            return found;
        }

        if (isClassObjectName(classId.getRelativeClassName().shortName())) {
            ClassDescriptor parent = findClassImpl(classId.getOuterClassId());
            if (parent != null) {
                return parent.getClassObjectDescriptor();
            }
        }

        return null;
    }

    @Nullable
    public abstract ClassDescriptor findClassImpl(@NotNull ClassId classId);
}
