create extension if not exists vector;

create table if not exists knowledge_document (
    id uuid primary key,
    title text not null,
    source text not null,
    content text not null,
    embedding vector(1536) not null
);
