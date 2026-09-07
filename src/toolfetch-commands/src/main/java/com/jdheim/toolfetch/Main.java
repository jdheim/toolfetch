/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch;

import com.jdheim.toolfetch.command.ToolFetch;

public final class Main {

    private Main() {
        throw new AssertionError();
    }

    static void main(String[] args) {
        System.exit(ToolFetch.execute(System.nanoTime(), args));
    }

}
