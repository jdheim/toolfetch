/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.step.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public final class FileSteps {

    private FileSteps() {
        throw new AssertionError();
    }

    public static void setExecutable(Path path) {
        try {
            if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(path, permissions);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to set executable permission on " + path, e);
        }
    }

}
