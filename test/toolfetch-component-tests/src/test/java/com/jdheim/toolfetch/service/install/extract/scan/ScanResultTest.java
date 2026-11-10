/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Component Tests for [ScanResult]
class ScanResultTest {

    Map<String, Integer> zipModes;

    ScanResult scanResult;

    @BeforeEach
    void setUp() {
        zipModes = new HashMap<>();
        zipModes.put("tool", UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
        scanResult = new ScanResult("tools", zipModes);
    }

    @Test
    void testConstructorCopiesZipModes() {
        zipModes.clear();

        assertThat(scanResult.zipMode("tool")).isEqualTo(UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
    }

    @Test
    void testZipModesCannotBeModified() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> scanResult.zipModes().clear());
    }

}
