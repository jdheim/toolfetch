#!/usr/bin/env bash
# COMMON FUNCTIONS

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

set -o errexit  # ABORT ON NON-ZERO EXIT STATUS
set -o nounset  # TREAT UNSET VARIABLES AS AN ERROR AND EXIT
set -o pipefail # DON'T HIDE ERRORS WITHIN PIPES

: "${INFO:=$'[\033[1;34mINFO\033[0m]'}"
: "${WARN:=$'[\033[1;33mWARN\033[0m]'}"
: "${ERROR:=$'[\033[1;31mERROR\033[0m]'}"
: "${MAVEN_VERSION_IGNORE:=$'.*-(M|alpha|beta|rc)[-.]?[0-9]+'}"

readonly INFO WARN ERROR MAVEN_VERSION_IGNORE

# General functions

## step "Message"
step() {
  local step="[\e[1;96mSTEP\e[0m]"
  local line="\e[1;96m-----\e[0m"
  local message="${1}"
  echo -e "${step} ${line} ${message} ${line}"
}

## run command --argument
run() {
	echo -e "${INFO} \e[1m$\e[0m $*"; "$@"
}

## fileSize "file"
fileSize() {
  local file="${1}"
  numfmt --to=iec --suffix=B --format="%.2f" "$(stat -c %s "${file}")"
}

getProjectVersion() {
  yq -r ".project.version" "jreleaser.yml" | sed "s/-.*//"
}

getProjectArtifactId() {
  xmlstarlet sel -N "n=http://maven.apache.org/POM/4.0.0" -t -v "/n:project/n:artifactId" "pom.xml"
}

getProjectGroupId() {
  xmlstarlet sel -N "n=http://maven.apache.org/POM/4.0.0" -t -v "/n:project/n:groupId" "pom.xml"
}

getProjectModules() {
  xmlstarlet sel -N "n=http://maven.apache.org/POM/4.0.0" -t -v "/n:project/n:modules/n:module" "pom.xml"
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

## updateCopyrightInFile "README.md"
updateCopyrightInFile() {
  local file="${1}"
  local oldValue newValue
  copyrightsStartYear="$(grep -E "© [0-9]{4}-[0-9]{4} JDHeim.com" "${file}" | sed -E 's/.*([0-9]{4})-.*/\1/')"
  oldValue="$(grep -E "© [0-9]{4}-[0-9]{4} JDHeim.com" "${file}" | sed -E 's/.*-([0-9]{4}).*/\1/')"
  newValue="$(date +%Y)"
  if [[ "${oldValue}" != "${newValue}" ]]; then
    echo -e "${INFO} Updating ${file}: ${copyrightsStartYear}-${oldValue} -> ${copyrightsStartYear}-${newValue}"
    sed -i "s/\(©\).*\(JDHeim.com\)/\1 ${copyrightsStartYear}-${newValue} \2/" "${file}"
    isUpdated=true
  fi
}

## findAndUpdateCopyright "*.md"
findAndUpdateCopyright() {
  local name="${1}"
  for file in $(grep -ERil --include="${name}" --exclude-dir={target} "© [0-9]{4}-[0-9]{4} JDHeim.com"); do
    updateCopyrightInFile "${file}"
  done
}

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
