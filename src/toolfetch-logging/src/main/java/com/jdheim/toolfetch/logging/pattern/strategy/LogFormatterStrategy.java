/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern.strategy;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface LogFormatterStrategy {

    String convertLogLevel(ILoggingEvent event);

    String convertLogMessage(ILoggingEvent event);

}
