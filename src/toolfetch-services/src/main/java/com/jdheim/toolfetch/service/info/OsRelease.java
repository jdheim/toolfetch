/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.info;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.jdheim.toolfetch.service.util.CharacterConstants;
import org.apache.commons.lang3.StringUtils;

public final class OsRelease {

    public static final String PRETTY_NAME = "PRETTY_NAME";

    public static final String NAME = "NAME";

    public static final String VERSION_ID = "VERSION_ID";

    public static final String VERSION = "VERSION";

    public static final String VERSION_CODENAME = "VERSION_CODENAME";

    public static final String ID = "ID";

    public static final String ID_LIKE = "ID_LIKE";

    public static final String HOME_URL = "HOME_URL";

    public static final String SUPPORT_URL = "SUPPORT_URL";

    public static final String BUG_REPORT_URL = "BUG_REPORT_URL";

    public static final String PRIVACY_POLICY_URL = "PRIVACY_POLICY_URL";

    private static final Path OS_RELEASE_PATH = Path.of("/etc/os-release");

    private OsRelease() {
        throw new AssertionError();
    }

    public static Map<String, String> get() {
        return LazyHolder.VALUE;
    }

    static Map<String, String> parseOsRelease() {
        if (!Files.isReadable(osReleasePath())) {
            return Map.of();
        }

        try (Stream<String> lines = Files.lines(osReleasePath())) {
            return lines.filter(OsRelease::lineNotBlankNotCommented)
                    .flatMap(OsRelease::parseLine)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, OsRelease::mergeDuplicates));
        } catch (IOException _) {
            return Map.of();
        }
    }

    static Path osReleasePath() {
        return OS_RELEASE_PATH;
    }

    private static boolean lineNotBlankNotCommented(String line) {
        return !line.isBlank() && !line.stripLeading().startsWith(CharacterConstants.COMMENT_PREFIX);
    }

    private static Stream<Map.Entry<String, String>> parseLine(String line) {
        int separator = line.indexOf(CharacterConstants.EQUALS_CHAR);
        if (separator < 0) {
            return Stream.empty();
        }
        String key = line.substring(0, separator).trim();
        if (key.isEmpty()) {
            return Stream.empty();
        }
        String value = StringUtils.unwrap(line.substring(separator + 1).trim(), CharacterConstants.DOUBLE_QUOTE_CHAR);
        return Stream.of(Map.entry(key, value));
    }

    /**
     * If the file contains the same key twice, the last one wins
     */
    private static String mergeDuplicates(String first, String second) {
        return second;
    }

    static final class LazyHolder {

        private static final Map<String, String> VALUE = parseOsRelease();

        private LazyHolder() {
            throw new AssertionError();
        }

    }

}
