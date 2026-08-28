#!/usr/bin/env bash
# MANAGES SONARQUBE CONTAINER

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

if [[ -n "${SONARQUBE_LOADED:-}" ]]; then
  return 0
fi
declare -r SONARQUBE_LOADED="true"
source "$(dirname "${BASH_SOURCE[0]}")/projectFunctions.sh"
source "$(dirname "${BASH_SOURCE[0]}")/createBadge.sh"

readonly SONAR_CONTAINER_NAME="sonarqube"
readonly SONAR_IMAGE="sonarqube:community"
readonly SONAR_HOST_URL="http://localhost:9000"
readonly SONAR_HOST_PORT="${SONAR_HOST_URL##*:}"
readonly SONAR_ADMIN_USER="admin"
readonly SONAR_ADMIN_OLD_PASS="${SONAR_ADMIN_USER}"

sonarqubeStart() {
  step "SonarQube Start"
  local projectGroupId projectArtifactId
  projectGroupId="$(projectGroupId)"
  projectArtifactId="$(projectArtifactId)"
  SONAR_ADMIN_NEW_PASS="Admin${projectArtifactId}1!"
  SONAR_TOKEN_NAME="${projectArtifactId}"
  SONAR_PROJECT_NAME="${projectGroupId}:${projectArtifactId}"
  SONAR_QUALITY_GATE_NAME="${projectArtifactId}"
  local sonarqubeId
  sonarqubeId="$(sonarqubeId)"
  if [[ -z "${sonarqubeId}" ]]; then
    run docker container run -d --rm --pull "always" --name "${SONAR_CONTAINER_NAME}" -p "${SONAR_HOST_PORT}":"${SONAR_HOST_PORT}" "${SONAR_IMAGE}"
    sonarqubeHealthcheck
    changeAdminPassword
  else
    revokeToken
  fi
  generateToken
  if [[ -z "${sonarqubeId}" ]]; then
    createProject
    createQualityGate
    assignQualityGateToProject
  fi
}

sonarqubeHealthcheck() {
  local timeout=120
  local start duration
  printf '[%bINFO%b] SonarQube Healthcheck [timeout=%ds] ...' "${BLUE}" "${RESET}" "${timeout}"
  start=$(date +%s)
  until curl -fs "${SONAR_HOST_URL}/api/system/status" | grep -q '"status":"UP"'; do
    local now
    now="$(date +%s)"
    if (( now - start >= timeout )); then
      printf '\n'
      error "Timeout"
      return 1
    fi
    printf '.'
    sleep 1
  done
  duration=$(( $(date +%s) - start ))
  printf ' UP after %ds\n' "${duration}"
}

changeAdminPassword() {
  info "Change Admin Password..."
  curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_OLD_PASS}" -X POST \
    --data-urlencode "login=${SONAR_ADMIN_USER}" \
    --data-urlencode "previousPassword=${SONAR_ADMIN_OLD_PASS}" \
    --data-urlencode "password=${SONAR_ADMIN_NEW_PASS}" \
    "${SONAR_HOST_URL}/api/users/change_password"
}

revokeToken() {
  info "Revoke Token..."
  curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_NEW_PASS}" -X POST \
    --data-urlencode "name=${SONAR_TOKEN_NAME}" \
    "${SONAR_HOST_URL}/api/user_tokens/revoke"
}

generateToken() {
  info "Generate Token..."
  if ! SONAR_TOKEN="$(curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_NEW_PASS}" -X POST \
      --data-urlencode "name=${SONAR_TOKEN_NAME}" \
      "${SONAR_HOST_URL}/api/user_tokens/generate" \
      | jq -er '.token | select(type=="string" and length>0)')"; then
    error "Failed to parse SONAR_TOKEN from the response"
    return 1
  fi
  export SONAR_TOKEN
}

