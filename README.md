# AI Engineering Copilot (AEC)

AEC is a Quarkus-based backend and CLI that demonstrates three engineering workflows:

- Pull request review from GitHub PR data
- Multi-source ticket analysis through a normalized `TicketSource` abstraction
- Engineering knowledge retrieval with embeddings and a pluggable repository
- Health and Prometheus-compatible metrics endpoints for runtime visibility

The project is intentionally modular. The application layer owns workflows, integrations map external systems into local models, and AI/storage providers stay replaceable behind interfaces.

## Problem

Engineering teams lose time moving between PR review, ticket grooming, and knowledge lookup. AEC provides a single service surface that can:

- fetch and normalize engineering context
- pass that context to an LLM provider
- return structured output that fits real workflow automation

## Architecture

Detailed design docs:

- [V2 PostgreSQL + Qdrant Architecture](docs/architecture/postgres-qdrant-design.md)
- [V2 Implementation Plan](docs/implementation/v2-implementation-plan.md)

### Layers

- `api`: REST endpoints for `/pr/review`, `/ticket/analyze`, `/knowledge/query`, `/knowledge/ingest`
- `api`: REST endpoints for `/pr/review`, `/ticket/analyze`, `/knowledge/query`, `/knowledge/ingest`, `/knowledge/documents/{id}`, `/knowledge/jobs/{id}`
- `cli`: developer-facing entry points using Picocli
- `application`: orchestration services and provider/source selection
- `domain`: shared models for tickets, PR reviews, and knowledge results
- `infrastructure`: GitHub, JSON, OpenAI-compatible, and repository adapters
- `observability`: health checks and operational status reporting

### Key boundaries

- `TicketSource` is the source abstraction for GitHub, JSON, and future Jira support
- `PullRequestGateway` owns GitHub PR retrieval
- `LlmProvider` isolates prompt/response handling from workflow logic
- `EmbeddingProvider` isolates vector generation
- `KnowledgeRepository` isolates memory vs pgvector persistence

### Failure-aware choices

- Default mode is `mock` AI plus in-memory knowledge so the app is runnable without secrets
- GitHub and OpenAI integrations fail loudly with explicit configuration errors
- pgvector is opt-in via `aec.knowledge.repository=pgvector` to avoid hidden DB coupling in local demos
- Health and metrics are exposed through Quarkus management endpoints for safer local and deployment validation

## Core Features

### 1. PR Review Agent

Input:

- GitHub owner, repo, and PR number

Behavior:

- fetches PR metadata and changed files from GitHub REST API
- normalizes into `PullRequestData`
- returns structured JSON review output

Output shape:

```json
{
  "summary": "string",
  "issues": [
    { "type": "bug | performance | design", "description": "string" }
  ],
  "suggestions": ["string"]
}
```

### 2. Ticket Analysis Agent

Input:

- `json`: local file path
- `github`: `owner/repo#123`
- `jira`: reserved for future implementation

Behavior:

- normalizes all sources into the common `Ticket` model
- sends that model to the configured LLM provider
- returns structured analysis

Output shape:

```json
{
  "summary": "string",
  "risks": ["string"],
  "missing_info": ["string"],
  "dependencies": ["string"]
}
```

### 3. Engineering Knowledge (RAG)

Input:

- knowledge documents for ingest
- free-text query for retrieval

Behavior:

- queues ingest work and returns a document/job identifier
- asynchronously chunks content and generates embeddings
- stores vectors in the configured knowledge repository
- retrieves top matches and asks the LLM provider to synthesize an answer

## API Usage

Prerequisites:

- Java 21
- Gradle 8.x or a checked-in Gradle wrapper

Start the app:

```bash
./gradlew quarkusDev
```

If the wrapper is not checked in yet, use:

```bash
gradle quarkusDev
```

Analyze a local JSON ticket:

```bash
curl -X POST http://localhost:8080/ticket/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "source": "json",
    "identifier": "examples/tickets/sample-ticket.json"
  }'
```

Review a PR:

```bash
curl -X POST http://localhost:8080/pr/review \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "octocat",
    "repo": "Hello-World",
    "number": 1
  }'
```

Ingest and query knowledge:

```bash
curl -X POST http://localhost:8080/knowledge/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Cache Strategy",
    "source": "docs",
    "content": "Use cache entries as derived data, never as the system of record."
  }'

curl http://localhost:8080/knowledge/jobs/<job-id>
curl http://localhost:8080/knowledge/documents/<document-id>

curl -X POST http://localhost:8080/knowledge/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "cache strategy"
  }'
```

