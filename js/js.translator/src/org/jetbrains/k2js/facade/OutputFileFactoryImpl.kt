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

package org.jetbrains.k2js.facade

import org.jetbrains.jet.OutputFileFactory
import java.io.File

class OutputFileFactoryImpl(private val sources: List<File> = listOf()) : OutputFileFactory {
    private val outputs = hashMapOf<String, String>()

    override val outputFiles: List<String>
        get() = outputs.keySet().toList()

    override fun getSourceFiles(file: String): List<File> {
        return sources;
    }

    override fun asBytes(file: String): ByteArray{
        assert(outputs.containsKey(file), "Requested byte content for non-existent file '$file'")
        return outputs[file].orEmpty().toByteArray()
    }

    override fun asText(file: String): String {
        assert(outputs.containsKey(file), "Requested content for non-existent file '$file'")
        return outputs[file].orEmpty()
    }

    fun addOutput(file: String, data: String) {
        assert(!outputs.containsKey(file), "Output file '$file' already added")
        outputs[file] = data
    }
}