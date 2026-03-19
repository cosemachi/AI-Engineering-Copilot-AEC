# AEC V2 Implementation Plan

## Objective

Implement the V2 architecture defined in:

- [PostgreSQL + Qdrant Design](../architecture/postgres-qdrant-design.md)

## Milestone 1: Persistence Foundation

Goal:

- introduce canonical PostgreSQL persistence

Tasks:

- add Flyway dependency and baseline migration setup
- create schema for tickets, PRs, analyses, documents, chunks, jobs
- add local Docker Compose for Postgres and Qdrant
- add repository interfaces and Postgres implementations
- persist PR snapshots and ticket snapshots on fetch

Exit criteria:

- app boots against Postgres
- Flyway migrations apply cleanly
- PR review and ticket analysis write durable rows

## Milestone 2: Async Knowledge Ingestion

Goal:

- replace synchronous in-memory ingest with queued ingest

Tasks:

- create `ingestion_job` and `ingestion_job_attempt` flows
- change `/knowledge/ingest` to return `202 Accepted`
- write document metadata and enqueue ingestion job in one transaction
- build worker polling loop with `SKIP LOCKED`
- add retry and lease-expiry handling

Exit criteria:

- document ingest returns queued status
- worker processes pending jobs
- failed jobs retry with backoff

## Milestone 3: Qdrant Integration

Goal:

- move retrieval to vector-native storage

Tasks:

- add Qdrant client integration
- create `knowledge_chunks` collection bootstrap
- chunk documents and embed chunks
- upsert chunk vectors with stable IDs
- query Qdrant during `/knowledge/query`

Exit criteria:

- ingested documents become searchable through Qdrant
- query flow returns retrieved sources backed by Qdrant results

## Milestone 4: Operational Hardening

Goal:

- make the async workflow operable and debuggable

Tasks:

- add job metrics
- add Qdrant readiness check
- add structured workflow logging with identifiers
- add dead-letter handling
- add document and job status endpoints

Exit criteria:

- failures are visible
- stuck jobs are diagnosable
- health reflects real dependency readiness

## Milestone 5: Retrieval Quality Improvements

Goal:

- improve answer quality and indexing fidelity

Tasks:

- introduce configurable chunking strategy
- add metadata filtering
- add document versioning / reindex semantics
- add retrieval evaluation fixtures
- add richer source citations in query responses

Exit criteria:

- retrieval is filterable, explainable, and version-safe

## Suggested Build Order Inside The Codebase

1. `infra-postgres`
2. `review` and `ticket` persistence
3. `knowledge` document persistence
4. `jobs` worker and queue leasing
5. `infra-qdrant`
6. queued knowledge API changes
7. operational endpoints and metrics extensions

## Risks To Watch

- adding too much abstraction before the queue and persistence paths are proven
- letting Qdrant payload shape become the domain model
- losing idempotency in worker retries
- making local dev too heavy before the main flows work

## Working Rules

- PostgreSQL remains canonical
- Qdrant remains derived
- queue writes happen transactionally with canonical records
- every async workflow must be retryable and observable
