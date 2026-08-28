#!/usr/bin/env bash
# REGENERATE MAVEN WRAPPER

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

source "$(dirname "${BASH_SOURCE[0]}")/common/projectFunctions.sh"

usage() {
  cat << EOF
Usage: $(basename "$0")

Regenerate Maven Wrapper
EOF
  return 1
}

main() {
  readOptions "$@"
  (
    cd "${PROJECT_DIR}"
    regenerateMavenWrapper
  )
}

readOptions() {
  while (( $# > 0 )); do
    case "$1" in
      *) usage ;;
    esac
    shift
  done
}

regenerateMavenWrapper() {
  step "Regenerate Maven Wrapper"
  local newValue
  newValue="$(mvn -B -v | grep "Apache Maven" | sed "s/Apache Maven \([^ ]*\).*/\1/")"
  mvn wrapper:wrapper -pl . -Dmaven="${newValue}"
  updatePropertyInXmlFile "${PROJECT_DIR}/pom.xml" "n=http://maven.apache.org/POM/4.0.0" "/n:project/n:properties/n:enforce-maven.version" "${newValue}"
  replaceHttpWithHttps
}

replaceHttpWithHttps() {
  local file files
  files="$(grep -Ril --exclude-dir=target --exclude-dir=scripts "http://www.apache.org" "${PROJECT_DIR}")"
  if [[ -z "${files}" ]]; then
    error "The file with \"http://www.apache.org\" does not exist"
    return 1
  fi
  while IFS= read -r file; do
    sed -i 's|http\(://www.apache.org\)|https\1|' "${file}"
  done <<< "${files}"
}

main "$@"
