/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model.http.ssl;

import com.jdheim.toolfetch.model.http.ssl.truststore.TrustStore;
import org.jspecify.annotations.Nullable;

public record Ssl(@Nullable TrustStore trustStore) {}
