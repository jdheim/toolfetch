/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.logging.pattern.strategy.LogFormatterStrategy;
import com.jdheim.toolfetch.logging.pattern.strategy.PlainLogFormatterStrategy;

public final class PlainLogMessageConverter extends ClassicConverter {

    private static final LogFormatterStrategy LOG_FORMATTER_STRATEGY = new PlainLogFormatterStrategy();

    @Override
    public String convert(ILoggingEvent event) {
        return LOG_FORMATTER_STRATEGY.convertLogMessage(event);
    }

}
