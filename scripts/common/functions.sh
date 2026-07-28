#!/usr/bin/env bash
# COMMON FUNCTIONS

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

set -o errexit  # ABORT ON NON-ZERO EXIT STATUS
set -o nounset  # TREAT UNSET VARIABLES AS AN ERROR AND EXIT
set -o pipefail # DON'T HIDE ERRORS WITHIN PIPES

: "${INFO:=$'[\033[1;34mINFO\033[0m]'}"
: "${WARN:=$'[\033[1;33mWARN\033[0m]'}"
: "${ERROR:=$'[\033[1;31mERROR\033[0m]'}"
: "${MAVEN_VERSION_IGNORE:=".*-(M|alpha|beta|rc)[-.]?[0-9]+"}"
: "${PROPERTY_UPDATES_FILE:="property-updates.txt"}"
: "${PROPERTY_UPDATES_PATH:="target/${PROPERTY_UPDATES_FILE}"}"

readonly INFO WARN ERROR MAVEN_VERSION_IGNORE PROPERTY_UPDATES_FILE PROPERTY_UPDATES_PATH

# General functions

## step "Message"
step() {
  local step="[\e[1;96mSTEP\e[0m]"
  local line="\e[1;96m===\e[0m"
  local message="${1}"
  echo -e "${step} ${line} ${message} ${line}"
}

## run command --argument
run() {
	echo -e "${INFO} \e[1m$\e[0m $*"; "$@"
}

## trim "string"
trim() {
  local string="${1}"
  string="${string#"${string%%[![:space:]]*}"}"
  string="${string%"${string##*[![:space:]]}"}"
  echo "${string}"
}

## fileSize "file"
fileSize() {
  local file="${1}"
  numfmt --to=iec --suffix=B --format="%.2f" "$(stat -c %s "${file}")"
}

projectVersion() {
  grep "version:" "jreleaser.yml" | awk '{print $2}' | tr -d "'\""
}

projectArtifactId() {
  xmlProperty "artifactId" "pom.xml"
}

projectGroupId() {
  xmlProperty "groupId" "pom.xml"
}

modulePath() {
  local module="${1}"
  if [[ -z "${module}" ]]; then
    echo -e "${ERROR} Provide module as parameter" >&2
    exit 1
  fi
  local modulePath
  modulePath="$(xmlProperty "module>.*/${module}</module" "pom.xml")"
  if [[ -z "${modulePath}" ]]; then
    echo -e "${ERROR} Module ${module} does not exists" >&2
    exit 1
  elif [[ ! -d "${modulePath}" ]]; then
    echo -e "${ERROR} Path to Module ${modulePath} does not exists" >&2
    exit 1
  fi
  echo "${modulePath}"
}

## xmlProperty "propertyName" "xmlFile"
xmlProperty() {
  local propertyName="<${1}>"
  local xmlFile="${2}"
  grep -m 1 "${propertyName}" "${xmlFile}" | awk -F'[<>]' '{print $3}'
}

## updatePropertyInXmlFile "pom.xml" "n=http://maven.apache.org/POM/4.0.0" "/n:project/n:version" "0.0.1"
updatePropertyInXmlFile() {
  local file="${1}"
  local namespace="${2}"
  local propertyName="${3}"
  local propertyValue="${4}"
  local oldValue
  oldValue=$(xmlstarlet sel -N "${namespace}" -t -v "${propertyName}" "${file}")
  if [[ "${oldValue}" != "${propertyValue}" ]]; then
    echo -e "${INFO} Updating ${file}: ${oldValue} -> ${propertyValue}"
    xmlstarlet ed --inplace -P -N "${namespace}" -u "${propertyName}" -v "${propertyValue}" "${file}"
    isUpdated=true
  fi
}

## githubStepSummary "${exitCode}" "${logFile}" "^[[]INFO[]] Check for updates complete"
githubStepSummary() {
  local exitCode=${1}
  local logFile="${2}"
  local startPattern="${3}"
  local endPattern="${4:-"^[[]ERROR[]] Re-run Maven"}"
  {
    echo -n "### Status: "
    if (( exitCode == 0 )); then
      echo -e "✅ OK"
    else
      echo -e "❌ ERROR"
      echo
      echo "<details><summary>Logs</summary>"
      echo
      echo '```text'
      awk -v start="${startPattern}" -v end="${endPattern}" '$0 ~ start {p=1} p {print} $0 ~ end {exit}' "${logFile}"
      echo '```'
      echo
      echo "</details>"
    fi
  } >> "${GITHUB_STEP_SUMMARY}"
}
