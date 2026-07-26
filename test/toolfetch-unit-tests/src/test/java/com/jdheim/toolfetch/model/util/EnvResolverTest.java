/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/// OOC Tests for [EnvResolver]
class EnvResolverTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "$HOME/test", "${HOME}/test", "${HOME/test", "$HOME}/test", "$NOT_EXISTS/test", "$HOME/test1:$HOME/test2",
            "$HOME/test1:${HOME/test2", "$HOME/test1:$HOME}/test2", "$HOME/test1:${HOME}/test2", "$HOME/test1:$NOT_EXISTS/test2",
            "$HOME/test1:${NOT_EXISTS}/test2"
    })
    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV",
            justification = "Test code intentionally uses System.getenv to validate environment variable handling")
    void testResolve(String stringWithEnv) {
        String expectedResolvedString = StringUtils.isNotBlank(stringWithEnv) ? stringWithEnv : StringUtils.EMPTY;
        if (expectedResolvedString.contains("$HOME/")) {
            expectedResolvedString = expectedResolvedString.replace("$HOME/", System.getenv("HOME") + "/");
        }
        if (expectedResolvedString.contains("${HOME}/")) {
            expectedResolvedString = expectedResolvedString.replace("${HOME}/", System.getenv("HOME") + "/");
        }
        if (expectedResolvedString.contains("NOT_EXISTS")) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> EnvResolver.resolve(stringWithEnv))
                    .withMessage("Environment variable \"NOT_EXISTS\" is not set (provided in \"%s\")".formatted(stringWithEnv));
        } else {
            String envValue = EnvResolver.resolve(stringWithEnv);
            assertThat(expectedResolvedString).isEqualTo(envValue);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "$HOME/test", "${HOME}/test", "${HOME/test", "$HOME}/test", "$NOT_EXISTS/test", "$HOME/test1:$HOME/test2",
            "$HOME/test1:${HOME/test2", "$HOME/test1:$HOME}/test2", "$HOME/test1:${HOME}/test2", "$HOME/test1:$NOT_EXISTS/test2",
            "$HOME/test1:${NOT_EXISTS}/test2"
    })
    @SuppressFBWarnings(value = "ENV_USE_PROPERTY_INSTEAD_OF_ENV",
            justification = "Test code intentionally uses System.getenv to validate environment variable handling")
    void testResolveAsPath(String stringWithEnv) {
        String expectedResolvedString = StringUtils.isNotBlank(stringWithEnv) ? stringWithEnv : null;
        if (expectedResolvedString != null && expectedResolvedString.contains("$HOME/")) {
            expectedResolvedString = expectedResolvedString.replace("$HOME/", System.getenv("HOME") + "/");
        }
        if (expectedResolvedString != null && expectedResolvedString.contains("${HOME}/")) {
            expectedResolvedString = expectedResolvedString.replace("${HOME}/", System.getenv("HOME") + "/");
        }
        if (expectedResolvedString == null) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> EnvResolver.resolveAsPath(stringWithEnv))
                    .withMessage("Environment variable could not be resolved as the input is empty");
        } else if (expectedResolvedString.contains("NOT_EXISTS")) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> EnvResolver.resolveAsPath(stringWithEnv))
                    .withMessage("Environment variable \"NOT_EXISTS\" is not set (provided in \"%s\")".formatted(stringWithEnv));
        } else {
            Path envValue = EnvResolver.resolveAsPath(stringWithEnv);
            assertThat(Path.of(expectedResolvedString)).isEqualTo(envValue);
        }
    }

    @Test
    void testNoInstance() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(EnvResolver::new);
    }

}
