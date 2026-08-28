#!/usr/bin/env bash
# PROJECT FUNCTIONS

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

if [[ -n "${PROJECT_FUNCTIONS_LOADED:-}" ]]; then
  return 0
fi
declare -r PROJECT_FUNCTIONS_LOADED="true"
source "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

# Project constants
declare -rx MAVEN_VERSION_IGNORE=".*-(M|alpha|beta|rc)[-.]?[0-9]+"

# Project paths
declare -rx PROPERTY_UPDATES_FILE="property-updates.txt"
declare -rx PROPERTY_UPDATES_PATH="target/${PROPERTY_UPDATES_FILE}"

# Project functions

projectVersion() {
  grep "version:" "${PROJECT_DIR}/jreleaser.yml" | awk '{print $2}' | tr -d "'\""
}

projectArtifactId() {
  xmlProperty "artifactId" "${PROJECT_DIR}/pom.xml"
}

projectGroupId() {
  xmlProperty "groupId" "${PROJECT_DIR}/pom.xml"
}

mvnw() {
  (
    cd "${PROJECT_DIR}"
    ./mvnw "$@"
  )
}

modulePath() {
  local module="${1}"
  if [[ -z "${module}" ]]; then
    error "Provide module as parameter" >&2
    return 1
  fi
  local modulePath
  modulePath="$(xmlProperty "module>.*/${module}</module" "${PROJECT_DIR}/pom.xml")"
  if [[ -z "${modulePath}" ]]; then
    error "Module ${module} does not exists" >&2
    return 1
  elif [[ ! -d "${modulePath}" ]]; then
    error "Path to Module ${modulePath} does not exists" >&2
    return 1
  fi
  printf '%s\n' "${modulePath}"
}

xmlProperty() {
  local propertyName="<${1}>"
  local xmlFile="${2}"
  grep -m 1 "${propertyName}" "${xmlFile}" | awk -F'[<>]' '{print $3}'
}

updatePropertyInXmlFile() {
  local file="${1}"
  local namespace="${2}"
  local propertyName="${3}"
  local propertyValue="${4}"
  local oldValue
  oldValue=$(xmlstarlet sel -N "${namespace}" -t -v "${propertyName}" "${file}")
  if [[ "${oldValue}" != "${propertyValue}" ]]; then
    info "Updating ${file}: ${oldValue} -> ${propertyValue}"
    xmlstarlet ed --inplace -P -N "${namespace}" -u "${propertyName}" -v "${propertyValue}" "${file}"
  fi
}
