#!/usr/bin/env bash
# UPDATES NOTICE

#
# Copyright 2026 JDHeim.com
# SPDX-License-Identifier: Apache-2.0
#

if [[ -n "${UPDATE_NOTICE_LOADED:-}" ]]; then
  return 0
fi
declare -r UPDATE_NOTICE_LOADED="true"
source "$(dirname "${BASH_SOURCE[0]}")/functions.sh"

readonly NOTICE="${PROJECT_DIR}/NOTICE"
readonly THIRD_PARTY="${PROJECT_DIR}/src/toolfetch-commands/target/third-party/THIRD-PARTY.txt"
readonly LIBS="${PROJECT_DIR}/src/toolfetch-commands/target/third-party/LIBS.txt"
readonly LIBS_DIR="${PROJECT_DIR}/build/toolfetch-native-image/target/third-party/lib"
readonly LIBS_LICENSE_DIR="${PROJECT_DIR}/build/toolfetch-native-image/target/third-party/license"
readonly LIBS_NOTICE_DIR="${PROJECT_DIR}/build/toolfetch-native-image/target/third-party/notice"
readonly LIBS_SOURCES_DIR="${PROJECT_DIR}/src/toolfetch-commands/target/third-party/sources"
readonly LICENSE_CACHE_DIR="${HOME}/.m2/repository/.cache/bash/licenses"

declare -Ar HARDCODED_URLS=(
  ["ch.qos.logback:logback-classic"]="https://logback.qos.ch"
  ["ch.qos.logback:logback-core"]="https://logback.qos.ch"
  ["org.brotli:dec"]="https://brotli.org"
)
declare -Ar SPDX_LICENSES=(
  ["0BSD"]="0BSD"
  [".*Apache.*2\.0.*"]="Apache-2.0"
  ["BSD.*2-Clause.*"]="BSD-2-Clause"
  ["(EPL-2.0|Eclipse.*Public.*License.*2\.0)"]="EPL-2.0"
  ["LGPL-2.1-only"]="LGPL-2.1-only"
  ["MIT.*"]="MIT"
)
declare -Ar SPDX_LICENSE_PRECEDENCE=(
  ["EPL-2.0|LGPL-2.1-only"]="EPL-2.0"
)
declare -Ar HARDCODED_LICENSE_URLS=(
  ["EPL-2.0"]="https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt"
)
declare -Ar HARDCODED_COPYRIGHT_NOTICES=(
  ["com.github.luben:zstd-jni"]="Copyright (c) 2015-present, Luben Karavelov/ All rights reserved"
)

main() {
  step "Update Notice"
  if [[ "${GITHUB_ACTIONS:-false}" == "true" ]]; then
    warn "Skipping in GitHub Actions"
    return
  fi
  if [[ -f "${THIRD_PARTY}" ]]; then
    validateLibs
    generateNotice
    if [[ "${isUpdated:-false}" == "false" ]]; then
      warn "Nothing to update"
    fi
  else
    warn "${THIRD_PARTY} is missing"
  fi
}

validateLibs() {
  local thirdPartyDependenciesCount libsCount libsSourcesCount
  thirdPartyDependenciesCount=$(grep "Lists of" "${THIRD_PARTY}" | awk '{print $3}')
  if [[ ! "${thirdPartyDependenciesCount}" =~ ^[0-9]+$ ]]; then
    error "Could not determine third party dependencies count from ${THIRD_PARTY}"
    return 1
  fi
  if [[ -d "${LIBS_DIR}" ]]; then
    libsCount="$(find "${LIBS_DIR}" -maxdepth 1 -type f ! -name 'toolfetch-*' -printf '.' | wc -c)"
    if (( libsCount != thirdPartyDependenciesCount )); then
      error "${LIBS_DIR} contains ${libsCount} files. Expected: ${thirdPartyDependenciesCount}"
      return 1
    fi
  fi
  if [[ ! -d "${LIBS_SOURCES_DIR}" ]]; then
    error "Directory does not exist: ${LIBS_SOURCES_DIR}"
    return 1
  fi
  libsSourcesCount="$(find "${LIBS_SOURCES_DIR}" -maxdepth 1 -type f -printf '.' | wc -c)"
  if (( libsSourcesCount != thirdPartyDependenciesCount )); then
    error "${LIBS_SOURCES_DIR} contains ${libsSourcesCount} files. Expected: ${thirdPartyDependenciesCount}" >&2
    return 1
  fi
}

generateNotice() {
  local oldNoticeSha256Sum lineRegex line name groupId artifactId version url spdxLicense newNoticeSha256Sum
  local -a licenses uniqueSpdxLicenses
  oldNoticeSha256Sum="$(sha256sum "${NOTICE}")"
  lineRegex="$(lineRegex)"

  generateNoticeHeader
  while IFS= read -r line; do
    [[ "${line}" =~ ${lineRegex} ]] || continue
    extractLineData
    validateLib
    toSpdxLicenses
    generateNoticeForLib
    generateCopyrightForLib
    generateMissingCopyrightForBundledLib
    uniqueSpdxLicenses+=("${spdxLicense}")
  done < "${THIRD_PARTY}"
  normalizeLineEndings
  unpackLicenses

  newNoticeSha256Sum="$(sha256sum "${NOTICE}")"
  if [[ "${oldNoticeSha256Sum}" != "${newNoticeSha256Sum}" ]]; then
    info "Updating NOTICE"
    isUpdated="true"
  fi
}

