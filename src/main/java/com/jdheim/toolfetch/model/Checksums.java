/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

public class Checksums {

    private final Map<Algorithm, String> values;

    @JsonCreator
    public Checksums(Map<String, String> rawValues) {
        EnumMap<Algorithm, String> checksums = new EnumMap<>(Algorithm.class);
        if (rawValues != null) {
            rawValues.forEach(
                    (configurationKey, value) -> checksums.put(Algorithm.fromConfigurationKey(configurationKey), value));
        }
        this.values = Map.copyOf(checksums);
    }

    public Map<Algorithm, String> values() {
        return values;
    }

    @JsonValue
    public Map<String, String> rawValues() {
        Map<String, String> rawValues = HashMap.newHashMap(values.size());
        values.forEach((algorithm, value) -> rawValues.put(algorithm.configurationKey, value));
        return Map.copyOf(rawValues);
    }

    public enum Algorithm {

        SHA256("sha256", MessageDigestAlgorithms.SHA_256),
        SHA384("sha384", MessageDigestAlgorithms.SHA_384),
        SHA512("sha512", MessageDigestAlgorithms.SHA_512);

        private final String configurationKey;

        private final String value;

        Algorithm(String configurationKey, String value) {
            this.configurationKey = configurationKey;
            this.value = value;
        }

        public static Algorithm fromConfigurationKey(String configurationKey) {
            return Arrays.stream(values())
                    .filter(algorithm -> algorithm.configurationKey.equals(configurationKey))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown checksum algorithm: " + configurationKey));
        }

        @Override
        public String toString() {
            return value;
        }

    }

}