createProject() {
  info "Create Project..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "project=${SONAR_PROJECT_NAME}" \
    --data-urlencode "name=ToolFetch" \
    "${SONAR_HOST_URL}/api/projects/create" >/dev/null
}

createQualityGate() {
  info "Create QualityGate..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "name=${SONAR_QUALITY_GATE_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/create" >/dev/null

  info "Delete QualityGate default conditions..."
  curl -fsS -u "${SONAR_TOKEN}:" -G \
      --data-urlencode "name=${SONAR_QUALITY_GATE_NAME}" \
      "${SONAR_HOST_URL}/api/qualitygates/show" | jq -r '.conditions[].id' \
      | while read -r id; do
          curl -fsS -u "${SONAR_TOKEN}:" -X POST \
            --data-urlencode "id=${id}" \
            "${SONAR_HOST_URL}/api/qualitygates/delete_condition"
        done

  info "Create QualityGate conditions..."
  printf -- '- Test Coverage is not less than 90%%\n'
    curl -fsS -u "${SONAR_TOKEN}:" -X POST \
      --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
      --data-urlencode "metric=coverage" \
      --data-urlencode "op=LT" \
      --data-urlencode "error=90" \
      "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  printf -- '- Test Condition Coverage is not less than 90%%\n'
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=branch_coverage" \
    --data-urlencode "op=LT" \
    --data-urlencode "error=90" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  printf -- '- Duplicated Lines Density is not greater than 3%%\n'
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=duplicated_lines_density" \
    --data-urlencode "op=GT" \
    --data-urlencode "error=3" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  printf -- '- Code Issues is not greater than 0\n'
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=violations" \
    --data-urlencode "op=GT" \
    --data-urlencode "error=0" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  printf -- '- Security Hotspots Reviewed is not less than 100%%\n'
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=security_hotspots_reviewed" \
    --data-urlencode "op=LT" \
    --data-urlencode "error=100" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null
}

assignQualityGateToProject() {
  info "Assign QualityGate to Project..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "projectKey=${SONAR_PROJECT_NAME}" \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/select"
}

sonarqubeQualityGateStatus() {
  local response status
  response="$(curl -fsS -u "${SONAR_TOKEN}:" -G \
    --data-urlencode "projectKey=${SONAR_PROJECT_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/project_status")"

  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    actualTestCoverage=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="coverage") | .actualValue) // "0.0"')
    actualTestConditionCoverage=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="branch_coverage") | .actualValue) // "0.0"')
    actualDuplicatedLinesDensityStatus=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="duplicated_lines_density") | .status) // "OK"')
    actualDuplicatedLinesDensity=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="duplicated_lines_density") | .actualValue) // "0.0"')
    actualSonarCodeIssuesStatus=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="violations") | .status) // "OK"')
    actualSonarCodeIssues=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="violations") | .actualValue) // "0"')
    actualSonarSecurityHotspotsStatus=$(jq -r -n --argjson data "${response}" 'first($data.projectStatus.conditions[]? | select(.metricKey=="security_hotspots_reviewed") | .status) // "OK"')
    createPercentageBadge "test-coverage" "sonarqubeserver" "Test Coverage (%)" "${actualTestCoverage}"
    createPercentageBadge "test-condition-coverage" "sonarqubeserver" "Test Condition Coverage (%)" "${actualTestConditionCoverage}"
    createStatusBadge "duplicated-lines" "sonarqubeserver" "Duplicated Lines (%)" "${actualDuplicatedLinesDensityStatus}" "${actualDuplicatedLinesDensity}"
    createStatusBadge "sonarqube-code-issues" "sonarqubeserver" "SonarQube Code Issues" "${actualSonarCodeIssuesStatus}" "${actualSonarCodeIssues}"
    createStatusBadge "sonarqube-security-hotspots" "sonarqubeserver" "SonarQube Security Hotspots" "${actualSonarSecurityHotspotsStatus}"
  fi

  step "SonarQube QualityGate Status"
  jq -r -n --argjson data "${response}" '
    def orderedMetrics: [
      { key: "coverage", label: "Test Coverage (%)" },
      { key: "branch_coverage", label: "Test Condition Coverage (%)" },
      { key: "duplicated_lines_density", label: "Duplicated Lines (%)" },
      { key: "violations", label: "Code Issues" },
      { key: "security_hotspots_reviewed", label: "Security Hotspots Reviewed (%)" }
    ];

    def colorStatus:
      if . == "OK" then "\u001b[1;32m" + . + "\u001b[0m"
      elif . == "WARN" then "\u001b[1;33m" + . + "\u001b[0m"
      elif . == "ERROR" then "\u001b[1;31m" + . + "\u001b[0m"
      else .
      end;

    orderedMetrics[] | .key as $metricKey | .label as $metricLabel
      | first($data.projectStatus.conditions[]? | select(.metricKey == $metricKey)) as $condition
      | select($condition != null)
      | "[\($condition.status | colorStatus)] \(
            $metricLabel): \(
            $condition.actualValue // "-") \(
            $condition.comparator | if . == "LT" then "is less than"
              elif . == "GT" then "is greater than"
              else .
              end // "-") \(
            $condition.errorThreshold // "-")"
  '

  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    if [[ -z "${GITHUB_STEP_SUMMARY:-}" ]]; then
      error "The GITHUB_STEP_SUMMARY environment is not set"
      return 1
    fi
    {
      printf '### Status: '
      status="$(printf '%s\n' "${response}" | jq -r '.projectStatus.status')"
      if [[ "${status}" == "OK" ]]; then
        printf '✅ %s\n' "${status}"
      else
        printf '❌ %s\n' "${status}"
      fi
      printf '\n'
      printf "| Metric | Status | Actual | Comparator | Threshold |\n"
      printf "|---|---:|---:|---:|---|\n"
      jq -r -n --argjson data "${response}" '
        def orderedMetrics: [
          { key: "coverage", label: "Test Coverage (%)" },
          { key: "branch_coverage", label: "Test Condition Coverage (%)" },
          { key: "duplicated_lines_density", label: "Duplicated Lines (%)" },
          { key: "violations", label: "Code Issues" },
          { key: "security_hotspots_reviewed", label: "Security Hotspots Reviewed (%)" }
        ];

        orderedMetrics[] | .key as $metricKey | .label as $metricLabel
          | first($data.projectStatus.conditions[]? | select(.metricKey == $metricKey)) as $condition
          | select($condition != null)
          | [
            $metricLabel,
            ($condition.status // "-"),
            ($condition.actualValue // "-"),
            ($condition.comparator | if . == "LT" then "is less than"
                elif . == "GT" then "is greater than"
                else .
                end // "-"),
            ($condition.errorThreshold // "-")
          ] | @tsv
      ' | while IFS=$'\t' read -r metricKey status actualValue comparator errorThreshold; do
            printf "| \`%s\` | **%s** | \`%s\` | \`%s\` | \`%s\` |\n" \
              "${metricKey}" "${status}" "${actualValue}" "${comparator}" "${errorThreshold}"
          done
      printf '\n'
      printf '<details><summary>Raw JSON Response</summary>\n'
      printf '\n'
      printf '```json\n'
      printf '%s\n' "${response}" | jq -r
      printf '```\n'
      printf '\n'
      printf '</details>\n'
    } >> "${GITHUB_STEP_SUMMARY}"
  fi
}

sonarqubeStop() {
  step "SonarQube Stop"
  local sonarqubeId
  sonarqubeId="$(sonarqubeId)"
  if [[ -n "${sonarqubeId}" ]]; then
    run docker container stop "${sonarqubeId}"
  else
    warn "Nothing to stop"
  fi
  exit $?
}

sonarqubeId() {
  docker container ls --filter "name=^${SONAR_CONTAINER_NAME}$" --filter "status=running" --format "{{.ID}}"
}
