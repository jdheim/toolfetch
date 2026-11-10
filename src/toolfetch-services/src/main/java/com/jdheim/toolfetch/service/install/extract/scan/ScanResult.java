/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import java.util.Map;
import com.jdheim.toolfetch.service.util.CharacterConstants;
import org.apache.commons.lang3.StringUtils;

public record ScanResult(String topLevelDir, Map<String, Integer> zipModes) {

    public ScanResult {
        zipModes = Map.copyOf(zipModes);
        if (StringUtils.isNotEmpty(topLevelDir) && !topLevelDir.endsWith(CharacterConstants.SLASH)) {
            topLevelDir += CharacterConstants.SLASH;
        }
    }

    public int zipMode(String entryName) {
        return zipModes.getOrDefault(entryName, 0);
    }

}
