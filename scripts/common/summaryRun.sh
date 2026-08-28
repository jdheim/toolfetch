#!/usr/bin/env bash
# WRAPS COMMAND EXECUTION WITH OPTIONAL GITHUB ACTIONS SUMMARY REPORTING

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

if [[ -n "${SUMMARY_RUN_LOADED:-}" ]]; then
  return 0
fi
declare -r SUMMARY_RUN_LOADED="true"
source "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

## summaryRun --start "^[[]INFO[]] Check for updates complete" --end "^[[]ERROR[]] Re-run Maven" "${command[@]}"
summaryRun() {
  local startPattern endPattern logFile exitCode
  local quiet=false
  while (( $# > 0 )); do
    case "${1}" in
      --start) startPattern="${2}"; shift ;;
      --end) endPattern="${2}"; shift ;;
      -q|--quiet) quiet=true ;;
      *) break ;;
    esac
    shift
  done
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    mkdir -p "${PROJECT_DIR}/target"
    logFile="$(mktemp "${PROJECT_DIR}/target/github-step-summary.XXXXXX.log")"
    set +o errexit
    if [[ "${quiet}" == "true" ]]; then
      "$@" 2>&1 | tee "${logFile}"
    else
      run "$@" 2>&1 | tee "${logFile}"
    fi
    exitCode=${PIPESTATUS[0]}
    set -o errexit
    if [[ -z "${GITHUB_STEP_SUMMARY:-}" ]]; then
      error "The GITHUB_STEP_SUMMARY environment is not set"
      return 1
    fi
    {
      printf '### Status: '
      if (( exitCode == 0 )); then
        printf '✅ OK\n'
      else
        printf '❌ ERROR\n'
        printf '\n'
        printf '<details><summary>Logs</summary>\n'
        printf '\n'
        printf '```text\n'
        if [[ -z "${startPattern:-}" ]]; then
          cat "${logFile}"
        else
          awk -v start="${startPattern:-}" -v end="${endPattern:-}" \
            '$0 ~ start {p=1} p {print} end != "" && $0 ~ end {exit}' \
            "${logFile}"
        fi
        printf '```\n'
        printf '\n'
        printf '</details>\n'
      fi
    } >> "${GITHUB_STEP_SUMMARY}"
  else
    if [[ "${quiet}" == "true" ]]; then
      "$@"
    else
      run "$@"
    fi
    exitCode=$?
  fi
  return "${exitCode}"
}

summaryRunAndExit() {
  summaryRun "$@"
  exit $?
}

githubActionsMode() {
  export GITHUB_ACTIONS="true";
  local githubStepSummaryPath
  githubStepSummaryPath="${PROJECT_DIR}/GITHUB_STEP_SUMMARY.txt"
  export GITHUB_STEP_SUMMARY="${githubStepSummaryPath}";
  : > "${githubStepSummaryPath}"
}
