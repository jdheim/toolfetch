/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.model.http;

import com.jdheim.toolfetch.model.http.ssl.Ssl;
import org.jspecify.annotations.Nullable;

public record Http(@Nullable Integer connectTimeout, @Nullable Integer requestTimeout, @Nullable Ssl ssl) {}
