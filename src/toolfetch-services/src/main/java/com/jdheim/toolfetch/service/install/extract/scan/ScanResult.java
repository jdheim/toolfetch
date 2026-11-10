/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.extract.scan;

import java.util.Map;
import com.jdheim.toolfetch.service.util.CharacterConstants;
import org.apache.commons.lang3.StringUtils;

/// ZIP modes are keyed by entry data offset so repeated names retain distinct metadata
public record ScanResult(String topLevelDir, Map<Long, Integer> zipModes) {

    public ScanResult {
        zipModes = Map.copyOf(zipModes);
        if (StringUtils.isNotEmpty(topLevelDir) && !topLevelDir.endsWith(CharacterConstants.SLASH)) {
            topLevelDir += CharacterConstants.SLASH;
        }
    }

    public int zipMode(long dataOffset) {
        return zipModes.getOrDefault(dataOffset, 0);
    }

}
