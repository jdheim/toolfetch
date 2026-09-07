/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.step.assertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/// Utility class for performing common assertions
public final class AssertionSteps {

    private AssertionSteps() {
        throw new AssertionError();
    }

    /// Reflection should not be used to increase accessibility of classes, methods, or fields (java:S3011)
    /// Justification: Test-only method
    @SuppressWarnings("java:S3011")
    public static <T> void assertNotInstantiable(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(AssertionError.class);
        } catch (ReflectiveOperationException e) {
            throw new LinkageError("Failed to assert that class is not instantiable", e);
        }
    }

    /// Asserts the structure and contents of the specified destination directory after the "sample1" archive is extracted
    public static void assertSample1Archive(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        for (int i = 1; i <= 3; i++) {
            assertThat(destinationPath.resolve("test%d.txt".formatted(i))).isRegularFile()
                    .hasContent("Hello ToolFetch %d".formatted(i));
        }
    }

    /// Asserts the structure and contents of the specified destination directory after the "sample2" archive is extracted
    public static void assertSample2Archive(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).doesNotExist();
        assertThat(destinationPath.resolve("test11")).doesNotExist();
        assertThat(destinationPath.resolve("test111")).isDirectory();
        assertThat(destinationPath.resolve("test111/test1111")).isDirectory();
        assertThat(destinationPath.resolve("test111/test1111/test11111")).isDirectory();
        assertThat(destinationPath.resolve("test111/test1111/test11111/test111111")).isDirectory();
        assertThat(destinationPath.resolve("test222")).isDirectory();
        assertThat(destinationPath.resolve("test333")).isDirectory();
        assertThat(destinationPath.resolve("test333/test3333")).isDirectory();
        assertThat(destinationPath.resolve("test111/test1111/test11111/test11111.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 11111");
        assertThat(destinationPath.resolve("test111/test1111/test11111/test111111/test111111.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 111111");
        assertThat(destinationPath.resolve("test222/test222.txt")).isRegularFile().hasContent("Hello ToolFetch 222");
        assertThat(destinationPath.resolve("test333/test3333/test3333-1.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-1");
        assertThat(destinationPath.resolve("test333/test3333/test3333-2.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-2");
        assertThat(destinationPath.resolve("test333/test3333/test3333-3.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-3");
    }

    /// Asserts the structure and contents of the specified destination directory after the "sample3" archive is extracted
    public static void assertSample3Archive(Path destinationPath) {
        assertThat(destinationPath).isDirectory();
        assertThat(destinationPath.resolve("test1")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test111")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test111/test1111")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111/test111111")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test222")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test333")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test333/test3333")).isDirectory();
        assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111/test11111.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 11111");
        assertThat(destinationPath.resolve("test1/test11/test111/test1111/test11111/test111111/test111111.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 111111");
        assertThat(destinationPath.resolve("test1/test11/test222/test222.txt")).isRegularFile().hasContent("Hello ToolFetch 222");
        assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-1.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-1");
        assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-2.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-2");
        assertThat(destinationPath.resolve("test1/test11/test333/test3333/test3333-3.txt")).isRegularFile()
                .hasContent("Hello ToolFetch 3333-3");
        assertThat(destinationPath.resolve("test4.txt")).isRegularFile().hasContent("Hello ToolFetch 4");
    }

}
