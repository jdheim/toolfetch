<p align="center">
    <a href="https://github.com/jdheim/toolfetch/releases" rel="noreferrer">
        <img src="https://img.shields.io/github/v/release/jdheim/toolfetch?label=Latest%20Release&logo=github&logoColor=white" alt="Latest Release"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/releases" rel="noreferrer">
        <img src="https://img.shields.io/github/downloads/jdheim/toolfetch/total?label=Downloads&logo=github&logoColor=white" alt="Downloads"/>
    </a>
    <a href="https://slsa.dev" rel="noreferrer">
        <img src="https://slsa.dev/images/gh-badge-level3.svg" alt="SLSA: Level 3"/>
    </a>
    <a href="LICENSE" rel="noreferrer">
        <img src="https://img.shields.io/github/license/jdheim/toolfetch?label=License&logo=googledocs&logoColor=white" alt="License"/>
    </a>
    <br/>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/tests-and-scans.yml?label=Tests%20%26%20Scans&logo=github&logoColor=white&branch=main" alt="Tests & Scans"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/scheduled-security-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/scheduled-security-scans.yml?label=Security%20Scans&logo=github&logoColor=white&branch=main" alt="Security Scans"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/github-code-scanning/codeql" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/github-code-scanning/codeql?label=CodeQL&logo=github&logoColor=white&branch=main" alt="CodeQL"/>
    </a>
    <br/>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jdheim/toolfetch/refs/heads/badges/test-coverage.json" alt="Test Coverage (%)"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jdheim/toolfetch/refs/heads/badges/test-condition-coverage.json" alt="Test Condition Coverage (%)"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jdheim/toolfetch/refs/heads/badges/duplicated-lines.json" alt="Duplicated Lines (%)"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jdheim/toolfetch/refs/heads/badges/sonarqube-code-issues.json" alt="SonarQube Code Issues"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/tests-and-scans.yml" rel="noreferrer">
        <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jdheim/toolfetch/refs/heads/badges/sonarqube-security-hotspots.json" alt="SonarQube Security Hotspots"/>
    </a>
</p>

## ToolFetch

**ToolFetch** is a CLI for **fetching and installing external tools**
from release URLs (e.g. GitHub releases) using a YAML configuration file.

It is designed for:

- setting up new developer machines quickly and consistently
- reproducible tool installations

Installation demo using [toolfetch.yaml](assets/tapes/toolfetch.yaml):

![Demo](assets/demo.gif)

## Installation

