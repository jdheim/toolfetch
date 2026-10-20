/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.step.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

public class TestLogListAppenderSteps extends ListAppender<ILoggingEvent>
        implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        list.clear();
        start();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Logger logger = (Logger) LoggerFactory.getLogger("com.jdheim");
        logger.detachAppender(this);
        stop();
    }

    public List<String> logs() {
        return formatLogs(list);
    }

    public List<String> messages() {
        return list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    public void assertAnyMatch(String level, String message) {
        assertThat(formatLogs(list)).anyMatch(line -> line.contains("[%s] %s".formatted(level, message)));
    }

    public void assertNoMatch(String level, String message) {
        assertThat(formatLogs(list)).noneMatch(line -> line.contains("[%s] %s".formatted(level, message)));
    }

    public void assertExceptionLogged(Class<? extends Throwable> exceptionType) {
        assertThat(list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getThrowableProxy()).isNotNull().satisfies(throwable -> {
                assertThat(throwable.getClassName()).isEqualTo(exceptionType.getName());
                assertThat(throwable.getStackTraceElementProxyArray()).isNotEmpty();
            });
        });
        assertThat(list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
                .allSatisfy(event -> assertThat(event.getThrowableProxy()).isNull());
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
        super.start();
    }

}
