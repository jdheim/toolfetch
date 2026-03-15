/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.util.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import java.nio.file.Path;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import org.apache.commons.lang3.StringUtils;

/// Utility class for configuring WireMock stubs for serving file downloads
public final class WireMockStubber {

    private WireMockStubber() {
        throw new AssertionError();
    }

    /// Configures a WireMock stub for serving a file download based on the provided archive name and content
    public static void stubFor(Path archiveName, byte[] archiveBytes) {
        stubFor(StringUtils.EMPTY, archiveName, archiveBytes);
    }

    /// Configures a WireMock stub for serving a file download based on the provided version, archive name, and content
    public static void stubFor(String version, Path archiveName, byte[] archiveBytes) {
        if (StringUtils.isNotEmpty(version)) {
            version = version + "/";
        }
        WireMock.stubFor(get("/download/" + version + archiveName).willReturn(aResponse().withStatus(200)
                .withHeader(ContentTypes.CONTENT_TYPE, ContentTypes.OCTET_STREAM)
                .withHeader(ContentTypes.CONTENT_LENGTH, String.valueOf(archiveBytes.length))
                .withHeader("Content-Disposition", "attachment; filename=" + archiveName)
                .withBody(archiveBytes)));
    }

}
