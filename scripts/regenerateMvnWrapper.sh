#!/usr/bin/env bash
# REGENERATE MAVEN WRAPPER

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
Usage: $(basename "$0")

Regenerate Maven Wrapper
EOF
  exit 1
}

main() {
  cd ..
  readOptions "$@"
  regenerateMavenWrapper
}

readOptions() {
  while getopts ":h" option; do
    case "${option}" in
      h|?) usage ;;
    esac
  done
  shift $((OPTIND - 1))
}

regenerateMavenWrapper() {
  step "Regenerate Maven Wrapper"
  local newValue
  newValue="$(mvn -B -v | grep "Apache Maven" | sed "s/Apache Maven \([^ ]*\).*/\1/")"
  mvn wrapper:wrapper -pl . -Dmaven="${newValue}"
  updatePropertyInXmlFile "pom.xml" "n=http://maven.apache.org/POM/4.0.0" "/n:project/n:properties/n:enforce-maven.version" "${newValue}"
  replaceHttpWithHttps
}

replaceHttpWithHttps() {
  for file in $(grep -Ril --exclude-dir={target,scripts} "http://www.apache.org"); do
    sed -i "s|http\(://www.apache.org\)|https\1|" "${file}"
  done
}

main "$@"
