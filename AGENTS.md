# Repository guide

This repository contains a Java 11 OmegaT machine-translation plugin.

## Architecture

- `AiTranslatePlugin` is the manifest entry point.
- `AiTranslate` is the OmegaT machine-translation adapter.
- `ProviderDialog` owns the Swing configuration UI.
- `ProviderRepository` persists non-secret settings.
- API keys must always use OmegaT's credential manager through `AiTranslate`.
- `OpenAiClient` implements the dependency-free Chat Completions transport.

## Development rules

- Keep the distributable JAR self-contained and avoid runtime dependencies where practical.
- Never log API keys, authorization headers, source segments, or translated content.
- Preserve `{{text}}` as a required prompt variable.
- Add tests for JSON parsing, prompt expansion, and protocol changes.
- Run `gradle clean test jar` before release.
