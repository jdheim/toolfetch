/*
 * © 2026-2026 JDHeim.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdheim.toolfetch.service.install.resolve;

import java.nio.file.Path;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import org.apache.commons.lang3.StringUtils;

public class ToolDestinationResolver implements DestinationResolver {

    @Override
    public Path resolve(Configuration configuration, Tool tool) {
        return Path.of("%s/%s".formatted(StringUtils.firstNonEmpty(tool.destination(), configuration.destination()), tool.id()));
    }

}
