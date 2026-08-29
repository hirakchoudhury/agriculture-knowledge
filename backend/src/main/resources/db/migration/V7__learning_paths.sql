-- Phase 7: learner-owned learning paths, and progress through material.

create table learning_paths (
    id          bigserial     primary key,
    user_id     bigint        not null references users (id) on delete cascade,
    title       varchar(200)  not null,
    description varchar(1000),

    -- Reserves the sharing feature without building it. Everything is private
    -- for now, and the read endpoints check ownership regardless.
    is_public   boolean       not null default false,

    created_at  timestamptz   not null default now(),
    updated_at  timestamptz   not null default now()
);

create index learning_paths_user_idx on learning_paths (user_id, created_at desc);

create table learning_path_items (
    id            bigserial primary key,
    path_id       bigint    not null references learning_paths (id) on delete cascade,
    material_id   bigint    not null references materials (id) on delete cascade,
    display_order integer   not null default 0,
    note          varchar(500),

    -- The same material twice in one path is almost always a mistake, and the
    -- progress view would show it as two independent steps.
    constraint learning_path_items_once unique (path_id, material_id)
);

create index learning_path_items_path_idx on learning_path_items (path_id, display_order);

-- Progress belongs to the learner and the material, not to a path. The same
-- article read inside one path counts as read everywhere it appears.
create table material_progress (
    id                    bigserial   primary key,
    user_id               bigint      not null references users (id) on delete cascade,
    material_id           bigint      not null references materials (id) on delete cascade,
    status                varchar(20) not null default 'IN_PROGRESS',

    -- Where to resume a video. Meaningless for other types, hence nullable.
    last_position_seconds integer,

    completed_at          timestamptz,
    updated_at            timestamptz not null default now(),

    -- A surrogate key with a unique constraint rather than a composite primary
    -- key: the guarantee is identical -- one progress row per learner per
    -- material -- and a single-column id keeps the JPA mapping straightforward.
    constraint material_progress_once unique (user_id, material_id),

    constraint material_progress_status_check check (status in ('IN_PROGRESS', 'COMPLETED')),
    constraint material_progress_completed_has_date
        check (status <> 'COMPLETED' or completed_at is not null)
);

create index material_progress_material_idx on material_progress (material_id);
