#!/usr/bin/env bash
# SCANS PROJECT

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
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
  --st-native            Runs Smoke Tests for a standalone executable (native image)
  --native-agent-scan    Native Agent Scan (generates reachability-metadata.json)
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
      -t|--jacoco-scan) testsWithJacocoScan "$@" ;;
      --st-native) smokeTestsNative "$@" ;;
      --native-agent-scan) nativeAgentScan "$@" ;;
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
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    local logFile="target/dependency-analyze.log"
    set +o errexit
    run ./mvnw -ntp dependency:analyze -DfailOnWarning "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]ERROR\[]]"
  else
    run ./mvnw -ntp dependency:analyze -DfailOnWarning "$@"
    exitCode=$?
  fi
  exit "${exitCode}"
}

dependencyTree() {
  step "Dependency Tree"
  shift
  run ./mvnw -ntp dependency:tree "$@"
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
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    local logFile="target/owasp-scan.log"
    set +o errexit
    run ./mvnw -ntp clean verify -Powasp-scan -DskipTests "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]INFO[]] Check for updates complete"
  else
    run ./mvnw -ntp clean verify -Powasp-scan -DskipTests "$@"
    exitCode=$?
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
  run ./mvnw -ntp clean verify -Pjacoco-scan -Psonar-scan -Djacoco.check.skip=true "$@"
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
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    local logFile="target/spotbugs-scan.log"
    set +o errexit
    run ./mvnw -ntp clean verify -Pspotbugs-scan -DskipTests "$@" | tee "${logFile}"
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    githubStepSummary "${exitCode}" "${logFile}" "^[[]INFO[]] --- spotbugs:.*:check [(]check[)] @ $(getProjectArtifactId) ---"
  else
    run ./mvnw -ntp clean verify -Pspotbugs-scan -DskipTests "$@"
    exitCode=$?
  fi
  exit "${exitCode}"
}

spotBugsGui() {
  step "SpotBugs Scan and launch GUI to check analysis result"
  shift
  run ./mvnw -ntp clean verify -Pspotbugs-scan -DskipTests -Dspotbugs.failOnError=false spotbugs:gui "$@"
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
  run ./mvnw -ntp clean verify -Pjacoco-scan "$@"
  exit $?
}

smokeTestsNative() {
  step "Smoke Tests for a standalone executable (native image)"
  shift
  local binary="target/toolfetch"
  if [[ ! -x "${binary}" ]]; then
    echo -e "${ERROR} Executable \"${binary}\" does not exist. Run: ./build.sh -n "
    exit 1
  fi
  run ./mvnw -ntp verify -Psmoke-tests-native "$@"
  exit $?
}

nativeAgentScan() {
  step "Native Agent Scan"
  shift
  run ./mvnw -ntp clean verify -Psetup-graalvm -Pnative-agent-scan "$@"
  exit $?
}

versionUpdate() {
  step "Version update"
  shift
  run ./mvnw -ntp versions:update-properties \
    -pl . \
    -DgenerateBackupPoms=false \
    -Dmaven.version.ignore="${MAVEN_VERSION_IGNORE}" "$@"
  exit $?
}

versionCheck() {
  step "Version check"
  shift
  run ./mvnw -ntp versions:display-property-updates \
    -pl . \
    -Dmaven.version.ignore="${MAVEN_VERSION_IGNORE}" "$@"
  exit $?
}

scan() {
  step "Scan Project"
  run ./mvnw -ntp clean verify \
    -Pjacoco-scan \
    -Powasp-scan \
    -Pspotbugs-scan \
    dependency:analyze -DfailOnWarning \
    "${remainingOptions[@]}"
  sonarQubeStart
  step "Sonar Scan"
  set +o errexit
  run ./mvnw -ntp verify -Psonar-scan -DskipTests "${remainingOptions[@]}"
  local exitCode=$?
  set -o errexit
  sonarQubeQualityGateStatus
  exit ${exitCode}
}

main "$@"
