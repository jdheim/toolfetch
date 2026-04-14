#!/usr/bin/env bash
# MANAGES SONARQUBE CONTAINER

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

[[ -f "$(dirname "${BASH_SOURCE[0]}")/functions.sh" ]] && . "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

readonly SONAR_CONTAINER_NAME="sonarqube"
readonly SONAR_IMAGE="sonarqube:community"
readonly SONAR_HOST_URL="http://localhost:9000"
readonly SONAR_HOST_PORT="${SONAR_HOST_URL##*:}"
readonly SONAR_ADMIN_USER="admin"
readonly SONAR_ADMIN_OLD_PASS="${SONAR_ADMIN_USER}"

sonarQubeStart() {
  step "SonarQube Start"
  SONAR_ADMIN_NEW_PASS="Admin$(getProjectArtifactId)1!"
  SONAR_TOKEN_NAME="$(getProjectArtifactId)"
  SONAR_PROJECT_NAME="$(getProjectGroupId):$(getProjectArtifactId)"
  SONAR_QUALITY_GATE_NAME="$(getProjectArtifactId)"
  local sonarQubeId
  sonarQubeId="$(sonarQubeId)"
  if [[ -z "${sonarQubeId}" ]]; then
    run docker container run -d --rm --pull "always" --name "${SONAR_CONTAINER_NAME}" -p "${SONAR_HOST_PORT}":"${SONAR_HOST_PORT}" "${SONAR_IMAGE}"
    sonarQubeHealthcheck
    changeAdminPassword
  else
    revokeToken
  fi
  generateToken
  if [[ -z "${sonarQubeId}" ]]; then
    createProject
    createQualityGate
    assignQualityGateToProject
  fi
}

sonarQubeHealthcheck() {
  local timeout=120
  local start duration
  echo -en "${INFO} SonarQube Healthcheck [timeout=${timeout}s] ..."
  start=$(date +%s)
  until curl -fs "${SONAR_HOST_URL}/api/system/status" | grep -q '"status":"UP"'; do
    if (( $(date +%s) - start >= timeout )); then
      echo -e "\n${ERROR} Timeout"
      exit 1
    fi
    echo -n "."
    sleep 1
  done
  duration=$(( $(date +%s) - start ))
  echo -en " UP after ${duration}s\n"
}

changeAdminPassword() {
  echo -e "${INFO} Change Admin Password..."
  curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_OLD_PASS}" -X POST \
    --data-urlencode "login=${SONAR_ADMIN_USER}" \
    --data-urlencode "previousPassword=${SONAR_ADMIN_OLD_PASS}" \
    --data-urlencode "password=${SONAR_ADMIN_NEW_PASS}" \
    "${SONAR_HOST_URL}/api/users/change_password"
}

revokeToken() {
  echo -e "${INFO} Revoke Token..."
  curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_NEW_PASS}" -X POST \
    --data-urlencode "name=${SONAR_TOKEN_NAME}" \
    "${SONAR_HOST_URL}/api/user_tokens/revoke"
}

generateToken() {
  echo -e "${INFO} Generate Token..."
  if ! SONAR_TOKEN="$(curl -fsS -u "${SONAR_ADMIN_USER}:${SONAR_ADMIN_NEW_PASS}" -X POST \
      --data-urlencode "name=${SONAR_TOKEN_NAME}" \
      "${SONAR_HOST_URL}/api/user_tokens/generate" \
      | jq -er '.token | select(type=="string" and length>0)')"; then
    echo -e "${ERROR} Failed to parse SONAR_TOKEN from the response"
    exit 1
  fi
  export SONAR_TOKEN
}

createProject() {
  echo -e "${INFO} Create Project..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "project=${SONAR_PROJECT_NAME}" \
    --data-urlencode "name=ToolFetch" \
    "${SONAR_HOST_URL}/api/projects/create" >/dev/null
}

