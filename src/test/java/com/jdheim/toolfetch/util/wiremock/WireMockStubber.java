/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
