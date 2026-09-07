/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern.strategy;

import static org.jline.jansi.Ansi.ansi;

import ch.qos.logback.classic.spi.ILoggingEvent;

public final class JansiLogFormatterStrategy extends AbstractLogFormatterStrategy {

    @Override
    String error() {
        return ansi().a(LOG_LEVEL_START).fgRed().bold().a(LOG_LEVEL_ERROR).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String warn() {
        return ansi().a(LOG_LEVEL_START).fgYellow().bold().a(LOG_LEVEL_WARN).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String info() {
        return ansi().a(LOG_LEVEL_START).fgBlue().bold().a(LOG_LEVEL_INFO).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String step() {
        return ansi().a(LOG_LEVEL_START).fgCyan().bold().a(LOG_LEVEL_STEP).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String stepMessage(String message) {
        String delimiter = stepDelimiter();
        message = ansi().bold().a(message).reset().toString();
        return "%s %s %s".formatted(delimiter, message, delimiter);
    }

    @Override
    String stepDelimiter() {
        return ansi().fgCyan().bold().a(super.stepDelimiter()).reset().toString();
    }

    @Override
    String debug() {
        return ansi().a(LOG_LEVEL_START).fgGreen().bold().a(LOG_LEVEL_DEBUG).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String trace() {
        return ansi().a(LOG_LEVEL_START).fgBrightBlack().bold().a(LOG_LEVEL_TRACE).reset().a(LOG_LEVEL_END).toString();
    }

    @Override
    String other(ILoggingEvent event) {
        return ansi().a(LOG_LEVEL_START)
                .fgBrightBlack()
                .bold()
                .a("%s".formatted(event.getLevel()))
                .reset()
                .a(LOG_LEVEL_END)
                .toString();
    }

}