1. Download the latest release for your OS/architecture from [Releases](https://github.com/jdheim/toolfetch/releases)
2. Move the `toolfetch` binary to a directory in your `$PATH` (e.g., system-wide: `/usr/local/bin` or user-specific:
   `$HOME/.local/bin`)

## Verify Releases

See [VERIFICATION.md](VERIFICATION.md) for details.

## Usage

Given the following configuration file named `toolfetch.yaml`:

```yaml
destination: "/opt"
tools:
  - id: "toolfetch"
    url: "https://github.com/jdheim/toolfetch/releases/download/v0.0.3/toolfetch-0.0.3-linux-amd64.tar.gz"
```

When you invoke the command: `toolfetch --config "toolfetch.yaml"`, the latest version of the tool will be installed like this:

```shell
/opt
|-- toolfetch
```

---

You can use optional placeholder: `${version}` in `url` which will be replaced with `version` value at runtime:

```yaml
destination: "/opt"
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
  - id: "intellij-idea"
    version: "2026.1.1"
    url: "https://download.jetbrains.com/idea/idea-${version}.tar.gz"
```

When you invoke the command again, tools will be installed like this:

```shell
/opt
|-- intellij-idea
|-- toolfetch
```

---

You can optionally define a `destination` key for a specific tool to install it somewhere else:

```yaml
destination: "/opt"
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
    destination: "best-tools" # or absolute path: "/opt/best-tools"
  - id: "intellij-idea"
    version: "2026.1.1"
    url: "https://download.jetbrains.com/idea/idea-${version}.tar.gz"
```

Now, when you invoke the same command, tools will be installed like this:

```shell
/opt
|-- intellij-idea
|-- best-tools
    |-- toolfetch
```

---

### Checksum Verification Formats

You can optionally define a `checksums` key for a specific tool to verify the downloaded archive before it is extracted:

```yaml
destination: "/opt"
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
    checksums:
      sha256: "5c0a98ae80e06eea619ae878d84748154405903b42fc12c1cd02ddf98440eb77"
  - id: "intellij-idea"
    version: "2026.1.1"
    url: "https://download.jetbrains.com/idea/idea-${version}.tar.gz"
    checksums:
      sha256: "7a58d386f2a2e5a8cd7e4591657b4fe599aeac22d960c7accf5f927846507bfb"
```

Currently, the following Checksum Verification Formats are supported:

- `sha256`
- `sha384`
- `sha512`

---

### Environment Variables

You can optionally use environment variables in `toolfetch.yaml` using either `$VAR` or `${VAR}` syntax.

Currently supported in:

- `destination`
- `http.ssl.trustStore.path`
- `tools[].destination`

Example:

```yaml
destination: "$HOME/tools"
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
    destination: "${DEV_HOME}/best-tools"
  - id: "intellij-idea"
    version: "2026.1.1"
    url: "https://download.jetbrains.com/idea/idea-${version}.tar.gz"
```

---

### HTTP Client Settings

You can optionally define an `http` key to customize HTTP client settings:

```yaml
destination: "/opt"
http:
  connectTimeout: 30  # HTTP connect timeout in seconds. Default: 10 seconds
  requestTimeout: 300 # HTTP request timeout in seconds. Default: 900 seconds (15 minutes)
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
```

If your organization uses a custom Certificate Authority, you may need to configure a TrustStore:

```yaml
destination: "/opt"
http:
  ssl:
    trustStore:
      path: "/path/to/truststore" # Location of the TrustStore file containing trusted CA certificates. Default: $JAVA_HOME/lib/security/cacerts, $JAVA_HOME/jre/lib/security/cacerts or the bundled default TrustStore
      type: "PKCS12"              # TrustStore type. Default: autodetected. Set the type if autodetection fails
      # If the TrustStore is password-protected, specify the password using the TOOLFETCH_HTTP_SSL_TRUSTSTORE_PASSWORD environment variable
tools:
  - id: "toolfetch"
    version: "0.0.3"
    url: "https://github.com/jdheim/toolfetch/releases/download/v${version}/toolfetch-${version}-linux-amd64.tar.gz"
```

Otherwise, you may encounter an exception like this:

```text
(certificate_unknown) PKIX path building failed:
sun.security.provider.certpath.SunCertPathBuilderException:
unable to find valid certification path to requested target
```

> [!NOTE]  
> TrustStore precedence:
> 1. `http.ssl.trustStore.path`
> 2. `$JAVA_HOME/lib/security/cacerts` (JDK 9+)
> 3. `$JAVA_HOME/jre/lib/security/cacerts` (JDK 8)
> 4. The bundled default TrustStore

> [!TIP]  
> If you [import a Certificate for the CA](https://dev.java/learn/jvm/tool/security/keytool/#importing-for-ca) into `$JAVA_HOME/lib/security/cacerts`
> or `$JAVA_HOME/jre/lib/security/cacerts`, you do not need to configure `http.ssl.trustStore`.

## Archive and Compression Formats

Currently, the following Archive Formats are supported:

- `tar`
- `zip`
- `jar`

> [!WARNING]  
> `7z` support is planned

and Compression Formats:

- `brotli`
- `bzip2`
- `deflate`
- `gzip`
- `lz4`
- `lzma`
- `pack200` (for `jars`)
- `snappy` (excluding `iwa`)
- `xz`
- `z`
- `zstandard`
- concatenated streams for `bzip2`, `gzip`, `xz` and `lz4`

## 💖 Support

Hey there! If you enjoy my work and would like to support me, consider buying me a coffee! :slightly_smiling_face: Your contributions help me keep
creating, and I truly appreciate every bit of support you offer.

<p>
  <a href="https://www.buymeacoffee.com/jdheim" rel="noreferrer">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy me a Coffee" style="height: 40px !important;width: 160px !important;" >
  </a>
</p>

Also, please consider giving this project a ⭐ on GitHub. This kind of support helps promote the project and lets others know that it's worth checking
out.

Thank you for being amazing!

## ©️ License

Copyright 2026 JDHeim.com

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for full license terms.
