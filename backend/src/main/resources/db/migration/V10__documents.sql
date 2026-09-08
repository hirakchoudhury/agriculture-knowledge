-- Phase 9: PDF documents, for previous year papers.
--
-- A fourth material type alongside articles, videos and quizzes, so a paper is
-- tagged by exam and topic and appears in the same feed and search as everything
-- else. Only the bytes live elsewhere.
--
-- The file itself is NOT stored here. Neon's free tier is half a gigabyte for the
-- whole database, and a bytea column would spend it on a handful of papers while
-- making every backup proportionally slower. The table holds the object key and
-- enough metadata to render a card without fetching the file.

-- V4 wrote this constraint with the three types that existed then. Postgres has
-- no "add value to check constraint", so it is dropped and rewritten.
alter table materials drop constraint materials_type_check;
alter table materials add constraint materials_type_check
    check (type in ('ARTICLE', 'VIDEO', 'QUIZ', 'DOCUMENT'));

create table documents (
    material_id   bigint primary key references materials (id) on delete cascade,

    -- Opaque key within the bucket, e.g. "papers/2026/icar-jrf-2024-a1b2c3.pdf".
    -- Never shown to anyone: downloads go through a short-lived signed URL.
    storage_key   varchar(500) not null unique,

    -- What the file was called when it was uploaded. Used for the download
    -- filename, so a student gets "ICAR JRF 2024.pdf" rather than the key.
    original_name varchar(255) not null,

    content_type  varchar(100) not null default 'application/pdf',
    size_bytes    bigint       not null,
    page_count    integer,

    -- The year the paper is from, which is the axis people actually browse
    -- previous year papers by. Null for anything that is not year-specific.
    paper_year    integer,

    constraint documents_size_positive check (size_bytes > 0),
    constraint documents_year_sane     check (paper_year is null or paper_year between 1900 and 2200)
);

-- Papers for one exam, newest first: the query the exam page will run.
create index documents_year_idx on documents (paper_year desc);