createQualityGate() {
  echo -e "${INFO} Create QualityGate..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "name=${SONAR_QUALITY_GATE_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/create" >/dev/null

  echo -e "${INFO} Delete QualityGate default conditions..."
  curl -fsS -u "${SONAR_TOKEN}:" -G \
      --data-urlencode "name=${SONAR_QUALITY_GATE_NAME}" \
      "${SONAR_HOST_URL}/api/qualitygates/show" | jq -r '.conditions[].id' \
      | while read -r id; do
          curl -fsS -u "${SONAR_TOKEN}:" -X POST \
            --data-urlencode "id=${id}" \
            "${SONAR_HOST_URL}/api/qualitygates/delete_condition"
        done

  echo -e "${INFO} Create QualityGate conditions..."
  echo "- Test Coverage is not less than 90%"
    curl -fsS -u "${SONAR_TOKEN}:" -X POST \
      --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
      --data-urlencode "metric=coverage" \
      --data-urlencode "op=LT" \
      --data-urlencode "error=90" \
      "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  echo "- Test Condition Coverage is not less than 90%"
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=branch_coverage" \
    --data-urlencode "op=LT" \
    --data-urlencode "error=90" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  echo "- Duplicated Lines Density is not greater than 3%"
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=duplicated_lines_density" \
    --data-urlencode "op=GT" \
    --data-urlencode "error=3" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  echo "- Code Issues is not greater than 0"
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=violations" \
    --data-urlencode "op=GT" \
    --data-urlencode "error=0" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null

  echo "- Security Hotspots Reviewed is not less than 100%"
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    --data-urlencode "metric=security_hotspots_reviewed" \
    --data-urlencode "op=LT" \
    --data-urlencode "error=100" \
    "${SONAR_HOST_URL}/api/qualitygates/create_condition" >/dev/null
}

assignQualityGateToProject() {
  echo -e "${INFO} Assign QualityGate to Project..."
  curl -fsS -u "${SONAR_TOKEN}:" -X POST \
    --data-urlencode "projectKey=${SONAR_PROJECT_NAME}" \
    --data-urlencode "gateName=${SONAR_QUALITY_GATE_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/select"
}

sonarQubeQualityGateStatus() {
  step "SonarQube QualityGate Status"
  local response status
  response="$(curl -fsS -u "${SONAR_TOKEN}:" -G \
    --data-urlencode "projectKey=${SONAR_PROJECT_NAME}" \
    "${SONAR_HOST_URL}/api/qualitygates/project_status")"
  echo "${response}" | jq -r 'def metricLabel: {
    "coverage": "Test Coverage (%)",
    "branch_coverage": "Test Condition Coverage (%)",
    "duplicated_lines_density": "Duplicated Lines Density (%)",
    "violations": "Code Issues",
    "security_hotspots_reviewed": "Security Hotspots Reviewed (%)"
  }[.] // .;
  def colorStatus:
    if . == "OK" then "\u001b[1;32m" + . + "\u001b[0m"
    elif . == "WARN" then "\u001b[1;33m" + . + "\u001b[0m"
    elif . == "ERROR" then "\u001b[1;31m" + . + "\u001b[0m"
    else .
    end;
  .projectStatus.conditions[]
  | "[\(.status | colorStatus)] \(.metricKey | metricLabel): \(.actualValue // "-") \(
      .comparator | if . == "LT" then "is less than"
        elif . == "GT" then "is greater than"
        else .
        end // "-"
      ) \(.errorThreshold // "-")"'
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    {
      echo -n "### Status: "
      status="$(echo "${response}" | jq -r '.projectStatus.status')"
      if [[ "${status}" == "OK" ]]; then
        echo -e "✅ ${status}"
      else
        echo -e "❌ ${status}"
      fi
      echo
      echo "| Metric | Status | Actual | Comparator | Threshold |"
      echo "|---|---:|---:|---:|---|"
      echo "${response}" | jq -r 'def metricLabel:
        {
          "coverage": "Test Coverage (%)",
          "branch_coverage": "Test Condition Coverage (%)",
          "duplicated_lines_density": "Duplicated Lines Density (%)",
          "violations": "Code Issues",
          "security_hotspots_reviewed": "Security Hotspots Reviewed (%)"
        }[.] // .;
        .projectStatus.conditions[] | [
        (.metricKey | metricLabel),
        .status,
        (.actualValue // "-"),
        (.comparator
          | if . == "LT" then "is less than"
            elif . == "GT" then "is greater than"
            else .
            end // "-"),
        (.errorThreshold // "-")
        ] | @tsv' \
      | while IFS=$'\t' read -r metricKey status actualValue comparator errorThreshold; do
        echo "| \`${metricKey}\` | **${status}** | \`${actualValue}\` | \`${comparator}\` | \`${errorThreshold}\` |"
      done
      echo
      echo "<details><summary>Raw JSON Response</summary>"
      echo
      echo '```json'
      echo "${response}" | jq -r
      echo '```'
      echo
      echo "</details>"
    } >> "${GITHUB_STEP_SUMMARY}"
  fi
}

sonarQubeStop() {
  step "SonarQube Stop"
  local sonarQubeId
  sonarQubeId="$(sonarQubeId)"
  if [[ -n "${sonarQubeId}" ]]; then
    run docker container stop "${sonarQubeId}"
  else
    echo -e "${WARN} Nothing to stop"
  fi
  exit $?
}

sonarQubeId() {
  docker container ls --filter "name=^${SONAR_CONTAINER_NAME}$" --filter "status=running" --format "{{.ID}}"
}
