#!/usr/bin/env bash
# REGENERATE MAVEN WRAPPER

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
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
  [[ $PWD == */scripts ]] && cd ..
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
