/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.step.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

public class TestLogListAppenderSteps extends ListAppender<ILoggingEvent> {

    public void assertAnyMatch(String level, String message) {
        assertThat(formatLogs(list)).anyMatch(line -> line.contains("[%s] %s".formatted(level, message)));
    }

    public void assertNoMatch(String level, String message) {
        assertThat(formatLogs(list)).noneMatch(line -> line.contains("[%s] %s".formatted(level, message)));
    }

    public void assertNoErrorNoWarn() {
        assertThat(formatLogs(list)).noneMatch(line -> line.contains("[WARN]") || line.contains("[ERROR]"));
    }

    public void assertNoError() {
        assertThat(formatLogs(list)).noneMatch(line -> line.contains("[ERROR]"));
    }

    private List<String> formatLogs(List<ILoggingEvent> events) {
        PatternLayout layout = new PatternLayout();
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            layout.setContext(context);
            layout.setPattern("%toolFetchPlainLogLevel %toolFetchPlainLogMessage");
            layout.start();
            return events.stream().map(layout::doLayout).toList();
        } finally {
            layout.stop();
        }
    }

    @Override
    public void start() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.jdheim");
        logger.addAppender(this);
        if (getContext() == null) {
            setContext(logger.getLoggerContext());
        }
        PatternLayout layout = new PatternLayout();
        layout.setContext(getContext());
        layout.setPattern("%toolFetchPlainLogLevel %toolFetchPlainLogMessage");
        layout.start();
        super.start();
    }

}
