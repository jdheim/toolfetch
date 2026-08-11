#!/usr/bin/env bash
# VERIFY PROJECT

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

source "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh"
source "$(dirname "${BASH_SOURCE[0]}")/common/sonarqube.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Verify Project

OPTIONS:
  --da, --dependency-analyze    Dependency Analyze
  --dt, --dependency-tree       Dependency Tree
  -t, --jacoco-scan             Runs Tests with Jacoco Coverage Scan
  --jacoco-html                 Launch default browser to check Jacoco Coverage Scan analysis result
  --jacoco-unit-html            Launch default browser to check Jacoco Coverage Scan analysis result for Unit Tests only
  --jacoco-it-html              Launch default browser to check Jacoco Coverage Scan analysis result for Integration Tests only
  --jacoco-st-html              Launch default browser to check Jacoco Coverage Scan analysis result for Smoke Tests only
  --st-native                   Runs Smoke Tests for a standalone executable (native image)
  --native-agent-scan           Native Agent Scan (generates reachability-metadata.json)
  --native-agent-maven-scan     Native Agent Scan (generates reachability-metadata.json) using native-maven-plugin
  --owasp-scan                  OWASP Scan
  --owasp-html                  Launch default browser to check OWASP Scan analysis result
  --sonarqube-scan              Runs Tests with SonarQube Scan
  --sonarqube-html              Launch default browser to check SonarQube Scan analysis result
  --sonarqube-stop              Stop SonarQube server
  --spotbugs-scan               SpotBugs Scan
  --spotbugs-html               Launch default browser to check SpotBugs Scan analysis result
  -u                            Version update
  -v                            Version check
EOF
  return 1
}

main() {
  [[ ${PWD} == */scripts ]] && cd ..
  readOptions "$@"
  verify
}

readOptions() {
  while (( $# > 0 )); do
    case "${1}" in
      --da|--dependency-analyze) dependencyAnalyze "$@" ;;
      --dt|--dependency-tree) dependencyTree "$@" ;;
      -t|--jacoco-scan) testsWithJacocoScan "$@" ;;
      --jacoco-html) jacocoHtml "all" ;;
      --jacoco-unit-html) jacocoHtml "unit" ;;
      --jacoco-it-html) jacocoHtml "it" ;;
      --jacoco-st-html) jacocoHtml "st" ;;
      --st-native) smokeTestsNative "$@" ;;
      --native-agent-scan) nativeAgentScan "$@" ;;
      --native-agent-maven-scan) nativeAgentMavenScan "$@" ;;
      --owasp-scan) owaspScan "$@" ;;
      --owasp-html) owaspHtml ;;
      --sonarqube-scan) sonarqubeScan "$@" ;;
      --sonarqube-html) sonarqubeHtml ;;
      --sonarqube-stop) sonarqubeStop ;;
      --spotbugs-scan) spotBugsScan "$@" ;;
      --spotbugs-html) spotBugsHtml "$@" ;;
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
  summaryRun --start "^[[]ERROR\[]]" --end "^[[]ERROR[]] Re-run Maven" ./mvnw -ntp dependency:analyze -DfailOnWarning "$@"
  exit "$?"
}

dependencyTree() {
  step "Dependency Tree"
  shift
  run ./mvnw -ntp dependency:tree "$@"
  exit $?
}

jacocoHtml() {
  step "Launch default browser to check Jacoco Coverage Scan analysis result"
  local reportType="${1}"
  run open "test/toolfetch-test-coverage/target/site/jacoco-aggregate-${reportType}/index.html"
  exit $?
}

owaspScan() {
  step "Owasp Scan"
  shift
  summaryRun --start "^[[]INFO[]] Check for updates complete" --end "^[[]ERROR[]] Re-run Maven" ./mvnw -ntp verify -Powasp-scan -DskipTests "$@"
  exit "$?"
}

owaspHtml() {
  step "Launch default browser to check OWASP Scan analysis result"
  run open "target/dependency-check-report.html"
  exit $?
}

sonarqubeScan() {
  sonarqubeStart
  step "SonarQube Scan"
  shift
  set +o errexit
  run ./mvnw -ntp verify -pl :toolfetch-test-coverage -am -Pjacoco-scan,sonarqube-scan "$@"
  local exitCode=$?
  set -o errexit
  (( exitCode >= 128 )) && exit "${exitCode}"
  sonarqubeQualityGateStatus
  exit "${exitCode}"
}

sonarqubeHtml() {
  step "Launch default browser to check SonarQube Scan analysis result"
  if [[ -f "target/sonar/report-task.txt" ]]; then
    local dashboardUrl
    dashboardUrl="$(sed -n 's/^dashboardUrl=//p' "target/sonar/report-task.txt")"
    run open "${dashboardUrl}"
  else
    echo -e "${WARN} File \"target/sonar/report-task.txt\" does not exist"
  fi
  exit $?
}

spotBugsScan() {
  step "SpotBugs Scan"
  shift
  local projectArtifactId
  projectArtifactId="$(projectArtifactId)"
  summaryRun --start "^[[]INFO[]] --- spotbugs:.*:check [(]check[)] @ ${projectArtifactId} ---" --end "^[[]ERROR[]] Re-run Maven" ./mvnw -ntp verify -Pspotbugs-scan -DskipTests "$@"
  exit "$?"
}

spotBugsHtml() {
  step "Launch default browser to check SpotBugs Scan analysis result"
  shift
  local modulePath
  modulePath="$(modulePath "${1:-}")"
  run open "${modulePath}/target/site/spotbugs.html"
  exit $?
}

