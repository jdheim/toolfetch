/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
