/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.env;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/// A utility class that provides functionality to resolve environment variables in strings.
/// This class is designed to transform strings containing placeholders for environment
/// variables (e.g., $VAR or ${VAR}) into strings with the placeholders
/// replaced by their corresponding values from the system's environment variables.
public final class EnvResolver {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Z_][A-Z0-9_]*)}|\\$([A-Z_][A-Z0-9_]*)(?![A-Z0-9_]*})");

    EnvResolver() {
        throw new AssertionError();
    }

    public static Path resolveAsPath(String input) {
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException("Environment variable could not be resolved as the input is empty");
        }
        return Path.of(resolve(input));
    }

    public static String resolve(@Nullable String input) {
        if (StringUtils.isBlank(input)) {
            return StringUtils.EMPTY;
        }
        StringBuilder result = new StringBuilder();
        Matcher matcher = ENV_PATTERN.matcher(input);

        while (matcher.find()) {
            String envName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String envValue = System.getenv(envName);

            if (envValue == null) {
                throw new IllegalArgumentException(
                        "Environment variable \"%s\" is not set (provided in \"%s\")".formatted(envName, input));
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }

}
