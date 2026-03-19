create extension if not exists vector;

create table if not exists ticket (
    id uuid primary key,
    source_type text not null,
    source_ref text not null,
    title text not null,
    description text,
    normalized_status text,
    raw_payload jsonb,
    fetched_at timestamptz not null,
    created_at timestamptz not null default now(),
    unique (source_type, source_ref)
);

create table if not exists ticket_comment (
    id uuid primary key,
    ticket_id uuid not null references ticket(id) on delete cascade,
    author text,
    body text not null,
    source_comment_ref text,
    created_at timestamptz
);

create table if not exists ticket_analysis (
    id uuid primary key,
    ticket_id uuid not null references ticket(id),
    provider text not null,
    model text not null,
    summary text not null,
    risks jsonb not null,
    missing_info jsonb not null,
    dependencies jsonb not null,
    status text not null,
    created_at timestamptz not null default now()
);

create table if not exists pull_request (
    id uuid primary key,
    source_type text not null default 'github',
    source_ref text not null,
    repo_owner text not null,
    repo_name text not null,
    pr_number integer not null,
    title text not null,
    description text,
    author text,
    base_branch text,
    head_branch text,
    raw_payload jsonb,
    fetched_at timestamptz not null,
    created_at timestamptz not null default now(),
    unique (repo_owner, repo_name, pr_number)
);

create table if not exists pull_request_file (
    id uuid primary key,
    pull_request_id uuid not null references pull_request(id) on delete cascade,
    path text not null,
    status text not null,
    additions integer not null,
    deletions integer not null,
    patch_text text
);

create table if not exists pr_review (
    id uuid primary key,
    pull_request_id uuid not null references pull_request(id),
    provider text not null,
    model text not null,
    summary text not null,
    issues jsonb not null,
    suggestions jsonb not null,
    status text not null,
    created_at timestamptz not null default now()
);

create table if not exists knowledge_document (
    id uuid primary key,
    source_type text not null,
    source_ref text,
    title text not null,
    content text not null,
    content_hash text not null,
    metadata jsonb not null default '{}'::jsonb,
    ingestion_status text not null,
    latest_job_id uuid,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (source_type, source_ref, content_hash)
);

create table if not exists knowledge_chunk (
    id uuid primary key,
    document_id uuid not null references knowledge_document(id) on delete cascade,
    chunk_index integer not null,
    chunk_text text not null,
    token_count integer,
    embedding_model text,
    embedding_status text not null,
    qdrant_point_id text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (document_id, chunk_index)
);

create table if not exists ingestion_job (
    id uuid primary key,
    job_type text not null,
    target_type text not null,
    target_id uuid not null,
    status text not null,
    priority integer not null default 100,
    payload jsonb not null,
    scheduled_at timestamptz not null default now(),
    leased_until timestamptz,
    leased_by text,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    last_error text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_ingestion_job_status_schedule
    on ingestion_job (status, scheduled_at, priority);

create index if not exists idx_ingestion_job_target
    on ingestion_job (target_type, target_id);

create table if not exists ingestion_job_attempt (
    id uuid primary key,
    job_id uuid not null references ingestion_job(id) on delete cascade,
    attempt_number integer not null,
    started_at timestamptz not null,
    finished_at timestamptz,
    outcome text not null,
    error_message text
);
