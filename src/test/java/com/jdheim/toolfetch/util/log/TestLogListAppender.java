/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.util.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;

public class TestLogListAppender extends ListAppender<ILoggingEvent> {

    public void assertAnyMatch(Level level, String message) {
        assertThat(list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(level);
            assertThat(event.getFormattedMessage()).contains(message);
        });
    }

    public void assertNoErrorNoWarn() {
        assertThat(list).noneMatch(event -> event.getLevel() == Level.ERROR || event.getLevel() == Level.WARN);
    }

    public void assertNoError() {
        assertThat(list).noneMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Override
    public void start() {
        start(ArrayUtils.EMPTY_CLASS_ARRAY);
    }

    public void start(Class<?>... classes) {
        if (classes.length < 1) {
            throw new IllegalArgumentException("At least one class for logger name is expected");
        }
        Arrays.stream(classes).forEach(clazz -> {
            Logger logger = (Logger) LoggerFactory.getLogger(clazz);
            logger.addAppender(this);
            if (getContext() == null) {
                setContext(logger.getLoggerContext());
            }
        });
        super.start();
    }

}
