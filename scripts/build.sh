#!/usr/bin/env bash
# BUILDS PROJECT

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
  -d                     Dry-run JReleaser
  -n, --native           Create a standalone executable (native image)
  --native-prepare       Builds whole project and prepares native setup
  --native-debug         Create a standalone executable (native image) with debug enabled
EOF
  exit 1
}

main() {
  cd ..
  readOptions "$@"
  scripts/common/updateVersion.sh "$(getProjectVersion)"
  scripts/common/updateCopyright.sh
  build
}

readOptions() {
  while [[ "$#" -gt 0 ]]; do
    case "${1}" in
      -d) validateJReleaserGitHubToken; isJReleaserFullReleaseDryRun="true"; enrichNativeProfiles ;;
      -n|--native) isNativeImage="true"; enrichNativeProfiles ;;
      --native-prepare) enrichNativeProfiles ;;
      --native-debug) isNativeImage="true"; isNativeImageDebug="true"; enrichNativeProfiles ;;
      -h|--help) usage ;;
      *) remainingOptions+=("${1}") ;;
    esac
    shift
  done
}

enrichNativeProfiles() {
  remainingOptions+=("-Psetup-graalvm" "-Pnative-image")
}

build() {
  step "Build Project"
  run ./mvnw clean install -DskipTests "${remainingOptions[@]}"
  local binaryEnvs=( "JRELEASER_ASSEMBLE_NATIVE_IMAGE_TOOLFETCH_BINARY_ACTIVE=ALWAYS" )
  if [[ "${isNativeImage:-false}" == "true" ]]; then
    if [[ "${isNativeImageDebug:-false}" == "true" ]]; then
      binaryEnvs=( "JRELEASER_ASSEMBLE_NATIVE_IMAGE_TOOLFETCH_DEBUG_ACTIVE=ALWAYS" )
    fi
    jreleaserAssemble "${binaryEnvs[@]}"
  elif [[ "${isJReleaserFullReleaseDryRun:-false}" == "true" ]]; then
    jreleaserAssemble "${binaryEnvs[@]}"
    local archiveEnvs=( "JRELEASER_ASSEMBLE_ARCHIVE_TOOLFETCH_ACTIVE=ALWAYS" )
    jreleaserAssemble "${archiveEnvs[@]}"
    jreleaserFullReleaseDryRun "${archiveEnvs[@]}"
  fi
}

validateJReleaserGitHubToken() {
  if [[ -z "${GITHUB_TOKEN-}" ]]; then
    echo -e "${ERROR} The GITHUB_TOKEN env variable is not set"
    exit 1
  fi
}

jreleaserAssemble() {
  step "JReleaser: Assemble"
  run export "$@"
  run jreleaser assemble --output-directory=target || exit 1
  for kv in "$@"; do
    run unset "${kv%%=*}"
  done
}

jreleaserFullReleaseDryRun() {
  step "JReleaser: Dry-run Full Release"
  local envs=( "JRELEASER_SIGNING_ACTIVE=NEVER" )
  if [[ -f "jdheim.asc" && -f "jdheim-private.asc" && -f "jdheim.passphrase" ]]; then
    envs=(
      "JRELEASER_SIGNING_ACTIVE=ALWAYS"
      "JRELEASER_GPG_PUBLIC_KEY=$(<jdheim.asc)"
      "JRELEASER_GPG_SECRET_KEY=$(<jdheim-private.asc)"
      "JRELEASER_GPG_PASSPHRASE=$(<jdheim.passphrase)"
    )
  fi
  run export "$@"
  export "JRELEASER_GITHUB_TOKEN=${GITHUB_TOKEN-}" "${envs[@]}"
  run jreleaser full-release --dry-run --output-directory=target
  exit $?
}

main "$@"
