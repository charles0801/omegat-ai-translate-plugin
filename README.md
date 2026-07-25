# OmegaT AI Translate Plugin

An OmegaT machine-translation plugin for OpenAI and OpenAI-compatible APIs,
including local Llama servers exposed by Ollama, LM Studio, llama.cpp, and
similar tools.

## Current capabilities

- Add, edit, remove, and select providers.
- Configure endpoint URL, model, authorization header/prefix, temperature, and timeout.
- Store an API key persistently through OmegaT's credential manager or only for the current session.
- Edit system and user prompts with source language, target language, and segment variables.
- Use any `/v1/chat/completions`-compatible server.

## Build

The project requires Java 11 or newer and Gradle:

```sh
gradle clean test jar
```

The plugin JAR is written to `build/libs/`. Copy it to OmegaT's `plugins`
directory (normally inside the OmegaT configuration directory), then restart
OmegaT.

## Configure

1. Open an OmegaT project.
2. Go to **Options → Preferences → Machine Translation**.
3. Enable **AI Translate**, then open its configuration.
4. Select the included OpenAI or Local Llama profile, or add a provider.
5. Enter the model and API key, edit the prompts if desired, and save.
6. Show the **Machine Translation** pane to request translations for the active segment.

The base URL should include `/v1` but not `/chat/completions`. A complete
`.../chat/completions` URL is accepted as well.

Prompt variables:

- `{{sourceLanguage}}`
- `{{targetLanguage}}`
- `{{text}}` (required in the user prompt)

## Provider examples

| Provider | Base URL | Authentication |
|---|---|---|
| OpenAI | `https://api.openai.com/v1` | `Authorization: Bearer <key>` |
| Ollama | `http://localhost:11434/v1` | Usually none |
| LM Studio | `http://localhost:1234/v1` | Usually none |
| llama.cpp server | `http://localhost:8080/v1` | Depends on server configuration |

Only send content to providers you trust. Translation segments can contain
confidential client data.

## Scope

This initial version targets the widely supported Chat Completions API.
Provider-specific APIs and OpenAI's Responses API can be added as separate
protocol adapters without changing OmegaT integration.

## License

GPL-3.0-or-later.
