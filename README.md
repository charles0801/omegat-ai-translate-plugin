# OmegaT AI Translate Plugin

[简体中文](README.zh-CN.md)

An [OmegaT](https://omegat.org/) machine-translation plugin for OpenAI and
OpenAI-compatible Chat Completions APIs. It can use a hosted provider or a local
Llama server such as Ollama, LM Studio, or llama.cpp.

The plugin shows suggestions for the current segment in OmegaT's Machine
Translation pane. It can also translate the untranslated segments of the
current document directly into the project.

## Features

- Manage multiple providers, endpoints, models, authentication settings, and prompts.
- Store API keys with OmegaT's credential manager, persistently or for this session only.
- Send matching OmegaT glossary terms as guidance for each segment.
- Request a fresh suggestion for the current segment without changing its target text.
- Batch-translate the current document with progress and cancellation; existing
  translations are not overwritten.
- Configure a per-request timeout (60 seconds by default; 1–600 seconds in the UI).

## Requirements and installation

- OmegaT 6.0.0 is the version this plugin is built against. Other versions have
  not been tested.
- OmegaT must run on Java 11 or newer. The JAR uses Java 11 bytecode (class-file
  version 55).
- Gradle and a Java 11+ JDK are needed only when building from source.

Build and test:

```sh
gradle clean test jar
```

Copy the JAR from `build/libs/` to the `plugins` directory in your OmegaT
configuration folder, remove any older copies of this plugin, and restart
OmegaT. The distributable JAR does not bundle OmegaT or require additional
plugin-specific runtime libraries.

Once a version has been published, its JAR can also be downloaded from
[GitHub Releases](https://github.com/charles0801/omegat-ai-translate-plugin/releases).

### Publish a release

The [release workflow](.github/workflows/release.yml) runs only when a version
tag such as `v0.4.0` is pushed. It tests the tagged source, builds a JAR named
for the tag with the matching manifest version, and uploads it to a GitHub
Release. Ordinary commits do not publish a release. After the intended commit
has been pushed, create and push a tag:

```sh
git tag -a v0.4.0 -m "v0.4.0"
git push origin v0.4.0
```

Use a new `vX.Y.Z` tag for each release; do not move a published tag. The CI
build runs Gradle on JDK 21 but compiles the plugin for Java 11. It uses the
repository's `GITHUB_TOKEN`; no personal access token is needed.

## Set up a provider

1. Open an OmegaT project.
2. Open **Options → Preferences → Machine Translation** and enable
   **AI Translate**.
3. Open its configuration dialog. Select a provider or use **Add**.
4. In **Connection**, set the base URL, model, authorization settings, API key,
   temperature, and timeout. In **Prompts**, edit the system and user prompts.
5. Save, then open OmegaT's **Machine Translation** pane to see suggestions.

The base URL normally ends in `/v1`; the plugin appends `/chat/completions`.
A complete `/chat/completions` URL also works. Choose a model actually served
by your provider. Local servers commonly need no API key; leave it empty if so.

| Example provider | Base URL | Typical authentication |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | `Authorization: Bearer <key>` |
| Ollama | `http://localhost:11434/v1` | None |
| LM Studio | `http://localhost:1234/v1` | None |
| llama.cpp server | `http://localhost:8080/v1` | Depends on configuration |

The configured timeout applies to **each API request**, not to an entire
document batch. A provider error or timeout stops the batch; translations
already completed remain in the project.

## Translate

| Action | Menu | Linux / Windows | macOS |
| --- | --- | --- | --- |
| Request current segment again | Options → Machine Translation → Request machine translation again | `Ctrl+Alt+M` | `Command+Option+M` |
| Translate current document | Options → Machine Translation → Translate current document with AI... | `Ctrl+Alt+B` | `Command+Option+B` |

**Request current segment again** refreshes all enabled MT engines, including
when automatic fetching is off or the segment already has a translation. It
only refreshes the suggestion; it does not insert or replace target text.

**Translate current document** asks for confirmation, then sends one request
at a time for untranslated segments in the open file. It uses glossary matches
for each segment. Empty responses and responses that alter OmegaT tags are
skipped. Canceling keeps completed translations, which are saved when the batch
stops. The open document is then reloaded so the new translations appear in the
editor. Identical source text in other project files is not changed by this
command. Review the results before delivery.

## Prompts and glossary

The user prompt must contain `{{text}}`. Available variables are:

| Variable | Meaning |
| --- | --- |
| `{{sourceLanguage}}` | Project source language |
| `{{targetLanguage}}` | Project target language |
| `{{text}}` | Current source segment (required in the user prompt) |
| `{{glossary}}` | OmegaT glossary terms matching this segment |

Put project glossaries in OmegaT's project `glossary` directory. The plugin
sends only terms OmegaT matches for the segment, not the entire glossary. Put
`{{glossary}}` in either prompt to control its placement. If neither prompt
contains it, matching terms are appended to the user prompt automatically.
Terminology is guidance, not a guarantee; verify the model's choices.

For technical English-to-Chinese translation, a starting system prompt is:

```text
You are translating a computer-technology book from English into Simplified Chinese.
Preserve technical meaning, code, commands, paths, URLs, placeholders, and OmegaT tags exactly.
Use the supplied glossary terms when they match the intended meaning.
Write natural, consistent technical Chinese. Do not add explanations or omit content.
Return only the translation.
```

The default user prompt already includes the source language, target language,
and `{{text}}`; edit it as needed.

## Privacy and cost

Source segments and matching glossary terms are sent to the selected provider.
Only use a provider you trust with the project's content. Requests may incur
charges, especially for a document batch. API keys are stored through OmegaT's
credential manager rather than in the plugin's provider settings.

## Scope and license

This version implements the OpenAI-compatible Chat Completions protocol. It
does not implement provider-specific protocols or OpenAI's Responses API.

Licensed under GPL-3.0-or-later; see [LICENSE](LICENSE).
