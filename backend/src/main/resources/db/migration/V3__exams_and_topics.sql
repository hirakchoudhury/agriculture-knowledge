-- Phase 3: how admins organise material.
--
-- Two independent axes. Topics form a subject tree (Agronomy > Soil Science >
-- Soil Fertility). Exams are syllabi that draw on some set of those topics, so a
-- topic can serve several exams without being duplicated.

create table exams (
    id            bigserial primary key,
    name          varchar(160) not null,
    slug          varchar(180) not null unique,
    description   text,
    icon_url      varchar(500),
    display_order integer      not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create index exams_display_order_idx on exams (display_order, name);

create table topics (
    id            bigserial primary key,
    name          varchar(160) not null,
    slug          varchar(180) not null unique,
    parent_id     bigint       references topics (id) on delete cascade,
    description   text,
    display_order integer      not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),

    -- Catches the one-step cycle. Longer cycles are rejected in the service,
    -- which walks the ancestor chain before saving.
    constraint topics_not_own_parent check (parent_id is null or parent_id <> id)
);

create index topics_parent_idx on topics (parent_id, display_order);

create table exam_topics (
    exam_id  bigint not null references exams (id) on delete cascade,
    topic_id bigint not null references topics (id) on delete cascade,
    primary key (exam_id, topic_id)
);

-- The reverse lookup: which exams does this topic appear in.
create index exam_topics_topic_idx on exam_topics (topic_id);
