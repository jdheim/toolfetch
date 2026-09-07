/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.log;

import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import org.junit.jupiter.api.Test;

/// OOC Tests for [LogHelper]
class LogHelperTest {

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(LogHelper.class);
    }

}
