/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.log;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/// OOC Tests for [LogHelper]
class LogHelperTest {

    @Test
    void testNoInstance() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(LogHelper::new);
    }

}
