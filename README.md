<p align="center">
    <a href="https://github.com/jdheim/toolfetch/releases" rel="noreferrer">
        <img src="https://img.shields.io/github/v/release/jdheim/toolfetch?label=Latest%20Release&logo=github&logoColor=white" alt="Latest Release"/>
    </a>
    <a href="LICENSE" rel="noreferrer">
        <img src="https://img.shields.io/github/license/jdheim/toolfetch?label=License&logo=googledocs&logoColor=white" alt="License"/>
    </a>
    <br/>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/test-report.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/test-report.yml?label=Tests&logo=github&logoColor=white&branch=main" alt="Tests"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/test-report-native.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/test-report-native.yml?label=Smoke%20Tests&logo=github&logoColor=white&branch=main" alt="Smoke Tests"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/scan-pr.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/scan-pr.yml?label=Scans&logo=github&logoColor=white&branch=main" alt="Scans"/>
    </a>
    <a href="https://github.com/jdheim/toolfetch/actions/workflows/scan-owasp.yml" rel="noreferrer">
        <img src="https://img.shields.io/github/actions/workflow/status/jdheim/toolfetch/scan-owasp.yml?label=OWASP%20Scan&logo=github&logoColor=white&branch=main" alt="OWASP Scan"/>
    </a>
</p>

## ToolFetch

**ToolFetch** is a CLI for **fetching and installing external tools**
from release URLs (e.g. GitHub releases) using a YAML configuration file.

It is designed for:

- setting up new developer machines quickly and consistently
- reproducible tool installations

---

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

You can add optional `destination` key for specific tool in order to install it somewhere else:

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

## Archive and Compression Formats

Currently, the following Archive Formats are supported:

- `7z` - planned, work in progress
- `tar`
- `zip`
- `jar`

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

## SBOM

All releases include a **Software Bill of Materials (SBOM)** describing the dependencies used to build the binary.

An SBOM is provided in two forms:

- As a **standalone SBOM file** included in the release artifacts
- **Embedded** inside the executable

The distributed binaries are compressed with UPX, so the executable must first be decompressed before scanning.

Install the required tools:

- [UPX](https://upx.github.io)
- [Syft](https://oss.anchore.com/docs/installation/syft)

Then produce an uncompressed binary `toolfetch-raw`:

```shell
upx -d -o toolfetch-raw toolfetch
```

Next, generate the SBOM from the binary:

```shell
syft toolfetch-raw
```

## Provenance

### PGP

All release artifacts are signed with **Pretty Good Privacy (PGP)**. Follow these instructions to verify artifacts
against their signatures:

- Download the [public key](https://github.com/p-marcin/p-marcin/blob/main/gpg/jdheim.asc). Save it as `jdheim.asc`:

```shell
$ wget https://raw.githubusercontent.com/p-marcin/p-marcin/main/gpg/jdheim.asc
```

- Verify the fingerprint matches the following:

```shell
$ gpg --show-keys jdheim.asc
pub   rsa4096 2026-03-01 [SC]
      FFBE9F2EC1AF21943BBE06A35E0566252E0EC8A1
uid                      Marcin P. (jdheim) <114195537+p-marcin@users.noreply.github.com>
sub   rsa4096 2026-03-01 [E]
```

- Import the key with `gpg --import jdheim.asc`
- Verify the chosen artifact with:

```shell
$ gpg toolfetch-0.0.1-linux-amd64.tar.gz.asc
gpg: WARNING: no command supplied.  Trying to guess what you mean ...
gpg: assuming signed data in 'toolfetch-0.0.1-linux-amd64.tar.gz'
gpg: Signature made Sun Mar  1 18:55:02 2026 CET
gpg:                using RSA key 5E0566252E0EC8A1
gpg: Good signature from "Marcin P. (jdheim) <114195537+p-marcin@users.noreply.github.com>" [unknown]
gpg: WARNING: This key is not certified with a trusted signature!
gpg:          There is no indication that the signature belongs to the owner.
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

© 2026-2026 JDHeim.com

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for full license terms.
