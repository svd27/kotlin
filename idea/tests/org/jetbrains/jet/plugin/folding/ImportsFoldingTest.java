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

package org.jetbrains.jet.plugin.folding;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.lang.java.JavaImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.editor.importOptimizer.JetImportOptimizer;

import java.io.File;

public class ImportsFoldingTest extends LightCodeInsightFixtureTestCase {

    public void testFoldingAfterOptimizeImports() {
        myFixture.configureByFile(getTestName(true) + ".kt");
        CodeFoldingManager.getInstance(getProject()).buildInitialFoldings(myFixture.getEditor());
        assert isRegionValid(getFoldingDescription());

        CommandProcessor.getInstance().executeCommand(myFixture.getProject(), new JetImportOptimizer().processFile(myFixture.getFile()),
                                                      "Optimize Imports", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);

        String foldingDescription = getFoldingDescription();
        assert isRegionValid(foldingDescription) : foldingDescription;
    }

    public void testFoldingAfterOptimizeImportsJava() {
        myFixture.configureByFile(getTestName(true) + ".java");
        CodeFoldingManager.getInstance(getProject()).buildInitialFoldings(myFixture.getEditor());
        assert isRegionValid(getFoldingDescription());

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(myFixture.getProject(), new JavaImportOptimizer().processFile(myFixture.getFile()),
                                                              "Optimize Imports", null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
            }
        });

        String foldingDescription = getFoldingDescription();
        assert isRegionValid(foldingDescription) : foldingDescription;
    }

    private String getFoldingDescription() {
        FoldingModel model = myFixture.getEditor().getFoldingModel();
        FoldRegion[] foldingRegions = model.getAllFoldRegions();

        StringBuilder result = new StringBuilder();
        for (FoldRegion region : foldingRegions) {
            result.append(region.toString()).append(", expanded=").append(region.isExpanded());
        }
        return result.toString();
    }

    private static boolean isRegionValid(@NotNull String region) {
        return !region.contains("invalid");
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/folding/imports/").getPath() + File.separator;
    }
}
