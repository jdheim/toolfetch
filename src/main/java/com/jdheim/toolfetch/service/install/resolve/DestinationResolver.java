/*
 * Copyright 2026 JDHeim.com
 * SPDX-License-Identifier: Apache-2.0
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;

public interface DestinationResolver {

    Path resolve(Configuration configuration, Tool tool);

}