testsWithJacocoScan() {
  step "Tests with Jacoco Coverage Scan"
  shift
  run ./mvnw -ntp verify -pl :toolfetch-test-coverage -am -Pjacoco-scan "$@"
  exit $?
}

smokeTestsNative() {
  step "Smoke Tests for a standalone executable (native image)"
  shift
  local binary="target/toolfetch"
  if [[ ! -x "${binary}" ]]; then
    echo -e "${ERROR} Executable \"${binary}\" does not exist. Run: ./build.sh -n "
    return 1
  fi
  run ./mvnw -ntp verify -pl :toolfetch-smoke-tests -am -Psmoke-tests-native "$@"
  exit $?
}

nativeAgentScan() {
  step "Native Agent Scan"
  shift
  run ./mvnw -ntp verify -pl :toolfetch-test-coverage,:toolfetch-native-agent -am -Psetup-graalvm,native-agent-scan "$@"
  processReachabilityMetadataJson
  exit $?
}

nativeAgentMavenScan() {
  step "Native Agent Maven Scan"
  shift
  local graalVmJdkVersion
  graalVmJdkVersion="$(xmlProperty "graalvm-jdk.version" "pom.xml")"
  export GRAALVM_HOME="target/jdks/graalvm-linux-amd64/graalvm-jdk-${graalVmJdkVersion}"
  run ./mvnw -ntp verify -pl :toolfetch-test-coverage -am -Psetup-graalvm,native-agent-scan-with-maven "$@"
  processReachabilityMetadataJson
  exit $?
}

processReachabilityMetadataJson() {
  local reachabilityMetadataJsonPath tmp
  reachabilityMetadataJsonPath="src/toolfetch-commands/src/main/resources/META-INF/native-image/com.jdheim/toolfetch-commands/reachability-metadata.json"
  tmp="$(mktemp)"
  jq '
    def excluded_packages:
      [
        "com/tngtech",
        "org/assertj",
        "org/junit",
        "org/mockito",
        "org/wiremock",
        "com/github/tomakehurst"
      ];
    if (.resources | type) == "array" then
      .resources |= map(
        (.glob // "") as $glob
        | ($glob | gsub("\\."; "/")) as $normalized
        | select(
            (
              ($glob | endswith(".class"))
              or
              (
                [
                  excluded_packages[] as $package
                  | $normalized
                  | contains($package)
                ]
                | any
              )
            )
            | not
          )
        | if .glob == "logback-test.xml" then
            .glob = "logback.xml"
          else
            .
          end
      )
    else
      .
    end
  ' "${reachabilityMetadataJsonPath}" > "${tmp}" && mv "${tmp}" "${reachabilityMetadataJsonPath}"
}

versionUpdate() {
  step "Version Update"
  shift
  local propertyUpdatesFiles versionPropertyUpdates isUpdated propertyUpdatesFile moduleDir property oldVersion newVersion message
  propertyUpdatesFiles="$(find . -type f -name "${PROPERTY_UPDATES_FILE}" -print)"
  if [[ -z "${propertyUpdatesFiles}" ]]; then
    echo -e "${ERROR} The \"${PROPERTY_UPDATES_FILE}\" file does not exist. Run: ./verify.sh -v"
    return 1
  fi
  while IFS= read -r propertyUpdatesFile; do
    versionPropertyUpdates="$(awk '/version property updates are available/ { found=1 }
                                      found && /->/ {
                                        gsub(/^\$\{|\}$/, "", $1)
                                        print $1, $(NF-2), $NF
                                      }
                                      ' "${propertyUpdatesFile}")"
    moduleDir="${propertyUpdatesFile%/"${PROPERTY_UPDATES_PATH}"}"
    if [[ -n "${versionPropertyUpdates}" ]]; then
      while read -r property oldVersion newVersion; do
        isUpdated="true"
        message="Update ${property%.version} from ${oldVersion} to ${newVersion}"
        step "${message}"

        if ./mvnw versions:set-property \
          -pl "${moduleDir}" \
          -Dproperty="${property}" \
          -DnewVersion="${newVersion}" \
          -DgenerateBackupPoms=false
        then
          sed -i "\|^[[:space:]]*\${${property}}[[:space:]].*->[[:space:]]|d" "${propertyUpdatesFile}"
          echo -e "${INFO} Committing changes..."
          git add "${moduleDir}/pom.xml"
          git commit -m "deps: ${message}"
        else
          return 1
        fi
      done <<< "${versionPropertyUpdates}"
    fi
    rm "${propertyUpdatesFile}"
  done <<< "${propertyUpdatesFiles}"
  if ! "${isUpdated:-false}"; then
    echo -e "${WARN} Nothing to update"
  fi
  exit $?
}

versionCheck() {
  step "Version Check"
  shift
  run ./mvnw -ntp versions:display-property-updates \
    -pl .,:toolfetch-bom \
    -Dversions.outputFile="${PROPERTY_UPDATES_PATH}" \
    -Dversions.overwriteOutput=true \
    -Dmaven.version.ignore="${MAVEN_VERSION_IGNORE}" "$@"
  exit $?
}

verify() {
  step "Verify Project"
  run ./mvnw -ntp verify dependency:analyze \
    -Pjacoco-scan,owasp-scan,spotbugs-scan \
    -DfailOnWarning \
    "${remainingOptions[@]}"
  sonarqubeStart
  step "SonarQube Scan"
  set +o errexit
  run ./mvnw -ntp verify -pl :toolfetch-test-coverage -am -Psonarqube-scan -DskipTests "${remainingOptions[@]}"
  local exitCode=$?
  set -o errexit
  (( exitCode >= 128 )) && exit "${exitCode}"
  sonarqubeQualityGateStatus
  exit "${exitCode}"
}

main "$@"
