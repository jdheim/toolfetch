<p align="center">
    <a href="https://github.com/jdheim/toolfetch/releases" rel="noreferrer">
        <img src="https://img.shields.io/github/v/release/jdheim/toolfetch?label=Latest%20Release&logo=github&logoColor=white" alt="Latest Release"/>
    </a>
    <a href="LICENSE" rel="noreferrer">
        <img src="https://img.shields.io/github/license/jdheim/toolfetch?label=License&logo=googledocs&logoColor=white" alt="License"/>
    </a>
    <a href="https://slsa.dev" rel="noreferrer">
        <img src="https://slsa.dev/images/gh-badge-level3.svg" alt="SLSA: Level 3"/>
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
    <a href="https://github.com/jdheim/toolfetch/releases" rel="noreferrer">
        <img src="https://img.shields.io/github/downloads/jdheim/toolfetch/total?label=Downloads&logo=github&logoColor=white" alt="Downloads"/>
    </a>
</p>

## ToolFetch

**ToolFetch** is a CLI for **fetching and installing external tools**
from release URLs (e.g. GitHub releases) using a YAML configuration file.

It is designed for:

- setting up new developer machines quickly and consistently
- reproducible tool installations

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
destination: /opt
tools:
  - id: dbeaver
    url: "https://dbeaver.io/files/dbeaver-ce-latest-linux.gtk.x86_64.tar.gz"
```

When you invoke the command: `toolfetch --config "toolfetch.yaml"`, the latest version of the tool will be installed
like this:

```shell
/opt
|-- dbeaver
```

---

You can use optional placeholder: `${version}` in `url` which will be replaced with `version` value at runtime:

```yaml
destination: "/opt"
tools:
  - id: kitty
    version: "0.44.0"
    url: "https://github.com/kovidgoyal/kitty/releases/download/v${version}/kitty-${version}-x86_64.txz"
  - id: firefox
    version: "146.0.1"
    url: "https://ftp.mozilla.org/pub/firefox/releases/${version}/linux-x86_64/en-US/firefox-${version}.tar.xz"
```

When you invoke the command again, tools will be installed like this:

```shell
/opt
|-- kitty
|-- firefox
```

---

You can optionally define a `destination` key for a specific tool to install it somewhere else:

```yaml
destination: "/opt"
tools:
  - id: kitty
    version: "0.44.0"
    url: "https://github.com/kovidgoyal/kitty/releases/download/v${version}/kitty-${version}-x86_64.txz"
    destination: "/opt/tools"
  - id: firefox
    version: "146.0.1"
    url: "https://ftp.mozilla.org/pub/firefox/releases/${version}/linux-x86_64/en-US/firefox-${version}.tar.xz"
```

Now, when you invoke the same command, tools will be installed like this:

```shell
/opt
|-- firefox
|-- tools
    |-- kitty
```

---

You can optionally define a `checksums` key for a specific tool to verify the downloaded archive before it is extracted:

```yaml
destination: "/opt"
tools:
  - id: kitty
    version: "0.44.0"
    url: "https://github.com/kovidgoyal/kitty/releases/download/v${version}/kitty-${version}-x86_64.txz"
    checksums:
      sha256: "5b502801c8814c9fc5a2e8d9cfdf1c2ec5ee78b3e647f898704ad537a2ff452d"
  - id: firefox
    version: "146.0.1"
    url: "https://ftp.mozilla.org/pub/firefox/releases/${version}/linux-x86_64/en-US/firefox-${version}.tar.xz"
    checksums:
      sha256: "36a4dc0e3be8af2d49d8388021abf790976d2398162b9d13a6d758cc8c37f8dd"
```

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

## Checksum Verification Formats

Currently, the following Checksum Verification Formats are supported:

- `sha256`
- `sha384`
- `sha512`

## Custom Certificate Authorities

> [!WARNING]  
> TrustStore configuration support in `toolfetch.yaml` is planned

If your organization uses custom Certificate Authorities, you may need to configure a Java TrustStore.

Pass JVM options directly to `toolfetch`:

- `-Djavax.net.ssl.trustStore=path/to/truststore`
- `-Djavax.net.ssl.trustStorePassword=changeit`

Otherwise, you may encounter an exception like:

```text
(certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

## 💖 Support

Hey there! If you enjoy my work and would like to support me, consider buying me a coffee! :slightly_smiling_face: Your
contributions help me keep creating, and I truly appreciate every bit of support you offer.

<p>
  <a href="https://www.buymeacoffee.com/jdheim" rel="noreferrer">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy me a Coffee" style="height: 40px !important;width: 160px !important;" >
  </a>
</p>

Also, please consider giving this project a ⭐ on GitHub. This kind of support helps promote the project and lets others
know that it's worth checking out.

Thank you for being amazing!

## ©️ License

Copyright 2026 JDHeim.com

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for full license terms.
