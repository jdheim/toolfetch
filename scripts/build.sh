#!/usr/bin/env bash
# BUILDS PROJECT

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

[[ -f "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh" ]] && . "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Builds whole project

OPTIONS:
  -c                     Perform clean build
  -d                     Dry-run JReleaser
  -n, --native           Create a standalone executable (native image)
  --native-debug         Create a standalone executable (native image) with debug enabled
  --native-maven         Create a standalone executable (native image) using native-maven-plugin
  --native-prepare       Builds whole project and prepares native setup
EOF
  exit 1
}

main() {
  cd ..
  readOptions "$@"
  build
}

readOptions() {
  while [[ "$#" -gt 0 ]]; do
    case "${1}" in
      -c) phases=("clean") ;;
      -d) validateJReleaserGitHubToken; isJReleaserFullReleaseDryRun="true"; enrichNativeProfiles ;;
      -n|--native) isNativeImage="true"; enrichNativeProfiles ;;
      --native-maven) export GRAALVM_HOME="target/jdks/graalvm-linux-amd64/graalvm-jdk-25.0.2"; remainingOptions+=("-Psetup-graalvm" "-Pnative-image" "-Pnative-image-with-maven" "-Dcyclonedx.skipAttach=true") ;;
      --native-prepare) enrichNativeProfiles ;;
      --native-debug) isNativeImage="true"; isNativeImageDebug="true"; enrichNativeProfiles ;;
      -h|--help) usage ;;
      *) remainingOptions+=("${1}") ;;
    esac
    shift
  done
  phases+=("verify")
}

enrichNativeProfiles() {
  remainingOptions+=("-Psetup-graalvm" "-Pnative-image")
}

build() {
  step "Build Project"
  run ./mvnw -ntp "${phases[@]}" -DskipTests -Padd-third-party "${remainingOptions[@]}"
  scripts/common/updateNotice.sh
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
    jreleaserAssemble "JRELEASER_ASSEMBLE_TOOLFETCH_SBOM_ACTIVE=ALWAYS"
    jreleaserFullReleaseDryRun "${archiveEnvs[@]}" "JRELEASER_SELECT_CURRENT_PLATFORM=true"
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
