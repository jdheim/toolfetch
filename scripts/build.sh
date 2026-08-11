#!/usr/bin/env bash
# BUILD PROJECT

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

source "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Build Project

OPTIONS:
  -r, --release          Build standalone executable using native image and run JReleaser release in dry-run mode
  -n, --native           Build standalone executable using native image
  --native-debug         Build standalone executable using native image with debug enabled
  --native-maven         Build standalone executable using native-maven-plugin
  --native-prepare       Build Project and prepare the native image setup
EOF
  return 1
}

main() {
  [[ ${PWD} == */scripts ]] && cd ..
  readOptions "$@"
  build
}

readOptions() {
  while (( $# > 0 )); do
    case "${1}" in
      -r|--release) validateJReleaserGitHubToken; isJReleaserFullReleaseDryRun="true"; enrichNativeOptions ;;
      -n|--native) isNativeImage="true"; enrichNativeOptions ;;
      --native-debug) isNativeImage="true"; isNativeImageDebug="true"; enrichNativeOptions ;;
      --native-maven) local graalVmJdkVersion
        graalVmJdkVersion="$(xmlProperty "graalvm-jdk.version" "pom.xml")"
        export GRAALVM_HOME="target/jdks/graalvm-linux-amd64/graalvm-jdk-${graalVmJdkVersion}"
        remainingOptions+=("-Pnative-image-with-maven")
        enrichNativeOptions ;;
      --native-prepare) enrichNativeOptions ;;
      -h|--help) usage ;;
      *) remainingOptions+=("${1}") ;;
    esac
    shift
  done
  addMvnPhase "clean"
  addMvnPhase "install"
}

enrichNativeOptions() {
  remainingOptions+=("-DskipAllTests" "-Psetup-graalvm" "-Pnative-image")
}

addMvnPhase() {
  local phase existingPhase
  phase="${1}"
  for existingPhase in "${phases[@]}"; do
    [[ "${existingPhase}" == "${phase}" ]] && return
  done
  if [[ "${phase}" == "clean" ]]; then
    phases=("${phase}" "${phases[@]}")
  else
    phases+=("${phase}")
  fi
}

build() {
  step "Build Project"
  run ./mvnw -ntp "${phases[@]}" -DskipTests "${remainingOptions[@]}"
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
    return 1
  fi
}

jreleaserAssemble() {
  step "JReleaser: Assemble"
  run export "$@"
  run jreleaser assemble --output-directory=build/toolfetch-native-image/target
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
  run jreleaser full-release --dry-run --output-directory=build/toolfetch-native-image/target
}

main "$@"
