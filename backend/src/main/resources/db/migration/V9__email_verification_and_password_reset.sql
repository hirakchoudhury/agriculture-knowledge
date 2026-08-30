-- Phase 9: prove people own the address they signed up with, and let them get
-- back in when they forget the password.

alter table users
    add column email_verified boolean not null default false;

-- Everyone who already has an account predates verification. Marking them
-- verified is the only humane option: the alternative locks people out of
-- accounts they are already using, for a rule that did not exist when they
-- signed up.
update users set email_verified = true;

-- One table for both purposes rather than two nearly identical ones. The purpose
-- column keeps them from being used interchangeably: a reset code cannot verify
-- an address and a verification code cannot change a password.
create table verification_codes (
    id          bigserial   primary key,
    user_id     bigint      not null references users (id) on delete cascade,
    purpose     varchar(30) not null,

    -- SHA-256 of the code, never the code itself. A six-digit code is weak enough
    -- on its own; a leaked table should not also hand over live ones.
    -- varchar, not char: char pads with spaces and breaks equality lookups, which
    -- is exactly the bug V2 had to correct on refresh_tokens.
    code_hash   varchar(64) not null,

    expires_at  timestamptz not null,
    consumed_at timestamptz,

    -- Six digits is a million possibilities, which is brute-forceable in seconds
    -- without a ceiling on guesses.
    attempts    integer     not null default 0,

    created_at  timestamptz not null default now(),

    constraint verification_codes_purpose_check
        check (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

-- The lookup every verification does: newest live code for this person and purpose.
create index verification_codes_lookup_idx
    on verification_codes (user_id, purpose, created_at desc);

-- Lets expired rows be swept without scanning the table.
create index verification_codes_expiry_idx on verification_codes (expires_at);
