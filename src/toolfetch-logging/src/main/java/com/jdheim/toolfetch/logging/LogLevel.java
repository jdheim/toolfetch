/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public enum LogLevel {

    ERROR {
        @Override
        void log(Logger logger, String message) {
            logger.error(message);
        }
    },
    WARN {
        @Override
        void log(Logger logger, String message) {
            logger.warn(message);
        }
    },
    INFO {
        @Override
        void log(Logger logger, String message) {
            logger.info(message);
        }
    },
    STEP {
        private static final Marker STEP_MARKER = MarkerFactory.getMarker(LogMarker.STEP.name());

        @Override
        void log(Logger logger, String message) {
            logger.atInfo().addMarker(STEP_MARKER).log(message);
        }
    },
    DEBUG {
        @Override
        void log(Logger logger, String message) {
            logger.debug(message);
        }
    },
    TRACE {
        @Override
        void log(Logger logger, String message) {
            throw new UnsupportedOperationException(
                    "Trace logging must check ToolFetchLogger.isTraceEnabled() and use ToolFetchLogger.trace()");
        }
    };

    abstract void log(Logger logger, String message);

}
