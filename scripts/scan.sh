#!/usr/bin/env bash
# SCANS PROJECT

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
[[ -f "$(dirname "${BASH_SOURCE[0]}")/common/sonarQube.sh" ]] && . "$(dirname "${BASH_SOURCE[0]}")/common/sonarQube.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Builds whole project

OPTIONS:
  --da                   Dependency Analyze
  --dt                   Dependency Tree
  -t, --jacoco-scan      Runs Tests with Jacoco Coverage Scan
  --jacoco-html          Launch default browser to check Jacoco Coverage Scan analysis result
  --owasp-scan           OWASP Scan
  --owasp-html           Launch default browser to check OWASP Scan analysis result
  --sonar-scan           Runs Tests with Sonar Scan
  --sonar-html           Launch default browser to check Sonar Scan analysis result
  --sonar-stop           Stop SonarQube server
  --spotbugs-scan        SpotBugs Scan
  --spotbugs-gui         SpotBugs Scan and launch GUI to check analysis result
  --spotbugs-html        Launch default browser to check SpotBugs Scan analysis result
  -u                     Version update
  -v                     Version check
EOF
  exit 1
}

main() {
  cd ..
  readOptions "$@"
  scan
}

readOptions() {
  while [[ "$#" -gt 0 ]]; do
    case "${1}" in
      --da|--dependency-analyze) dependencyAnalyze "$@" ;;
      --dt|--dependency-tree) dependencyTree "$@" ;;
      --jacoco-scan|-t) testsWithJacocoScan "$@" ;;
      --jacoco-html) jacocoHtml ;;
      --owasp-scan) owaspScan "$@" ;;
      --owasp-html) owaspHtml ;;
      --sonar-scan) sonarScan "$@" ;;
      --sonar-html) sonarHtml ;;
      --sonar-stop) sonarQubeStop ;;
      --spotbugs-scan) spotBugsScan "$@" ;;
      --spotbugs-gui) spotBugsGui "$@" ;;
      --spotbugs-html) spotBugsHtml ;;
      -u) versionUpdate "$@" ;;
      -v) versionCheck "$@" ;;
      -h|--help) usage ;;
      *) remainingOptions+=("${1}") ;;
    esac
    shift
  done
}

dependencyAnalyze() {
  step "Dependency Analyze"
  shift
  local exitCode
  if [[ -z "${GITHUB_STEP_SUMMARY:-}" ]]; then
    run ./mvnw dependency:analyze -DfailOnWarning "$@"
    exitCode=$?
  else
    local logFile="target/dependency-analyze.log"
    set +o errexit
    run ./mvnw dependency:analyze -DfailOnWarning "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]ERROR\[]]"
  fi
  exit "${exitCode}"
}

dependencyTree() {
  step "Dependency Tree"
  shift
  run ./mvnw dependency:tree "$@"
  exit $?
}

jacocoHtml() {
  step "Launch default browser to check Jacoco Coverage Scan analysis result"
  run open "target/site/jacoco/index.html"
  exit $?
}

owaspScan() {
  step "Owasp Scan"
  shift
  local exitCode
  if [[ -z "${GITHUB_STEP_SUMMARY:-}" ]]; then
    run ./mvnw clean verify -Powasp-scan -DskipTests "$@"
    exitCode=$?
  else
    local logFile="target/owasp-scan.log"
    set +o errexit
    run ./mvnw clean verify -Powasp-scan -DskipTests "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]INFO[]] Check for updates complete"
  fi
  exit "${exitCode}"
}

owaspHtml() {
  step "Launch default browser to check OWASP Scan analysis result"
  run open "target/dependency-check-report.html"
  exit $?
}

sonarScan() {
  sonarQubeStart
  step "Sonar Scan"
  shift
  set +o errexit
  run ./mvnw clean verify -Pjacoco-scan -Psonar-scan "$@"
  local exitCode=$?
  set -o errexit
  sonarQubeQualityGateStatus
  exit ${exitCode}
}

sonarHtml() {
  step "Launch default browser to check Sonar Scan analysis result"
  if [[ -f "target/sonar/report-task.txt" ]]; then
    run open "$(grep "dashboardUrl=" "target/sonar/report-task.txt" | sed "s/dashboardUrl=//")"
  else
    echo -e "${WARN} File \"target/sonar/report-task.txt\" does not exist"
  fi
  exit $?
}

spotBugsScan() {
  step "SpotBugs Scan"
  shift
  local exitCode
  if [[ -z "${GITHUB_STEP_SUMMARY:-}" ]]; then
    run ./mvnw clean verify -Pspotbugs-scan -DskipTests "$@"
    exitCode=$?
  else
    local logFile="target/spotbugs-scan.log"
    set +o errexit
    run ./mvnw clean verify -Pspotbugs-scan -DskipTests "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]INFO[]] --- spotbugs:.*:check [(]check[)] @ $(getProjectArtifactId) ---"
  fi
  exit "${exitCode}"
}

spotBugsGui() {
  step "SpotBugs Scan and launch GUI to check analysis result"
  shift
  run ./mvnw clean verify -Pspotbugs-scan -DskipTests -Dspotbugs.failOnError=false spotbugs:gui "$@"
  exit $?
}

spotBugsHtml() {
  step "Launch default browser to check SpotBugs Scan analysis result"
  run open "target/site/spotbugs.html"
  exit $?
}

testsWithJacocoScan() {
  step "Tests with Jacoco Coverage Scan"
  shift
  run ./mvnw clean verify -Pjacoco-scan "$@"
  exit $?
}

versionUpdate() {
  step "Version update"
  shift
  run ./mvnw versions:update-properties \
    -pl . \
    -DgenerateBackupPoms=false \
    -Dmaven.version.ignore="${MAVEN_VERSION_IGNORE}" "$@"
  exit $?
}

versionCheck() {
  step "Version check"
  shift
  run ./mvnw versions:display-property-updates \
    -pl . \
    -Dmaven.version.ignore="${MAVEN_VERSION_IGNORE}" "$@"
  exit $?
}

scan() {
  step "Scan Project"
  run ./mvnw clean verify \
    -Pjacoco-scan \
    -Powasp-scan \
    -Pspotbugs-scan \
    dependency:analyze -DfailOnWarning \
    "${remainingOptions[@]}"
  sonarQubeStart
  step "Sonar Scan"
  set +o errexit
  run ./mvnw verify -Psonar-scan -DskipTests "${remainingOptions[@]}"
  local exitCode=$?
  set -o errexit
  sonarQubeQualityGateStatus
  exit ${exitCode}
}

main "$@"
