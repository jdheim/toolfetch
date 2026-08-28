#!/usr/bin/env bash
# CREATES BADGE

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

if [[ -n "${CREATE_BADGE_LOADED:-}" ]]; then
  return 0
fi
declare -r CREATE_BADGE_LOADED="true"
source "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

readonly BADGES_DIR="${PROJECT_DIR}/target/badges"

# createPercentageBadge "test-coverage" "sonarqubeserver" "Test Coverage" "73.9%"
createPercentageBadge() {
  local jsonFilename="${1}"
  local namedLogo="${2}"
  local logoColor="white"
  local label="${3}"
  local message="${4}"

  step "Create ${jsonFilename} percentage badge"

  local percentage="${message%%%}"
  local color="red"
  local percentageInt="${percentage%.*}"
  if (( percentageInt >= 90 )); then
    color="brightgreen"
  elif (( percentageInt >= 80 )); then
    color="green"
  elif (( percentageInt >= 70 )); then
    color="yellowgreen"
  elif (( percentageInt >= 50 )); then
    color="yellow"
  elif (( percentageInt >= 30 )); then
    color="orange"
  fi

  [[ ! -d "${BADGES_DIR}" ]] && mkdir -p "${BADGES_DIR}"

  jq -n \
    --arg namedLogo "${namedLogo}" \
    --arg logoColor "${logoColor}" \
    --arg label "${label}" \
    --arg message "${message}" \
    --arg color "${color}" \
    '{
       schemaVersion: 1,
       namedLogo: $namedLogo,
       logoColor: $logoColor,
       label: $label,
       message: $message,
       color: $color
     }' > "${BADGES_DIR}/${jsonFilename}.json"
}

# createStatusBadge "sonarqube-code-issues" "sonarqubeserver" "SonarQube Code Issues" "OK" "0"
createStatusBadge() {
  local jsonFilename="${1}"
  local namedLogo="${2}"
  local logoColor="white"
  local label="${3}"
  local status="${4}"
  local message="${5:-}"

  step "Create ${jsonFilename} status badge"

  local color="red"
  case "${status}" in
    OK|SUCCESS) color="brightgreen"; [[ -z "${message}" ]] && message="passing" ;;
    WARN|WARNING) color="yellow"; [[ -z "${message}" ]] && message="unstable" ;;
    *) ;;
  esac

  [[ ! -d "${BADGES_DIR}" ]] && mkdir -p "${BADGES_DIR}"

  jq -n \
    --arg namedLogo "${namedLogo}" \
    --arg logoColor "${logoColor}" \
    --arg label "${label}" \
    --arg message "${message:-failing}" \
    --arg color "${color}" \
    '{
       schemaVersion: 1,
       namedLogo: $namedLogo,
       logoColor: $logoColor,
       label: $label,
       message: $message,
       color: $color
     }' > "${BADGES_DIR}/${jsonFilename}.json"
}
