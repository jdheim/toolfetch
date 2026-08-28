#!/usr/bin/env bash
# VALIDATE ALL SCRIPTS WITH SHELLCHECK

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

source "$(dirname "${BASH_SOURCE[0]}")/common/summaryRun.sh"

usage() {
  cat << EOF
Usage: $(basename "$0") [OPTION]...

Validate all Scripts with ShellCheck

OPTIONS:
  --gh                   Run in GitHub Actions mode
EOF
  return 1
}

main() {
  requireCommand shellcheck
  readOptions "$@"
  shellCheck
}

readOptions() {
  while (( $# > 0 )); do
    case "$1" in
      --gh) githubActionsMode ;;
      *) usage ;;
    esac
    shift
  done
}

shellCheck() {
  step "Validate Scripts"
  requireDir "${SCRIPTS_DIR}"
  local foundScripts
  local -a scripts=()
  foundScripts="$(find "${SCRIPTS_DIR}" -name "*.sh" -type f -print | sort)"
  if [[ -n "${foundScripts}" ]]; then
    mapfile -t scripts <<< "${foundScripts}"
  fi
  if (( ${#scripts[@]} == 0 )); then
    error "Nothing to validate"
    return 1
  fi
  printf -- '- %s\n' "${scripts[@]}"
  set +o errexit
  summaryRun -q shellcheckWithCustomRules "${scripts[@]}"
  shellcheckExitCode=$?
  set -o errexit
  if (( shellcheckExitCode == 0 )); then
    info "ShellCheck passed"
  else
    printf '\n'
    error "ShellCheck failed"
  fi
  return "${shellcheckExitCode}"
}

shellcheckWithCustomRules() {
  shellcheck \
        --external-sources \
        --source-path="${SCRIPTS_DIR}" \
        --source-path="${COMMON_SCRIPTS_DIR}" \
        --severity=style \
        --enable=all \
        --norc "$@"
  local shellcheckExitCode=$?
  customShellcheckRulePrintf "$@"
  local customShellcheckExitCode=$?
  if (( shellcheckExitCode == 0 && customShellcheckExitCode != 0 )); then
    shellcheckExitCode="${customShellcheckExitCode}"
  fi
  return "${shellcheckExitCode}"
}

customShellcheckRulePrintf() {
  local violations file lineNumber lineText column
  violations="$(grep --line-number --extended-regexp '^[[:space:]]*echo[[:space:]]|^[^#]*[[:space:];|&(]echo[[:space:]]' "${@}" || true)"
  if [[ -z "${violations}" ]]; then
    return 0
  fi
  while IFS=: read -r file lineNumber lineText; do
    column="${lineText%%echo*}"
    column="${#column}"
    printf '\n'
    printf '%bIn %s line %s:%b\n' "${BOLD}" "${file}" "${lineNumber}" "${RESET}"
    printf '%s\n' "${lineText}"
    printf '%b%*s^--^ SC0001 (custom): Use "printf" instead%b\n' "${YELLOW_DARK}" "${column}" "" "${RESET}"
  done <<< "${violations}"
  return 1
}

main "$@"
