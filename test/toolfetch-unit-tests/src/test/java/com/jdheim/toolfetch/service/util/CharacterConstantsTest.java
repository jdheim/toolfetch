/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.util;

import com.jdheim.toolfetch.step.assertion.AssertionSteps;
import org.junit.jupiter.api.Test;

/// OOC Tests for [CharacterConstants]
class CharacterConstantsTest {

    @Test
    void testNotInstantiable() {
        AssertionSteps.assertNotInstantiable(CharacterConstants.class);
    }

}
