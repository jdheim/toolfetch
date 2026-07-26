/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.io.InputStream;
import java.net.http.HttpResponse;

public interface FileNameResolver {

    String resolve(HttpResponse<InputStream> httpResponse);

}
