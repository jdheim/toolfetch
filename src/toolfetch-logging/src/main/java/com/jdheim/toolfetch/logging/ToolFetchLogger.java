/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging;

import java.util.ResourceBundle;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/// Facade over an SLF4J [org.slf4j.Logger] that resolves
/// log messages from the ToolFetch resource bundle.
public final class ToolFetchLogger {

    private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("com.jdheim.bundle.log.messages");

    private static final String EXCEPTION_STACK_TRACE_MESSAGE = "Exception stack trace:";

    private final Logger delegate;

    private ToolFetchLogger(Class<?> type) {
        delegate = LoggerFactory.getLogger(type);
    }

    static ResourceBundle messages() {
        return MESSAGES;
    }

    public static ToolFetchLogger getLogger(Class<?> type) {
        return new ToolFetchLogger(type);
    }

    public void log(Exception exception, String key, @Nullable Object... args) {
        log(key, args);
        delegate.debug(EXCEPTION_STACK_TRACE_MESSAGE, exception);
    }

    public void log(String key, @Nullable Object... args) {
        String fullMessage = resolve(key, args);
        LogMessage logMessage = LogMessage.parse(fullMessage);
        logMessage.level().log(delegate, logMessage.message());
    }

    private String resolve(String key, @Nullable Object... args) {
        String fullMessage = messages().getString(key);
        if (ArrayUtils.isEmpty(args)) {
            return fullMessage;
        }
        return MessageFormatter.arrayFormat(fullMessage, args).getMessage();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public void trace(String message) {
        delegate.trace(message);
    }

}
