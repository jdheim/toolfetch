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

- `7z` - work in progress
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
