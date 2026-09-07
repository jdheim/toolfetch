/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.logging.pattern.strategy.JansiLogFormatterStrategy;
import com.jdheim.toolfetch.logging.pattern.strategy.LogFormatterStrategy;

public final class JansiLogMessageConverter extends ClassicConverter {

    private static final LogFormatterStrategy LOG_FORMATTER_STRATEGY = new JansiLogFormatterStrategy();

    @Override
    public String convert(ILoggingEvent event) {
        return LOG_FORMATTER_STRATEGY.convertLogMessage(event);
    }

}
