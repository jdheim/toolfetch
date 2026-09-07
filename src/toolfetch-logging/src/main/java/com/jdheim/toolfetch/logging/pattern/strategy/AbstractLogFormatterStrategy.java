/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern.strategy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.logging.LogLevel;
import com.jdheim.toolfetch.logging.LogMarker;
import org.slf4j.Marker;

abstract class AbstractLogFormatterStrategy implements LogFormatterStrategy {

    static final String LOG_LEVEL_START = "[";

    static final String LOG_LEVEL_END = "]";

    static final String LOG_LEVEL_ERROR = LogLevel.ERROR.toString();

    static final String LOG_LEVEL_WARN = LogLevel.WARN.toString();

    static final String LOG_LEVEL_INFO = LogLevel.INFO.toString();

    static final String LOG_LEVEL_STEP = LogMarker.STEP.toString();

    static final String LOG_LEVEL_STEP_DELIMITER = "===";

    static final String LOG_LEVEL_DEBUG = LogLevel.DEBUG.toString();

    static final String LOG_LEVEL_TRACE = LogLevel.TRACE.toString();

    @Override
    public String convertLogLevel(ILoggingEvent event) {
        if (isStep(event)) {
            return step();
        }
        return switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> error();
            case Level.WARN_INT -> warn();
            case Level.INFO_INT -> info();
            case Level.DEBUG_INT -> debug();
            case Level.TRACE_INT -> trace();
            default -> other(event);
        };
    }

    @Override
    public String convertLogMessage(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (!isStep(event)) {
            return message;
        }
        return stepMessage(message);
    }

    abstract String error();

    abstract String warn();

    abstract String info();

    abstract String step();

    boolean isStep(ILoggingEvent event) {
        return event.getMarkerList() != null && event.getMarkerList()
                .stream()
                .map(Marker::getName)
                .anyMatch(LOG_LEVEL_STEP::equals);
    }

    abstract String stepMessage(String message);

    String stepDelimiter() {
        return LOG_LEVEL_STEP_DELIMITER;
    }

    abstract String debug();

    abstract String trace();

    abstract String other(ILoggingEvent event);

}
