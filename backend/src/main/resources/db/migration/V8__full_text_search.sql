-- Phase 8: real search.
--
-- Until now, searching was ILIKE against title and summary. That cannot match
-- "fertiliser" against "fertilisers", ranks nothing, and scans every row. A
-- generated tsvector with a GIN index gives stemming, ranking and an index.
--
-- The column is GENERATED ALWAYS, so it can never drift from the text it
-- summarises: Postgres recomputes it on every insert and update. That is only
-- possible because to_tsvector with an explicit configuration is immutable.
alter table materials
    add column search_vector tsvector
    generated always as (
        -- Title matches count for more than summary matches.
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(summary, '')), 'B')
    ) stored;

create index materials_search_idx on materials using gin (search_vector);
