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
  -d                     Dry-run JReleaser release
  -n                     Create a standalone executable (native image)
  --nd                   Create a standalone executable (native image) with debug enabled
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
      -d) dryRunJReleaserRelease "$@" ;;
      -n) isNativeImage="true" ;;
      --nd) isNativeImage="true"; isNativeImageDebug="true" ;;
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
  shift
  JRELEASER_GITHUB_TOKEN=${GITHUB_TOKEN-} run jreleaser release --dry-run --output-directory=target "$@"
  exit $?
}

build() {
  step "Build Project"
  if [[ "${isNativeImage:-false}" == "true" ]]; then
    local mavenCompilerSource
    mavenCompilerSource="$(xmlstarlet sel -N "n=http://maven.apache.org/POM/4.0.0" -t -v "/n:project/n:properties/n:maven.compiler.source" "pom.xml")"
    run docker pull ghcr.io/graalvm/native-image-community:"${mavenCompilerSource}"
    local dockerTty mavenColors nativeImageDebugProfile nativeImageSharedLibrarySetup nativeImageSharedLibraryCopy
    if [[ "${GITHUB_ACTIONS:-}" != "true" ]]; then
      dockerTty="-t"
      mavenColors="--color=always"
    fi
    if [[ "${isNativeImageDebug:-false}" == "true" ]]; then
      nativeImageDebugProfile="-Pnative-image-debug"
      nativeImageSharedLibrarySetup="mkdir /tmp/.graalvm-jdwp; native-image --macro:svmjdwp-library -o /tmp/.graalvm-jdwp/libsvmjdwp &&"
      nativeImageSharedLibraryCopy="&& cp /tmp/.graalvm-jdwp/libsvmjdwp.so target/"
    fi
    run docker container run ${dockerTty:-} --rm --name "graalvm" -u "$(id -u):$(id -g)" \
      -v "$HOME/.m2":/home/user/.m2 \
      -v "$HOME/.config":/home/user/.config \
      -v "$PWD":/work \
      -w /work \
      -e HOME="/home/user" \
      --entrypoint "/bin/bash" \
      ghcr.io/graalvm/native-image-community:"${mavenCompilerSource}" \
      -lc "${nativeImageSharedLibrarySetup:-} ./mvnw clean package -Pnative-image ${nativeImageDebugProfile:-} -DskipTests ${mavenColors:-} ${remainingOptions[*]} ${nativeImageSharedLibraryCopy:-}"
  else
    run ./mvnw clean install -DskipTests "${remainingOptions[@]}"
  fi
}

main "$@"
