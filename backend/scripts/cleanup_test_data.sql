-- Removes the rows the smoke scripts leave behind.
--
-- Run this yourself, in the Neon SQL editor or any psql session. It is not wired
-- into the application on purpose: there is no hard-delete endpoint anywhere in
-- this codebase, and adding one so that tests could tidy up would be a poor trade.
--
-- READ THIS BEFORE RUNNING. It deletes accounts and material. The patterns below
-- only match what the scripts in this folder create:
--
--   * accounts whose email starts with a known test prefix
--   * material whose title ends in a 10-digit unix timestamp
--
-- Run the SELECTs first and check the counts look like test data, not your work.

-- ---------------------------------------------------------------------------
-- 1. What would go. Run these first.
-- ---------------------------------------------------------------------------

select count(*) as test_accounts
from users
where email ~ '^(smoke|learner|reader|alice|bob|carol|quizzer|nosy|planner|stranger)\+[0-9]+@example\.com$'
   or email = 'browsertest-phase2@example.com';

-- URGENT if you have deployed. browsertest-phase2@example.com was promoted to
-- ADMIN during development and its password was typed in plain text into a chat
-- transcript. Production shares this database, so that account is an admin on the
-- live site. This should go before anything else.
select email, role from users where email = 'browsertest-phase2@example.com';

select count(*) as test_material
from materials
where title ~ ' 1[0-9]{9}$';

-- ---------------------------------------------------------------------------
-- 2. The deletions. Everything below cascades from these two tables, so comments,
--    likes, attempts, progress and path items belonging to test rows go with them.
--    Foreign keys are declared ON DELETE CASCADE throughout.
-- ---------------------------------------------------------------------------

begin;

-- FIRST: hand over anything the development admin authored.
--
-- materials.author_id has no ON DELETE rule, so deleting that account while it
-- still owns published material would fail on the foreign key. The two starter
-- articles were written under it, and they should survive. Run this only after
-- you have signed up, so there is an account to hand them to.
update materials
set author_id = (select id from users where email = 'hhirakk18@gmail.com')
where author_id = (select id from users where email = 'browsertest-phase2@example.com')
  and exists (select 1 from users where email = 'hhirakk18@gmail.com');


delete from materials
where title ~ ' 1[0-9]{9}$';

delete from users
where email ~ '^(smoke|learner|reader|alice|bob|carol|quizzer|nosy|planner|stranger)\+[0-9]+@example\.com$'
   or email = 'browsertest-phase2@example.com';

-- Topics and exams created by catalog_smoke.py carry the same timestamp suffix.
delete from topics where name ~ ' 1[0-9]{9}$';
delete from exams  where name ~ ' 1[0-9]{9}$';

-- Check the row counts reported above look right, then:
commit;
-- or, if anything looks wrong:
-- rollback;

-- ---------------------------------------------------------------------------
-- 3. Denormalised counters
--
-- like_count and comment_count on materials are maintained incrementally, so
-- after a bulk delete they can be out of step with reality. This puts them back.
-- ---------------------------------------------------------------------------

update materials m
set like_count = (select count(*) from material_likes l where l.material_id = m.id),
    comment_count = (select count(*) from comments c
                     where c.material_id = m.id and c.is_deleted = false);
