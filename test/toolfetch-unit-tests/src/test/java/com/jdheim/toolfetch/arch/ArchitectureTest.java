/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.arch;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "com.jdheim.toolfetch")
class ArchitectureTest {

    @ArchTest
    void noTestHooksInProduction(JavaClasses classes) {
        noClasses().that()
                .haveSimpleNameNotEndingWith("Test")
                .and()
                .haveSimpleNameNotEndingWith("IT")
                .should()
                .callMethodWhere(nameMatching(".*TestHook"))
                .check(classes);
    }

}
