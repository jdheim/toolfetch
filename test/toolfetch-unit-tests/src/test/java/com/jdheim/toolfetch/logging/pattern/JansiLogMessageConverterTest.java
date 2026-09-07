/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.logging.LogMarker;
import org.jline.jansi.Ansi;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/// OOC Tests for [JansiLogMessageConverter]
class JansiLogMessageConverterTest {

    private static final String ANSI = "\u001B[";

    private JansiLogMessageConverter converter;

    private boolean ansiEnabled;

    @BeforeEach
    void enableAnsi() {
        ansiEnabled = Ansi.isEnabled();
        Ansi.setEnabled(true);
        converter = new JansiLogMessageConverter();
    }

    @AfterEach
    void rollbackAnsi() {
        Ansi.setEnabled(ansiEnabled);
    }

    @Test
    void testConvertOrdinaryMessage() {
        assertConvert("ordinary message", "ordinary message");
    }

    @Test
    void testConvertStepMessage() {
        assertConvert(MarkerFactory.getMarker("OTHER"), "install tool", "install tool");
        assertConvert(MarkerFactory.getMarker(LogMarker.STEP.name()), "install tool",
                cyan("===") + " " + bold("install tool") + " " + cyan("==="));
    }

    private void assertConvert(String message, String expected) {
        assertConvert(null, message, expected);
    }

    private void assertConvert(@Nullable Marker marker, String message, String expected) {
        List<Marker> markers = marker != null ? List.of(marker) : List.of();
        ILoggingEvent event = mock();
        when(event.getMarkerList()).thenReturn(markers);
        when(event.getFormattedMessage()).thenReturn(message);

        assertThat(converter.convert(event)).isEqualTo(expected);
    }

    private String bold(String value) {
        return ANSI + "1m" + value + ANSI + "m";
    }

    private String cyan(String value) {
        return ANSI + "36;1m" + value + ANSI + "m";
    }

}
