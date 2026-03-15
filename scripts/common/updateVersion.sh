#!/usr/bin/env bash
# UPDATES VERSION

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

[[ -f "$(dirname "${BASH_SOURCE[0]}")/functions.sh" ]] && . "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

main() {
  local version="${1-}"
  step "Update version"
  if [[ -z "${version}" ]]; then
    echo -e "${ERROR} Version is missing"
    exit 1
  fi
  updatePropertyInXmlFile "pom.xml" "n=http://maven.apache.org/POM/4.0.0" "/n:project/n:version" "${version}"
  for module in $(getProjectModules); do
    updatePropertyInXmlFile "${module}/pom.xml" "n=http://maven.apache.org/POM/4.0.0" "/n:project/n:parent/n:version" "${version}"
  done
  if [[ "${isUpdated:-false}" == false ]]; then
    echo -e "${WARN} Nothing to update"
  fi
}

main "$@"
