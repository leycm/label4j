<!-- Back to top -->
<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![LGPL License][license-shield]][license-url]

<br />
<div align="center">
  <h1 align="center">i18label4j</h1>

  <p align="center">
    A simple and lightweight registry library with flexible identifiers and namespaces for Java.<br/>
    Localize your application cleanly with typed labels, dynamic placeholders, and pluggable serializers.
    <br />
    <br />
    <a href="https://github.com/leycm/i18label4j"><strong>Explore the docs »</strong></a>
    &nbsp;·&nbsp;
    <a href="https://github.com/leycm/i18label4j/issues/new?labels=bug">Report Bug</a>
    &nbsp;·&nbsp;
    <a href="https://github.com/leycm/i18label4j/issues/new?labels=enhancement">Request Feature</a>
  </p>
</div>

---

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about-the-project">About The Project</a></li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#architecture">Architecture</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

---

## About The Project

**i18label4j** is a modular Java library for managing localizable text labels with a clean, fluent API. It provides:

- **Typed labels**: distinguish between locale-aware i18n labels, global labels and immutable literal labels at compile time.
- **Placeholder substitution**: register static or dynamic `Placeholder` objects on any label and apply them via configurable `PlaceholderRule` strategies (supports `${key}`, `{key}`, `%key%`, `<key>`, and more out of the box).
- **Pluggable serializers**: convert labels to any target type (plain `String`, Adventure `Component`, etc.) by registering a `LabelSerializer`.
- **Multiple localization sources**: load translations from a flat directory  `DeepDirSource` (supports file, resources, http and more), or implement your own `LocalizationSource`.
- **Format support**: JSON, YAML, TOML, and Java `.properties` files are supported out of the box.
- **Translation caching**: the `CommonLabelProvider` caches translations per locale with thread-safe `ConcurrentHashMap` internals and explicit cache eviction.
- **Fallback**: it supports fallbacks to default or to a fallback string `!{key}` or similar
<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

- [![Java][java-badge]][java-url]
- [![Gradle][gradle-badge]][gradle-url]
- [![Lombok][lombok-badge]][lombok-url]
- [snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml) · [toml4j](https://github.com/moandjiezana/toml4j) · [org.json](https://github.com/stleary/JSON-java)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Getting Started

### Prerequisites

- Java 21+
- Gradle 9+ (wrapper included)

### Installation

Add the repository and dependency to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://leycm.github.io/repository/")
}

dependencies {
    // API only (compile against the interface)
    compileOnly("de.leycm:label4j-api:2.0.0")

    // Full implementation (includes CommonLabelProvider, FileSource, DirSource, etc.)
    implementation("de.leycm.label4j-impl:2.0.0")
}
```

Or with Maven (`pom.xml`):

```xml
<repository>
  <id>leycm-repo</id>
  <url>https://leycm.github.io/repository/</url>
</repository>

<dependency>
  <groupId>de.leycm</groupId>
  <artifactId>label4j-api</artifactId>
  <version>2.0.0</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>de.leycm</groupId>
  <artifactId>label4j-impl</artifactId>
  <version>2.0.0</version>
</dependency>
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Usage
For Usage and How to start
<a href="https://github.com/leycm/i18label4j"><strong>explore the docs</strong></a>
<!-- todo: wiki -->

## Architecture

```
i18label4j
├── i18-api/          # Public API - Label, LabelProvider, Placeholder, PlaceholderRule, LabelSerializer, LocalizationSource
└── i18-impl/         # Implementation - CommonLabelProvider, LiteralLabel, LocaleLabel,
                      #                  FileSource, DirSource, FileParser, FileUtils
```

The library is split into two modules so downstream projects can depend only on the API and swap implementations at runtime via `Instanceable.register(...)`.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Roadmap

- [x] Core `Label` API with `LiteralLabel` and `LocaleLabel`
- [x] `CommonLabelProvider` with thread-safe translation cache
- [x] `MappingRule` with 10+ built-in placeholder styles
- [x] `FileSource` and `DirSource` with JSON, YAML, TOML, `.properties` support
- [x] Classpath (`resource://`), filesystem (`file://`), and remote (`http(s)://`) URI schemes
- [x] Nested/hierarchical key support in `DirSource`
- [x] Hot-reload support for translation files
- [ ] Maven / Gradle plugin for compile-time key validation
- [ ] Additional serializer modules (Adventure, MiniMessage)

See the [open issues](https://github.com/leycm/i18label4j/issues) for the full list of proposed features and known bugs.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Contributing

Contributions are what make open source such a great place to learn and build. Any contributions you make are **greatly appreciated**.

1. Fork the project
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add some amazing Features'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## License

Distributed under the **GNU Lesser General Public License v3.0**. See [`LICENSE.LGPL`](LICENSE.LGPL) for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Contact

**Lennard** — leycm@proton.me

Project Link: [https://github.com/leycm/label4j](https://github.com/leycm/i18label4j)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Acknowledgments

- [Lombok](https://projectlombok.org/) boilerplate-free Java
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) YAML parsing
- [toml4j](https://github.com/moandjiezana/toml4j) TOML parsing
- [org.json](https://github.com/stleary/JSON-java) JSON parsing
- [Adventure API](https://docs.advntr.net/) Minecraft text component library
- [Best-README-Template](https://github.com/othneildrew/Best-README-Template) README structure inspiration
- [Shields.io](https://shields.io) better readmes

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

<!-- MARKDOWN LINKS & BADGES -->
[contributors-shield]: https://img.shields.io/github/contributors/leycm/i18label4j.svg?style=for-the-badge
[contributors-url]: https://github.com/leycm/i18label4j/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/leycm/i18label4j.svg?style=for-the-badge
[forks-url]: https://github.com/leycm/i18label4j/network/members
[stars-shield]: https://img.shields.io/github/stars/leycm/i18label4j.svg?style=for-the-badge
[stars-url]: https://github.com/leycm/i18label4j/stargazers
[issues-shield]: https://img.shields.io/github/issues/leycm/i18label4j.svg?style=for-the-badge
[issues-url]: https://github.com/leycm/i18label4j/issues
[license-shield]: https://img.shields.io/github/license/leycm/i18label4j.svg?style=for-the-badge
[license-url]: https://github.com/leycm/i18label4j/blob/master/LICENSE.LGPL

[java-badge]: https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[java-url]: https://openjdk.org/projects/jdk/21/
[gradle-badge]: https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white
[gradle-url]: https://gradle.org/
[lombok-badge]: https://img.shields.io/badge/Lombok-BC4521?style=for-the-badge&logo=lombok&logoColor=white
[lombok-url]: https://projectlombok.org/
