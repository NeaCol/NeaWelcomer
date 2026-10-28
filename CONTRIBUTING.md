# Contributing to NeaWelcomer

Thank you for your interest in contributing to **NeaWelcomer**!

## Code Style & Standards

- **Language Level:** Java 21 LTS (`--release 21`).
- **Platform:** Paper / Purpur API 1.21.1+ (Kyori Adventure text library).
- **Asynchronous Execution:** Avoid performing synchronous filesystem I/O or network requests on the server tick thread.
- **Color & Text Formatting:** Always use MiniMessage syntax (`<color>`, `<gradient>`, `<hover>`, `<click>`) rather than legacy section codes.
- **Pull Requests:** Ensure all Maven builds pass cleanly with `mvn clean test` before opening a pull request.

## Submitting Changes

1. Fork the repository.
2. Create your feature branch (`git checkout -b feat/my-improvement`).
3. Commit your changes following [Conventional Commits](https://www.conventionalcommits.org/).
4. Push to your branch and open a Pull Request.
