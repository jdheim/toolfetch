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

package com.jdheim.toolfetch.service.install.resolve;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveNameResolver implements FileNameResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveNameResolver.class);

    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    private static final String CONTENT_DISPOSITION_HEADER_PREFIX = "attachment";

    private static final int CONTENT_DISPOSITION_HEADER_MAX_LENGTH = 8192;

    private static final Pattern FILENAME_RFC5987_PATTERN = Pattern.compile("filename\\*\\s?=\\s?([^']*+)'([^']*+)'([^;]++)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\s?=\\s?\"?([^\";]++)\"?",
            Pattern.CASE_INSENSITIVE);

    private static String escapePlusBeforeUrlDecode(String fileName) {
        return fileName.replace("+", "%2B");
    }

    @Override
    public String resolve(HttpResponse<InputStream> httpResponse) {
        return httpResponse.headers()
                .firstValue(CONTENT_DISPOSITION_HEADER)
                .filter(contentDisposition -> Strings.CI.startsWith(contentDisposition, CONTENT_DISPOSITION_HEADER_PREFIX))
                .map(this::resolveFromHeader)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> resolveFromUri(httpResponse));
    }

    String resolveFromHeader(String contentDisposition) {
        if (contentDisposition.length() > CONTENT_DISPOSITION_HEADER_MAX_LENGTH) {
            LOG.warn("{} header longer than {} characters. Skipping archive name resolution from header",
                    CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_HEADER_MAX_LENGTH);
            return StringUtils.EMPTY;
        }
        String archiveName = resolveFromFilenameRfc5987Pattern(contentDisposition);
        if (StringUtils.isEmpty(archiveName)) {
            archiveName = resolveFromFilenamePattern(contentDisposition);
        }
        return archiveName;
    }

    String resolveFromFilenameRfc5987Pattern(String contentDisposition) {
        Matcher matcher = FILENAME_RFC5987_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String charsetName = Optional.ofNullable(matcher.group(1))
                    .filter(StringUtils::isNotEmpty)
                    .map(String::trim)
                    .orElse(StandardCharsets.UTF_8.name());
            Charset charset;
            if (Charset.isSupported(charsetName)) {
                charset = Charset.forName(charsetName);
            } else {
                LOG.warn("{} header contains unsupported charset: {}. Falling back to UTF-8", CONTENT_DISPOSITION_HEADER,
                        charsetName);
                charset = StandardCharsets.UTF_8;
            }
            String filename = escapePlusBeforeUrlDecode(matcher.group(3).trim());
            return URLDecoder.decode(filename, charset);
        }
        return StringUtils.EMPTY;
    }

    String resolveFromFilenamePattern(String contentDisposition) {
        Matcher matcher = FILENAME_PATTERN.matcher(contentDisposition);
        if (matcher.find()) {
            String filename = escapePlusBeforeUrlDecode(matcher.group(1).trim());
            return URLDecoder.decode(filename, StandardCharsets.UTF_8);
        }
        return StringUtils.EMPTY;
    }

    String resolveFromUri(HttpResponse<InputStream> httpResponse) {
        return Optional.ofNullable(httpResponse.uri())
                .map(URI::getRawPath)
                .filter(path -> !path.endsWith("/"))
                .map(path -> path.substring(path.lastIndexOf('/') + 1))
                .map(String::trim)
                .map(ArchiveNameResolver::escapePlusBeforeUrlDecode)
                .map(fileName -> URLDecoder.decode(fileName, StandardCharsets.UTF_8))
                .orElse(StringUtils.EMPTY);
    }

}
