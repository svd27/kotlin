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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyResolveTestUtil {
    private LazyResolveTestUtil() {
    }

    public static InjectorForTopDownAnalyzer getEagerInjectorForTopDownAnalyzer(JetCoreEnvironment environment) {
        ModuleDescriptorImpl eagerModuleForLazy = AnalyzerFacadeForJVM.createJavaModule("<eager module for lazy>");

        InjectorForTopDownAnalyzer tdaInjectorForLazy = createInjectorForTDA(eagerModuleForLazy, environment);
        // This line is required fro the 'jet' namespace to be filled in with functions
        tdaInjectorForLazy.getTopDownAnalyzer().analyzeFiles(
                Collections.singletonList(JetPsiFactory.createFile(environment.getProject(), "")), Collections.<AnalyzerScriptParameter>emptyList());
        return tdaInjectorForLazy;
    }

    public static InjectorForTopDownAnalyzer createInjectorForTDA(ModuleDescriptorImpl module, JetCoreEnvironment environment) {
        JetTestUtils.newTrace(environment);

        TopDownAnalysisParameters params = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysTrue(), false, false, Collections.<AnalyzerScriptParameter>emptyList());
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).getTrace();
        return new InjectorForTopDownAnalyzerForJvm(environment.getProject(), params, sharedTrace, module);
    }

    public static ModuleDescriptor resolveEagerly(List<JetFile> files, JetCoreEnvironment environment) {
        ModuleDescriptorImpl module = AnalyzerFacadeForJVM.createJavaModule("<test module>");
        InjectorForTopDownAnalyzer injector = createInjectorForTDA(module, environment);
        injector.getTopDownAnalyzer().analyzeFiles(files, Collections.<AnalyzerScriptParameter>emptyList());
        return module;
    }

    public static KotlinCodeAnalyzer resolveLazilyWithSession(List<JetFile> files, JetCoreEnvironment environment, boolean addBuiltIns) {
        JetTestUtils.newTrace(environment);

        Project project = environment.getProject();
        BindingTrace sharedTrace = CliLightClassGenerationSupport.getInstanceForCli(project).getTrace();
        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(project, sharedTrace);

        return AnalyzerFacadeForJVM.createLazyResolveSession(project, files, sharedTrace, injector, addBuiltIns);
    }

    public static ModuleDescriptor resolveLazily(List<JetFile> files, JetCoreEnvironment environment) {
        return resolveLazily(files, environment, true);
    }

    public static ModuleDescriptor resolveLazily(List<JetFile> files, JetCoreEnvironment environment, boolean addBuiltIns) {
        return resolveLazilyWithSession(files, environment, addBuiltIns).getModuleDescriptor();
    }

    @NotNull
    public static Set<Name> getTopLevelPackagesFromFileList(@NotNull List<JetFile> files) {
        Set<Name> shortNames = Sets.newLinkedHashSet();
        for (JetFile file : files) {
            JetNamespaceHeader header = file.getNamespaceHeader();
            if (header != null) {
                List<JetSimpleNameExpression> names = header.getParentNamespaceNames();
                Name name = names.isEmpty() ? header.getNameAsName() : names.get(0).getReferencedNameAsName();
                shortNames.add(name);
            }
            else {
                throw new IllegalStateException("Scripts are not supported: " + file.getName());
            }
        }
        return shortNames;
    }
}
