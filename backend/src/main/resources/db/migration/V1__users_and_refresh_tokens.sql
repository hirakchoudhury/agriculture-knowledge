-- Phase 2: identity.
--
-- Emails are stored lower-cased by the application so that a unique index is
-- enough to make sign-up case-insensitive, without reaching for the citext
-- extension (which Neon and Railway would both need enabling separately).

create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255),
    name          varchar(120) not null,
    avatar_url    varchar(500),
    role          varchar(20)  not null default 'USER',
    provider      varchar(20)  not null default 'LOCAL',
    provider_id   varchar(255),
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),

    constraint users_role_check     check (role in ('USER', 'ADMIN')),
    constraint users_provider_check check (provider in ('LOCAL', 'GOOGLE')),

    -- An account created with email and password must have a hash. A Google
    -- account legitimately has none, and must never be treated as passwordless
    -- by the login endpoint.
    constraint users_local_needs_password check (provider <> 'LOCAL' or password_hash is not null)
);

-- One Google subject maps to at most one account.
create unique index users_provider_identity_idx
    on users (provider, provider_id)
    where provider_id is not null;

-- Refresh tokens are opaque random strings, never JWTs: they must be revocable.
-- Only the SHA-256 hash is stored, so a database leak does not hand out sessions.
create table refresh_tokens (
    id         bigserial   primary key,
    user_id    bigint      not null references users (id) on delete cascade,
    token_hash char(64)    not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    user_agent varchar(400),
    created_at timestamptz not null default now()
);

create index refresh_tokens_user_idx    on refresh_tokens (user_id);
create index refresh_tokens_expires_idx on refresh_tokens (expires_at);
