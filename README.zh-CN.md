# OmegaT AI Translate Plugin

[English](README.md)

这是一个 [OmegaT](https://omegat.org/) 机器翻译插件，支持 OpenAI 及兼容
Chat Completions API 的服务，也可连接 Ollama、LM Studio、llama.cpp 等本地
Llama 服务。它既能在机器翻译窗格中显示当前片段的译文建议，也能批量翻译当前文档中
尚未翻译的片段。

## 功能

- 添加和切换多个服务商，分别配置地址、模型、鉴权方式、提示词和超时时间。
- 通过 OmegaT 凭据管理器保存 API Key，可选择持久保存或仅在本次会话中使用。
- 将 OmegaT 为每个片段匹配到的术语作为翻译参考发送给模型。
- 重新请求当前片段的机器翻译建议，不修改已输入的目标译文。
- 批量翻译当前文档：显示进度、允许取消，不覆盖已有译文。
- 每次 API 请求默认超时 60 秒，可在界面中设置为 1–600 秒。

## 安装

插件针对 OmegaT 6.0.0 构建，其他版本尚未测试。OmegaT 需要使用 Java 11
或更新版本运行；本项目生成的是 Java 11 字节码（class-file version 55）。
从源码构建还需要 Gradle 和 Java 11 或更新版本的 JDK：

```sh
gradle clean test jar
```

将 `build/libs/` 中的 JAR 复制到 OmegaT 配置目录下的 `plugins` 文件夹，
删除此插件的旧版 JAR，然后重启 OmegaT。发布用 JAR 不包含 OmegaT，也不需要
额外安装本插件专用的运行库。

## 配置服务商

1. 打开 OmegaT 项目。
2. 在 **选项 → 首选项 → 机器翻译** 中启用 **AI Translate**。
3. 打开插件配置，选择已有服务商，或点击 **Add** 新增。
4. 在 **Connection** 页填写 Base URL、Model、鉴权配置、API Key、Temperature
   和 Timeout；在 **Prompts** 页编辑提示词。
5. 保存后打开 OmegaT 的**机器翻译**窗格，即可查看当前片段的建议译文。

Base URL 通常以 `/v1` 结尾，插件会追加 `/chat/completions`；直接填写完整的
`/chat/completions` 地址也可以。模型名称必须是服务商实际提供的名称。本地服务
若不要求鉴权，可将 API Key 留空。

| 服务示例 | Base URL | 常见鉴权方式 |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | `Authorization: Bearer <key>` |
| Ollama | `http://localhost:11434/v1` | 无 |
| LM Studio | `http://localhost:1234/v1` | 无 |
| llama.cpp server | `http://localhost:8080/v1` | 取决于服务器配置 |

超时时间针对**每次请求**，不是整个文档的总时长。若服务商报错或请求超时，批量
翻译会停止，之前完成的译文仍保留在项目中。

## 翻译操作

| 操作 | 菜单 | Linux / Windows | macOS |
| --- | --- | --- | --- |
| 重新请求当前片段 | 选项 → 机器翻译 → Request machine translation again | `Ctrl+Alt+M` | `Command+Option+M` |
| 翻译当前文档 | 选项 → 机器翻译 → Translate current document with AI... | `Ctrl+Alt+B` | `Command+Option+B` |

“重新请求当前片段”会刷新所有已启用的机器翻译引擎，即使关闭了自动获取，或当前
片段已有译文，也能重新请求。它只更新建议，不会写入或覆盖目标译文。

“翻译当前文档”会先显示待翻译数量并请求确认，然后逐段发送请求。已有译文不会
被覆盖；空响应或改动 OmegaT 标签的响应会被跳过。取消时保留并保存已完成的部分。
当前文档中的相同原文即使出现在项目的其他文件里，此操作也不会修改那些文件。
交付前请人工检查结果。

## 提示词与术语表

用户提示词必须包含 `{{text}}`。可用变量如下：

| 变量 | 含义 |
| --- | --- |
| `{{sourceLanguage}}` | 项目源语言 |
| `{{targetLanguage}}` | 项目目标语言 |
| `{{text}}` | 当前原文片段（用户提示词中必需） |
| `{{glossary}}` | OmegaT 为该片段匹配的术语 |

把术语表放在 OmegaT 项目的 `glossary` 目录中。插件只发送 OmegaT 为当前片段
匹配到的词条，不会上传整份术语表。可以在系统提示词或用户提示词中放置
`{{glossary}}`；如果两处都没有，插件会自动将匹配术语附加到用户提示词后面。
模型不一定严格遵守术语，重要内容仍需校对。

计算机技术书籍英译中可从下面的系统提示词开始：

```text
你是一名计算机技术书籍译者，将英文准确译成简体中文。
保持技术含义准确，原样保留代码、命令、路径、URL、占位符和 OmegaT 标签。
术语表中的词与上下文含义相符时，使用指定译法。
译文应自然、统一，不添加解释，不删减内容。只输出译文。
```

默认用户提示词已包含源语言、目标语言和 `{{text}}`，可按需要调整。

## 隐私、费用与支持范围

原文片段和匹配术语会发送至所选服务商，请仅使用你信任的服务。请求可能产生费用，
尤其是批量翻译。API Key 由 OmegaT 凭据管理器保存，不写入普通服务商配置。

当前版本支持兼容 OpenAI 的 Chat Completions 协议，不支持服务商专有协议或
OpenAI Responses API。项目采用 GPL-3.0-or-later 许可证，详见 [LICENSE](LICENSE)。
