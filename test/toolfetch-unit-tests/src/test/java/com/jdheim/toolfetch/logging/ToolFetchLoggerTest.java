/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.jdheim.toolfetch.step.log.TestLogListAppenderSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

/// OOC Tests for [ToolFetchLogger]
class ToolFetchLoggerTest {

    private ToolFetchLogger logger;

    private TestLogListAppenderSteps testLogListAppenderSteps;

    @BeforeEach
    void setUp() {
        logger = ToolFetchLogger.getLogger(ToolFetchLoggerTest.class);
        testLogListAppenderSteps = new TestLogListAppenderSteps();
        testLogListAppenderSteps.start();
    }

    @Test
    void testLogWithoutArgument() {
        assertThatIllegalArgumentException().isThrownBy(() -> logger.log("info"))
                .withMessage("Log message requires an argument: info,{}");
    }

    @Test
    void testLogWithoutArguments() {
        try (var _ = mockMessages("info", "info,This is 1st arg={}, this is 2nd arg={}")) {
            assertThatIllegalArgumentException().isThrownBy(() -> logger.log("info"))
                    .withMessage("Log message requires an argument: info,This is 1st arg={}, this is 2nd arg={}");
        }
    }

    @Test
    void testLogWithOneArgument() {
        try (var _ = mockMessages("info", "info,This is 1st arg={}, this is 2nd arg={}")) {
            assertThatIllegalArgumentException().isThrownBy(() -> logger.log("info", "ABC"))
                    .withMessage("Log message requires an argument: info,This is 1st arg=ABC, this is 2nd arg={}");
        }
    }

    @Test
    void testLogWithArgument() {
        logger.log("info", "info message");

        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "info message");
    }

    @Test
    void testLogException() {
        RuntimeException exception = new RuntimeException("exception message");

        logger.log(exception, "info", "info message");

        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "info message");
        assertThat(testLogListAppenderSteps.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("Exception stack trace:");
            assertThat(event.getThrowableProxy()).satisfies(throwable -> {
                assertThat(throwable.getClassName()).isEqualTo(RuntimeException.class.getName());
                assertThat(throwable.getMessage()).isEqualTo("exception message");
            });
        });
    }

    @Test
    void testLogWithNullArgument() {
        String nullArgument = null;
        logger.log("info", nullArgument);

        testLogListAppenderSteps.assertAnyMatch(LogLevel.INFO.toString(), "null");
    }

    @Test
    void testLogError() {
        logger.log("error", "error message");

        testLogListAppenderSteps.assertAnyMatch(LogLevel.ERROR.toString(), "error message");
    }

    @Test
    void testLogWarn() {
        logger.log("warn", "warn message");

        testLogListAppenderSteps.assertAnyMatch(LogLevel.WARN.toString(), "warn message");
    }

    @Test
    void testLogStep() {
        logger.log("step", "step message");

        testLogListAppenderSteps.assertAnyMatch(LogMarker.STEP.toString(), "=== step message ===");
    }

    @Test
    void testLogDebug() {
        logger.log("debug", "debug message");

        testLogListAppenderSteps.assertAnyMatch(LogLevel.DEBUG.toString(), "debug message");
    }

    @Test
    void testLogTrace() {
        try (var _ = mockMessages("trace", "trace,{}")) {
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> logger.log("trace", "trace message"))
                    .withMessage("Trace logging must check ToolFetchLogger.isTraceEnabled() and use ToolFetchLogger.trace()");
        }
    }

    @Test
    void testTraceWhenDisabled() {
        assertThat(logger.isTraceEnabled()).isFalse();

        logger.trace("trace message");

        testLogListAppenderSteps.assertNoMatch(LogLevel.TRACE.toString(), "trace message");
    }

    @Test
    void testTraceWhenEnabled() {
        Logger delegate = (Logger) LoggerFactory.getLogger(ToolFetchLoggerTest.class);
        Level originalLevel = delegate.getLevel();
        delegate.setLevel(Level.TRACE);
        try {
            assertThat(logger.isTraceEnabled()).isTrue();

            logger.trace("trace message");

            testLogListAppenderSteps.assertAnyMatch(LogLevel.TRACE.toString(), "trace message");
        } finally {
            delegate.setLevel(originalLevel);
        }
    }

    @Test
    void testLogMessageWithoutDelimiter() {
        try (var _ = mockMessages("invalid", "invalid message")) {
            assertThatIllegalArgumentException().isThrownBy(() -> logger.log("invalid"))
                    .withMessage("Invalid log message 'invalid message'. Expected format: <level>,<message>");
        }
    }

    @Test
    void testLogMessageWithoutText() {
        try (var _ = mockMessages("invalid", "info, ")) {
            assertThatIllegalArgumentException().isThrownBy(() -> logger.log("invalid"))
                    .withMessage("Log message must not be empty: 'info, '");
        }
    }

    @Test
    void testLogMessageWithUnsupportedLevel() {
        try (var _ = mockMessages("invalid", "invalid,invalid message")) {
            assertThatIllegalArgumentException().isThrownBy(() -> logger.log("invalid"))
                    .withMessage("Unsupported log level 'invalid' in message 'invalid,invalid message'");
        }
    }

    private MockedStatic<ToolFetchLogger> mockMessages(String key, String fullMessage) {
        ResourceBundle messages = mock();
        when(messages.getString(key)).thenReturn(fullMessage);
        MockedStatic<ToolFetchLogger> toolFetchLogger = mockStatic(ToolFetchLogger.class, CALLS_REAL_METHODS);
        toolFetchLogger.when(ToolFetchLogger::messages).thenReturn(messages);
        return toolFetchLogger;
    }

}
