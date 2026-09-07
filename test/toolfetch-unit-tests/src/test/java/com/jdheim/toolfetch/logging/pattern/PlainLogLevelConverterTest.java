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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/// OOC Tests for [PlainLogLevelConverter]
class PlainLogLevelConverterTest {

    private PlainLogLevelConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PlainLogLevelConverter();
    }

    @Test
    void testConvertKnownLevels() {
        assertConvert(Level.ERROR, "[ERROR]");
        assertConvert(Level.WARN, "[WARN]");
        assertConvert(Level.INFO, "[INFO]");
        assertConvert(Level.DEBUG, "[DEBUG]");
        assertConvert(Level.TRACE, "[TRACE]");
    }

    @Test
    void testConvertOtherLevel() {
        assertConvert(Level.OFF, "[OFF]");
    }

    @Test
    void testConvertMarkers() {
        assertConvert(Level.INFO, MarkerFactory.getMarker("OTHER"), "[INFO]");
        assertConvert(Level.INFO, MarkerFactory.getMarker(LogMarker.STEP.name()), "[STEP]");
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

}
