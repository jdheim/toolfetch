#!/usr/bin/env bash
# VALIDATE ALL SCRIPTS WITH SHELLCHECK

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

source "$(dirname "${BASH_SOURCE[0]}")/common/functions.sh"

usage() {
  cat << EOF
Usage: $(basename "$0")

Validate all Scripts with ShellCheck
EOF
  return 1
}

main() {
  [[ ${PWD} == */scripts ]] && cd ..
  readOptions "$@"
  validateShellCheck
  shellCheck
}

readOptions() {
  while (( $# > 0 )); do
    case "$1" in
      *) usage ;;
    esac
    shift
  done
}

validateShellCheck() {
  step "Validate ShellCheck"
  if ! command -v shellcheck &>/dev/null; then
    echo -e "${ERROR} ShellCheck is not installed" >&2
    return 1
  fi
}

shellCheck() {
  step "Validate Scripts"
  cd scripts || return 1
  local scriptFilesOutput
  scriptFilesOutput="$(find . -type f -name "*.sh" -print)"
  local -a scriptFiles=()
  if [[ -n "${scriptFilesOutput}" ]]; then
    mapfile -t scriptFiles <<< "${scriptFilesOutput}"
  fi
  if (( ${#scriptFiles[@]} == 0 )); then
    echo -e "${ERROR} Nothing to validate"
    return 1
  fi
  echo "${scriptFilesOutput}"
  set +o errexit
  summaryRun -q shellcheck --external-sources --source-path=SCRIPTDIR --severity=style --enable=all "${scriptFiles[@]}"
  local exitCode=$?
  set -o errexit
  if (( exitCode == 0 )); then
    echo -e "${INFO} ShellCheck passed"
  else
    echo -e "\n${ERROR} ShellCheck failed"
  fi
  return "${exitCode}"
}

main "$@"
