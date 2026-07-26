#!/usr/bin/env bash
# RUN ALL PROJECT VERIFICATIONS

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

./build.sh && ./verify.sh && ./verify.sh --native-agent-scan && ./build.sh -n && ./verify.sh --st-native
