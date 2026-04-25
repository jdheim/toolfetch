/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.net.URI;
import com.jdheim.toolfetch.model.tool.Tool;
import org.jspecify.annotations.Nullable;

public interface UriTransformer {

    @Nullable URI transform(Tool tool);

}
