-- V1 declared token_hash as char(64). Two problems with that:
--
--   1. Postgres pads char values with trailing spaces to the declared width, so an
--      equality lookup against an unpadded 64-character hash can miss.
--   2. char maps to bpchar, which does not match the varchar that JPA expects for a
--      String field, so Hibernate's schema validation refuses to start.
--
-- V1 is left untouched: it has already run, and migrations are append-only.
alter table refresh_tokens
    alter column token_hash type varchar(64);
