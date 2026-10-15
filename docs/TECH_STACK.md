# Technology Stack (Proposed)

> **Status: Proposed, not installed or benchmarked.** Confirm compatible versions before implementation. See [TODO](../TODO.md).

## Runtime / development

| Layer | Choice | Rationale / validation |
| --- | --- | --- |
| Language | Java 21 | Familiar, modern LTS baseline; developer owns Maven vs Gradle choice |
| API | Spring Boot | Validation, REST, configuration, persistence, Actuator |
| AI | Spring AI | Model abstraction, structured output, tools, RAG, Ollama and other integrations; verify chosen release compatibility with Spring Boot |
| Local inference | Ollama | Free local runs, no per-token charges |
| Primary chat model | Qwen3 4B **candidate** (`qwen3:4b`) | Test tool calls, JSON validity, speed, and memory before selection |
| Local embeddings | EmbeddingGemma **candidate** | Run separately or unload chat model when needed |
| Database | PostgreSQL | Incidents, investigation runs, evidence, agent trace |
| Vector search | pgvector extension | Reuse PostgreSQL, avoid separate vector service |
| Frontend | React + TypeScript | Incident list, status, trace and report |
| Packaging | Docker Compose | Database and app services; avoid excessive simultaneous containers |
| Tests | JUnit 5, Mockito, Testcontainers | Unit and integration coverage |
| Observability | Micrometer / OpenTelemetry | Request/tool duration, failures, traces (where supported) |

## Hardware constraints

Development laptop: **Intel i7-14700HX, 16 GB RAM, NVIDIA RTX 4050 Laptop GPU (6 GB VRAM)**. Start with one quantized model, around a 4K context, one local inference at a time, and a small number of Java processes. Context and KV cache consume GPU memory beyond downloaded model weight size. Verify with `nvidia-smi` and `ollama ps`.

**Avoid in v1:** Elasticsearch, Kafka, several Java microservices, multiple simultaneously loaded chat models. These add more overhead than the learning goals require.

## Provider selection strategy

1. **Ollama** is the default, including offline demonstrations. Benchmark answer quality and structured response success.
2. **GroqCloud** is optional; only use if the currently available free account quota permits, with rate-limit/error handling.
3. **Gemini** is a possible alternative cloud adapter, subject to current India availability and account/model-specific free-tier terms.
4. **Grok (xAI)** is distinct from **Groq**; don't assume chat-product free access includes free API access.
5. OpenAI / Anthropic paid APIs are not project prerequisites.

Do not claim any named cloud plan is permanently free. Do not commit provider keys.

## Provider interface idea (conceptual)

The orchestrator depends on an application-level model gateway, not a vendor SDK directly. It receives typed prompts/context/tool declarations and returns validated structured responses plus usage and error metadata. Actual interfaces are developer decisions.

## Practical setup checkpoints

- [ ] Confirm Java 21 and build tool.
- [ ] Select compatible Spring Boot/Spring AI release combination.
- [ ] Install Ollama and test `qwen3:4b` on GPU.
- [ ] Measure latency, context behavior and RAM/VRAM for a representative incident.
- [ ] Benchmark local embeddings before adding pgvector retrieval.
- [ ] Verify Docker/PostgreSQL fit alongside IDE + Ollama.

Useful references: [Spring AI](https://docs.spring.io/spring-ai/reference/), [Ollama](https://ollama.com/), [pgvector](https://github.com/pgvector/pgvector).
