#!/usr/bin/env bash
# BUILDS WHOLE PROJECT

#
# © 2026-2026 JDHeim.com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

[[ -f "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh" ]] && . "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Builds whole project

OPTIONS:
  -d                     Dry-run JReleaser release
  -u                     Update versions
  -v                     Version check
EOF
  exit 1
}

main() {
  cd ..
  readOptions "$@"
  scripts/common/updateVersion.sh "$(getProjectVersion)"
  scripts/common/updateCopyright.sh
  mvnCleanInstall
}

readOptions() {
  while [[ "$#" -gt 0 ]]; do
    case "${1}" in
      -d) dryRunJReleaserRelease ;;
      -u) updateVersions ;;
      -v) versionCheck ;;
      -h|--help) usage ;;
      *) remainingOptions+=("${1}") ;;
    esac
    shift
  done
}

dryRunJReleaserRelease() {
  step "Dry-run JReleaser release"
  if [[ -z "${GITHUB_TOKEN-}" ]]; then
    echo -e "${ERROR} The GITHUB_TOKEN env variable is not set"
    exit 1
  fi
  JRELEASER_GITHUB_TOKEN=${GITHUB_TOKEN-} run jreleaser release --dry-run --output-directory=target
  exit $?
}

updateVersions() {
  local mavenVersionIgnore=".*-M-?[0-9]+,.*-alpha-?[0-9]+,.*-beta-?[0-9]+"
  ./mvnw versions:update-properties \
    -pl . \
    -DgenerateBackupPoms=false \
    -Dmaven.version.ignore="${mavenVersionIgnore}"
  exit $?
}

versionCheck() {
  step "Maven - Version Check"
  local mavenVersionIgnore=".*-M-?[0-9]+,.*-alpha-?[0-9]+,.*-beta-?[0-9]+"
  ./mvnw versions:display-property-updates \
    -pl . \
    -Dmaven.version.ignore="${mavenVersionIgnore}"
  exit $?
}

mvnCleanInstall() {
  step "Clean and Install"
  run ./mvnw clean install "${remainingOptions[@]}"
}

main "$@"
