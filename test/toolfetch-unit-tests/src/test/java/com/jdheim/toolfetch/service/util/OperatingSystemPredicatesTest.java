/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

/// OOC Tests for [OperatingSystemPredicates]
class OperatingSystemPredicatesTest {

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(OperatingSystemPredicates.class);
    }

    @Test
    void testIsLinux() {
        assertThat(OperatingSystemPredicates.isLinux()).isEqualTo(SystemUtils.IS_OS_LINUX);
    }

    @Test
    void testIsMacOs() {
        assertThat(OperatingSystemPredicates.isMacOs()).isEqualTo(SystemUtils.IS_OS_MAC);
    }

    @Test
    void testIsWindows() {
        assertThat(OperatingSystemPredicates.isWindows()).isEqualTo(SystemUtils.IS_OS_WINDOWS);
    }

}