Operational endpoints:

```bash
curl http://localhost:8080/q/health
curl http://localhost:8080/q/metrics
```

## CLI Usage

Analyze a ticket from JSON:

```bash
java -jar build/quarkus-app/quarkus-run.jar analyze-ticket \
  --source=json \
  --file=examples/tickets/sample-ticket.json
```

Analyze a GitHub issue:

```bash
java -jar build/quarkus-app/quarkus-run.jar analyze-ticket \
  --source=github \
  --id=owner/repo#123
```

Review a PR:

```bash
java -jar build/quarkus-app/quarkus-run.jar review-pr \
  --owner=owner \
  --repo=repo \
  --number=42
```

## Configuration

Default local mode:

- `aec.ai.provider=mock`
- `aec.embedding.provider=mock`
- `aec.knowledge.repository=memory`
- queued ingestion is enabled even in memory mode
- health endpoint enabled at `/q/health`
- Prometheus metrics enabled at `/q/metrics`

Optional environment variables:

- `GITHUB_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_CHAT_MODEL`
- `OPENAI_EMBEDDING_MODEL`

To enable real OpenAI calls:

```bash
export AEC_AI_PROVIDER=openai
export AEC_EMBEDDING_PROVIDER=openai
export OPENAI_API_KEY=...
```

To enable pgvector:

```bash
export AEC_KNOWLEDGE_REPOSITORY=pgvector
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/aec
export QUARKUS_DATASOURCE_USERNAME=aec
export QUARKUS_DATASOURCE_PASSWORD=aec
psql "$QUARKUS_DATASOURCE_JDBC_URL" -f examples/sql/pgvector-bootstrap.sql
```

To start the planned local infrastructure stack for the V2 architecture:

```bash
docker compose up -d
```

## Design Decisions

- Quarkus with Gradle was chosen for fast REST/CLI delivery in Java with clean DI boundaries.
- The `TicketSource` interface is the main extensibility point because source growth is the most obvious near-term variation.
- AI providers are isolated from orchestration logic so prompts, models, and vendors can change without rewriting endpoint logic.
- Knowledge storage supports an in-memory demo path and a real pgvector path because portfolio projects need a safe default and an upgrade path.

## Trade-offs

- The default mock provider is deterministic and runnable, but not semantically strong. It exists to keep the project demonstrable without external services.
- Jira is intentionally stubbed rather than half-implemented. That keeps the boundary explicit without pretending the integration is production-ready.
- The OpenAI integration uses direct HTTP calls instead of a heavier SDK to keep control and dependencies small.
- The queued knowledge ingest flow is implemented first with in-memory stores so the async behavior is testable before the full Postgres + Qdrant persistence migration lands.
- Logging is intentionally workflow-focused rather than fully structured; production deployments would usually ship JSON logs and request correlation.

## Repository Layout

```text
src/main/java/com/aec
├── api
├── application
├── cli
├── domain
└── infrastructure
examples
├── knowledge
├── sql
└── tickets
```

## Verification Status

- Gradle wrapper is generated and checked in.
- REST tests cover JSON ticket analysis, knowledge ingest/query, and observability endpoints.
- `./gradlew test` passes locally.
- `./gradlew quarkusDev -Dquarkus.http.port=8081` was validated with a live `/ticket/analyze` request.
- queued knowledge ingest, job status, document status, and query were validated live on port `8081`.

## CI

GitHub Actions CI is defined in [ci.yml](.github/workflows/ci.yml).

Current CI behavior:

- validates the Gradle wrapper
- runs on `push`, `pull_request`, and manual dispatch
- uses GitHub-hosted `ubuntu-latest` runners
- sets up Java 21
- runs `./gradlew test`
- runs `./gradlew quarkusBuild`
- uploads test reports and Quarkus build artifacts

Additional GitHub automation:

- Dependabot config in [.github/dependabot.yml](.github/dependabot.yml)
- CodeQL workflow in [.github/workflows/codeql.yml](.github/workflows/codeql.yml)
- CODEOWNERS in [.github/CODEOWNERS](.github/CODEOWNERS)
- PR template in [.github/pull_request_template.md](.github/pull_request_template.md)

## Brain Update Candidate

- Session summary: scaffolded a modular Quarkus MVP for PR review, ticket analysis, and RAG with explicit provider and source boundaries.
- Knowledge candidate: “Default local paths for AI-heavy portfolio services should avoid secrets and external dependencies while preserving the same production interfaces.”
- Learning suggestion: add contract tests around provider selection and source normalization before expanding to Jira or richer PR analysis.
