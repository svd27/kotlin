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

import java.util.Collection;
import java.util.Iterator;

public abstract class VarRemapper {

    public static class ShiftRemapper extends VarRemapper {

        private final int shift;

        public ShiftRemapper(int shift) {
            this.shift = shift;
        }

        @Override
        public int doRemap(int index) {
            return index + shift;
        }
    }

    public static class ClosureRemapper extends ShiftRemapper {

        private final ClosureInfo info;
        private final int capturedSize;

        public ClosureRemapper(ClosureInfo info, int valueParametersShift) {
            super(valueParametersShift);
            this.info = info;
            capturedSize = info.getCapturedVarsSize();
        }

        @Override
        public int doRemap(int index) {
            if (index < capturedSize) {
                return index + info.getCapturedVarsOffset();
            }
            return super.doRemap(index - capturedSize);
        }
    }

    public static class DeltaRemapper extends VarRemapper {

        private final int paramShift;
        private final int paramSize;
        private final int additionalParamsSize;
        private final Collection<Integer> closureIndexes;

        public DeltaRemapper(int paramShift, int paramSize, int additionalParamsSize, Collection<Integer> closureIndexes) {
            this.paramShift = paramShift;
            this.paramSize = paramSize;
            this.additionalParamsSize = additionalParamsSize;
            this.closureIndexes = closureIndexes;
        }

        @Override
        public int doRemap(int index) {
            int closureDelta = calcClosureDelta(index);
            if (index < paramSize) {
                return paramShift + index - closureDelta;
            } else {
                return paramShift + index + additionalParamsSize - closureDelta;
            }
        }

        private int calcClosureDelta(int index) {
            int delta = 0;
            for (Iterator<Integer> iterator = closureIndexes.iterator(); iterator.hasNext(); ) {
                Integer next = iterator.next();
                if (index > next.intValue()) {
                    delta++;
                }
            }
            return delta;
        }
    }


    protected boolean nestedRemmapper;

    public int remap(int index) {
        if (nestedRemmapper) {
            return index;
        }
        return doRemap(index);
    }

    abstract public int doRemap(int index);

    public void setNestedRemap(boolean nestedRemap) {
        nestedRemmapper = nestedRemap;
    }

}
