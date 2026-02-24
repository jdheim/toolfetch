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

package com.jdheim.toolfetch.service.install;

import java.util.List;
import com.jdheim.toolfetch.model.Configuration;
import com.jdheim.toolfetch.model.Tool;
import com.jdheim.toolfetch.service.install.download.DownloadService;
import com.jdheim.toolfetch.service.install.download.WebDownloadService;
import com.jdheim.toolfetch.service.install.extract.ArchiveExtractService;
import com.jdheim.toolfetch.service.install.extract.ExtractService;

public class ArchiveInstallationService implements InstallationService {

    private final DownloadService downloadService;

    private final ExtractService extractService;

    public ArchiveInstallationService() {
        downloadService = new WebDownloadService();
        extractService = new ArchiveExtractService();
    }

    @Override
    public void install(Configuration configuration) {
        List<Tool> tools = configuration.tools();
        tools.forEach(tool -> downloadService.download(configuration, tool)
                .ifPresent(archivePath -> extractService.extract(configuration, tool, archivePath)));
    }

}
