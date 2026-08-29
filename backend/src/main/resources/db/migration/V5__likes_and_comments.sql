-- Phase 5: engagement.

-- A surrogate key with a unique constraint rather than a composite primary key.
-- The uniqueness guarantee is identical -- the database, not the application,
-- makes double-liking impossible -- but a single-column id keeps the JPA mapping
-- straightforward, and this table is written far more often than it is joined.
create table material_likes (
    id          bigserial   primary key,
    user_id     bigint      not null references users (id) on delete cascade,
    material_id bigint      not null references materials (id) on delete cascade,
    created_at  timestamptz not null default now(),

    constraint material_likes_once unique (user_id, material_id)
);

-- The reverse lookup: who liked this material.
create index material_likes_material_idx on material_likes (material_id);

create table comments (
    id          bigserial     primary key,
    material_id bigint        not null references materials (id) on delete cascade,
    user_id     bigint        not null references users (id) on delete cascade,

    -- One level of replies only. The service refuses a parent that is itself a
    -- reply, so threads cannot nest without bound.
    parent_id   bigint        references comments (id) on delete cascade,

    body        varchar(4000) not null,

    -- Soft delete. Removing the row would take its replies with it through the
    -- cascade, so a deleted parent would silently erase the conversation under it.
    is_deleted  boolean       not null default false,

    created_at  timestamptz   not null default now(),
    edited_at   timestamptz,

    constraint comments_not_own_parent check (parent_id is null or parent_id <> id)
);

create index comments_material_idx on comments (material_id, created_at desc);
create index comments_parent_idx on comments (parent_id, created_at);
