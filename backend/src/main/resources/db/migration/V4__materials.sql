-- Phase 4: learning material.
--
-- Articles, videos and (from phase 6) quizzes share one materials row plus a
-- type-specific table. That is JPA's JOINED inheritance: a mixed "latest in Soil
-- Science" feed is a single query, while each type keeps its own columns.

create table materials (
    id            bigserial primary key,
    type          varchar(20)  not null,
    title         varchar(250) not null,
    slug          varchar(280) not null unique,
    summary       varchar(500),
    thumbnail_url varchar(500),
    difficulty    varchar(20)  not null default 'BEGINNER',
    status        varchar(20)  not null default 'DRAFT',
    author_id     bigint       not null references users (id),
    published_at  timestamptz,

    -- Denormalised on purpose. A listing of 20 cards would otherwise fire two
    -- aggregate queries per card; these are incremented in the same transaction
    -- as the like or comment that causes them.
    view_count    bigint       not null default 0,
    like_count    bigint       not null default 0,
    comment_count bigint       not null default 0,

    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),

    constraint materials_type_check       check (type in ('ARTICLE', 'VIDEO', 'QUIZ')),
    constraint materials_status_check     check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    constraint materials_difficulty_check check (difficulty in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),

    -- Something published must know when. The listing sorts on it, and a null
    -- would quietly sort the row out of the feed.
    constraint materials_published_has_date check (status <> 'PUBLISHED' or published_at is not null)
);

create index materials_feed_idx on materials (status, published_at desc);
create index materials_type_idx on materials (type, status);
create index materials_author_idx on materials (author_id);

create table articles (
    material_id     bigint primary key references materials (id) on delete cascade,
    body_html       text    not null,
    reading_minutes integer not null default 1
);

create table videos (
    material_id      bigint primary key references materials (id) on delete cascade,
    -- The bare 11-character id, never a full URL: the same video arrives as
    -- youtu.be links, watch?v= links, embed links and shorts links.
    youtube_id       varchar(20) not null,
    duration_seconds integer
);

-- Two independent tagging axes, mirroring exams and topics themselves.
create table material_topics (
    material_id bigint not null references materials (id) on delete cascade,
    topic_id    bigint not null references topics (id) on delete cascade,
    primary key (material_id, topic_id)
);

create index material_topics_topic_idx on material_topics (topic_id, material_id);

create table material_exams (
    material_id bigint not null references materials (id) on delete cascade,
    exam_id     bigint not null references exams (id) on delete cascade,
    primary key (material_id, exam_id)
);

create index material_exams_exam_idx on material_exams (exam_id, material_id);
