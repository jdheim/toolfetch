#!/usr/bin/env bash
# RUN ALL PROJECT VERIFICATIONS

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

SONARQUBE_SUMMARY_FILE="$(mktemp)" || exit 1
export SONARQUBE_SUMMARY_FILE

printSummary() {
  local exitCode=$?
  if [[ -s "${SONARQUBE_SUMMARY_FILE}" ]]; then
    printf '\nSonarQube QualityGate Summary\n'
    cat "${SONARQUBE_SUMMARY_FILE}"
  fi
  rm -f "${SONARQUBE_SUMMARY_FILE}"
  exit "${exitCode}"
}

trap printSummary EXIT

./build.sh && ./verify.sh && ./verify.sh --native-agent-scan && ./build.sh --release && ./verify.sh --st-native