generateNoticeHeader() {
  cat <<EOF > "${NOTICE}"
ToolFetch
Copyright 2026 JDHeim.com

This product includes software developed at
The Apache Software Foundation (https://www.apache.org).

---

The following components are included in this product:
EOF
}

lineRegex() {
  local prefix="^[[:space:]]*"
  local licensesBlock="((\([^)]*\)[[:space:]]*)+)"
  local name="([^()]+)"
  local meta="[[:space:]]*\(([^ ]+)[[:space:]]-[[:space:]]([^)]+)\)$"
  printf '%s%s%s%s' "${prefix}" "${licensesBlock}" "${name}" "${meta}"
}

extractLineData() {
  local licensesBlock="${BASH_REMATCH[1]}"
  name="$(trim "${BASH_REMATCH[3]}")"
  local gav="${BASH_REMATCH[4]}"

  IFS=":" read -r groupId artifactId version <<< "${gav}"

  url="${HARDCODED_URLS["${groupId}:${artifactId}"]:-}"
  if [[ -z "${url}" ]]; then
    url="${BASH_REMATCH[5]/#http:/https:}"
  fi

  licenses=()
  local licensesOutput
  licensesOutput="$(grep -oP "\(\K[^)]+" <<< "${licensesBlock}")"
  if [[ -n "${licensesOutput}" ]]; then
    mapfile -t licenses <<< "${licensesOutput}"
  fi
}

validateLib() {
  if ! grep -q "${groupId}:${artifactId}:jar:${version}" "${LIBS}"; then
    error "${groupId}:${artifactId}:jar:${version} not found in ${LIBS}"
    return 1
  fi
}

toSpdxLicenses() {
  local spdxLicenses license convertedLicense
  spdxLicenses=()
  for license in "${licenses[@]}"; do
    convertedLicense=$(toSpdxLicense)
    spdxLicenses+=("${convertedLicense}")
  done
  determineSpdxLicense
}

toSpdxLicense() {
  for pattern in "${!SPDX_LICENSES[@]}"; do
    if [[ "${license,,}" =~ ${pattern,,} ]]; then
      printf '%s\n' "${SPDX_LICENSES[${pattern}]}"
      return 0
    fi
  done

  error "Unknown license: ${license}" >&2
  return 1
}

determineSpdxLicense() {
  if (( ${#spdxLicenses[@]} > 1 )); then
    local key
    key=$(printf "%s\n" "${spdxLicenses[@]}" | sort | paste -s -d '|')

    if [[ -n "${SPDX_LICENSE_PRECEDENCE[${key}]:-}" ]]; then
      spdxLicense="${SPDX_LICENSE_PRECEDENCE[${key}]}"
    else
      error "Review multiple licenses:"
      for i in "${!spdxLicenses[@]}"; do
        printf 'spdxLicenses[%d]=%s\n' "${i}" "${spdxLicenses[${i}]}"
      done
      return 1
    fi
  else
    spdxLicense=${spdxLicenses[0]}
  fi
}

generateNoticeForLib() {
  printf '\n' >> "${NOTICE}"
  cat <<EOF >> "${NOTICE}"
${name}
${url}
Licensed under ${spdxLicense} (https://spdx.org/licenses/${spdxLicense}.html)
EOF
}

generateCopyrightForLib() {
  local jarName="${artifactId}-${version}-sources.jar"
  local jarPath="${LIBS_SOURCES_DIR}/${jarName}"

  if [[ ! -d "${LIBS_NOTICE_DIR}" ]]; then
    mkdir -p "${LIBS_NOTICE_DIR}"
  fi

  local copyrightSources=(
    'META-INF/*NOTICE*'
    'META-INF/*LICENSE*'
    'META-INF/maven/*/*/pom.xml'
    '*package-info.java'
    '*module-info.java'
    '*package.html'
  )
  local genericCopyrightRegex="[[:space:]]*(~|/?\*\*?|///?)?[[:space:]]*(SPDX-FileCopyrightText:|Copyright)"
  local copyrightRegexes=(
    "(Copyright)"
    "(Copyright)"
    "${genericCopyrightRegex}"
    "${genericCopyrightRegex}"
    "${genericCopyrightRegex}"
    "${genericCopyrightRegex}"
  )

  local copyrightSourceContent copyrightRegexPrefix foundNotice thirdPartyCopyright firstJavaFile
  for i in "${!copyrightSources[@]}"; do
    copyrightSourceContent="$(unzip -p "${jarPath}" "${copyrightSources[${i}]}" 2>/dev/null || true)"
    [[ -z "${copyrightSourceContent}" ]] && continue

    if [[ "${copyrightSources[${i}]}" == *NOTICE* && -n "${copyrightSourceContent}" ]]; then
      printf '%s\n' "${copyrightSourceContent}" > "${LIBS_NOTICE_DIR}/NOTICE-${jarName%.jar}"
      foundNotice="true"
    fi

    thirdPartyCopyright=$(printf '%s\n' "${copyrightSourceContent}" | grep -m 1 -E "^${copyrightRegexes[${i}]}" || true)
    [[ -z "${thirdPartyCopyright}" ]] && continue

    copyrightRegexPrefix="${copyrightRegexes[${i}]%\(*}"
    if [[ -n "${copyrightRegexPrefix}" ]]; then
      thirdPartyCopyright=$(printf '%s\n' "${thirdPartyCopyright}" | sed -E "s@^${copyrightRegexPrefix}@@")
    fi
    if [[ "${thirdPartyCopyright}" =~ ^SPDX-FileCopyrightText: ]]; then
      thirdPartyCopyright="${thirdPartyCopyright#SPDX-FileCopyrightText:}"
      thirdPartyCopyright="$(trim "${thirdPartyCopyright}")"
      if [[ ! "${thirdPartyCopyright}" =~ ^Copyright ]]; then
        thirdPartyCopyright="Copyright (c) ${thirdPartyCopyright}"
      fi
    fi

    break
  done

  if [[ -z "${thirdPartyCopyright:-}" && "${foundNotice:-false}" == "true" ]]; then
    error "Found NOTICE in ${jarPath}, but Copyright could not be found"
    return 1
  fi

  if [[ -z "${thirdPartyCopyright:-}" ]]; then
    firstJavaFile="$(unzip -Z1 "${jarPath}" | grep -E "\.java$" | head -n1)"
    if [[ -n "${firstJavaFile}" ]]; then
      copyrightSourceContent="$(unzip -p "${jarPath}" "${firstJavaFile}")"
      thirdPartyCopyright=$(printf '%s\n' "${copyrightSourceContent}" | grep -m 1 -E "^${genericCopyrightRegex}" || true)
      copyrightRegexPrefix="${genericCopyrightRegex%\(*}"
      thirdPartyCopyright=$(printf '%s\n' "${thirdPartyCopyright}" | sed -E "s@^${copyrightRegexPrefix}@@")
    fi
  fi

  if [[ -z "${thirdPartyCopyright:-}" ]]; then
    thirdPartyCopyright="${HARDCODED_COPYRIGHT_NOTICES["${groupId}:${artifactId}"]:-}"
  fi

  if [[ -n "${thirdPartyCopyright:-}" ]]; then
    printf '%s\n' "${thirdPartyCopyright}" >> "${NOTICE}"
  else
    error "Missing Copyright Notice for ${jarPath}"
    return 1
  fi
}

generateMissingCopyrightForBundledLib() {
  if [[ "${artifactId}" == "zstd-jni" ]]; then
    uniqueSpdxLicenses+=("BSD-3-Clause")
    printf '\n' >> "${NOTICE}"
    cat <<EOF >> "${NOTICE}"
zstd-jni bundles the Zstandard software
https://github.com/facebook/zstd
Licensed under BSD-3-Clause (https://spdx.org/licenses/BSD-3-Clause.html)
Copyright (c) Meta Platforms, Inc. and affiliates. All rights reserved.
EOF
  fi
}

normalizeLineEndings() {
  sed -i 's/\r$//' "${NOTICE}"
}

unpackLicenses() {
  local sortedSpdxLicenses
  sortedSpdxLicenses="$(printf "%s\n" "${uniqueSpdxLicenses[@]}" | sort -u)"
  if [[ -n "${sortedSpdxLicenses}" ]]; then
    mapfile -t uniqueSpdxLicenses <<< "${sortedSpdxLicenses}"
  fi
  if [[ ! -d "${LIBS_LICENSE_DIR}" ]]; then
    mkdir -p "${LIBS_LICENSE_DIR}"
  fi
  if [[ ! -d "${LICENSE_CACHE_DIR}" ]]; then
    mkdir -p "${LICENSE_CACHE_DIR}"
  fi
  for uniqueSpdxLicense in "${uniqueSpdxLicenses[@]}"; do
    local cachedLicense="${LICENSE_CACHE_DIR}/LICENSE-${uniqueSpdxLicense}"
    if [[ ! -f "${cachedLicense}" ]]; then
      local spdxLicenseUrl="${HARDCODED_LICENSE_URLS[${uniqueSpdxLicense}]:-}"
      if [[ -z "${spdxLicenseUrl}" ]]; then
        spdxLicenseUrl="https://spdx.org/licenses/${uniqueSpdxLicense}.txt"
      fi
      info "Downloading from ${spdxLicenseUrl}"
      if ! wget -q --show-progress -O "${cachedLicense}" "${spdxLicenseUrl}"; then
        rm -f "${cachedLicense}"
        error "Failed to download license from ${spdxLicenseUrl}"
        return 1
      fi
    fi
    cp "${cachedLicense}" "${LIBS_LICENSE_DIR}/"
  done
}

main "$@"
