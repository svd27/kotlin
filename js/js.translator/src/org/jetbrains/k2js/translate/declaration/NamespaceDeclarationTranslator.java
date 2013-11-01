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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.k2js.translate.declaration.DefineInvocation.createDefineInvocation;

public final class NamespaceDeclarationTranslator extends AbstractTranslator {
    private final Iterable<JetFile> files;
    private final Map<PackageViewDescriptor, NamespaceTranslator> packageViewToTranslator =
            new LinkedHashMap<PackageViewDescriptor, NamespaceTranslator>();

    public static List<JsStatement> translateFiles(@NotNull Collection<JetFile> files, @NotNull TranslationContext context) {
        return new NamespaceDeclarationTranslator(files, context).translate();
    }

    private NamespaceDeclarationTranslator(@NotNull Iterable<JetFile> files, @NotNull TranslationContext context) {
        super(context);

        this.files = files;
    }

    @NotNull
    private List<JsStatement> translate() {
        // predictable order
        Map<PackageViewDescriptor, DefineInvocation> packageViewToDefineInvocation = new THashMap<PackageViewDescriptor, DefineInvocation>();
        PackageViewDescriptor rootNamespaceDescriptor = null;

        for (JetFile file : files) {
            PackageFragmentDescriptor packageFragment = context().bindingContext().get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);
            assert packageFragment != null;
            PackageViewDescriptor packageView = packageFragment.getContainingDeclaration().getPackage(packageFragment.getFqName());
            assert packageView != null;

            NamespaceTranslator translator = packageViewToTranslator.get(packageView);
            if (translator == null) {
                if (rootNamespaceDescriptor == null) {
                    rootNamespaceDescriptor = getRootPackageDescriptor(packageViewToDefineInvocation, packageView);
                }
                translator = new NamespaceTranslator(packageView, packageViewToDefineInvocation, context());
                packageViewToTranslator.put(packageView, translator);
            }

            translator.translate(file);
        }

        if (rootNamespaceDescriptor == null) {
            return Collections.emptyList();
        }

        context().classDeclarationTranslator().generateDeclarations();
        for (NamespaceTranslator translator : packageViewToTranslator.values()) {
            translator.add(packageViewToDefineInvocation);
        }

        JsVars vars = new JsVars(true);
        vars.addIfHasInitializer(context().classDeclarationTranslator().getDeclaration());
        vars.addIfHasInitializer(getRootPackageDeclaration(packageViewToDefineInvocation.get(rootNamespaceDescriptor)));

        return Collections.<JsStatement>singletonList(vars);
    }

    @NotNull
    private PackageViewDescriptor getRootPackageDescriptor(
            @NotNull Map<PackageViewDescriptor, DefineInvocation> descriptorToDefineInvocation,
            @NotNull PackageViewDescriptor descriptor
    ) {
        PackageViewDescriptor rootPackage = descriptor;
        while (!rootPackage.getFqName().isRoot()) {
            rootPackage = rootPackage.getContainingDeclaration();
        }

        descriptorToDefineInvocation.put(rootPackage, createDefineInvocation(rootPackage, null, new JsObjectLiteral(true), context()));
        return rootPackage;
    }

    private JsVar getRootPackageDeclaration(@NotNull DefineInvocation defineInvocation) {
        JsExpression rootPackageVar = new JsInvocation(context().namer().rootPackageDefinitionMethodReference(), defineInvocation.asList());
        return new JsVar(context().scope().declareName(Namer.getRootNamespaceName()), rootPackageVar);
    }
}
