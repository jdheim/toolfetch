/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.logging.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.jdheim.toolfetch.logging.LogMarker;
import org.jline.jansi.Ansi;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/// OOC Tests for [JansiLogLevelConverter]
class JansiLogLevelConverterTest {

    private static final String ANSI = "\u001B[";

    private JansiLogLevelConverter converter;

    private boolean ansiEnabled;

    @BeforeEach
    void enableAnsi() {
        ansiEnabled = Ansi.isEnabled();
        Ansi.setEnabled(true);
        converter = new JansiLogLevelConverter();
    }

    @AfterEach
    void rollbackAnsi() {
        Ansi.setEnabled(ansiEnabled);
    }

    @Test
    void testConvertKnownLevels() {
        assertConvert(Level.ERROR, colored("31", "ERROR"));
        assertConvert(Level.WARN, colored("33", "WARN"));
        assertConvert(Level.INFO, colored("34", "INFO"));
        assertConvert(Level.DEBUG, colored("32", "DEBUG"));
        assertConvert(Level.TRACE, colored("90", "TRACE"));
    }

    @Test
    void testConvertOtherLevel() {
        assertConvert(Level.OFF, colored("90", "OFF"));
    }

    @Test
    void testConvertStepMarker() {
        assertConvert(Level.INFO, MarkerFactory.getMarker("OTHER"), colored("34", "INFO"));
        assertConvert(Level.INFO, MarkerFactory.getMarker(LogMarker.STEP.name()), colored("36", "STEP"));
    }

    private void assertConvert(Level level, String expected) {
        assertConvert(level, null, expected);
    }

    private void assertConvert(Level level, @Nullable Marker marker, String expected) {
        List<Marker> markers = marker != null ? List.of(marker) : List.of();
        ILoggingEvent event = mock();
        when(event.getLevel()).thenReturn(level);
        when(event.getMarkerList()).thenReturn(markers);

        assertThat(converter.convert(event)).isEqualTo(expected);
    }

    private String colored(String color, String value) {
        return "[" + ANSI + color + ";1m" + value + ANSI + "m]";
    }

}
