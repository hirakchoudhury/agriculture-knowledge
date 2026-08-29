-- Phase 6: multiple-choice quizzes.
--
-- A quiz is a third kind of material, so it joins the existing hierarchy through
-- materials.id exactly as articles and videos do. That means quizzes appear in the
-- same feed, carry the same tags, and use the same draft/publish workflow for free.

create table quizzes (
    material_id        bigint primary key references materials (id) on delete cascade,

    -- Null means untimed. Stored in seconds so the client can render whatever
    -- units suit it.
    time_limit_seconds integer,

    pass_percentage    integer not null default 60,
    shuffle_questions  boolean not null default false,

    constraint quizzes_pass_percentage_range check (pass_percentage between 0 and 100),
    constraint quizzes_time_limit_positive   check (time_limit_seconds is null or time_limit_seconds > 0)
);

create table questions (
    id             bigserial     primary key,
    quiz_id        bigint        not null references quizzes (material_id) on delete cascade,
    text           varchar(2000) not null,

    -- Shown only after submission.
    explanation    varchar(2000),
    image_url      varchar(500),

    -- Decimal because negative marking in Indian competitive exams is routinely
    -- fractional: a quarter mark off for a wrong answer is the usual convention.
    marks          numeric(5, 2) not null default 1.00,
    negative_marks numeric(5, 2) not null default 0.00,

    display_order  integer       not null default 0,

    constraint questions_marks_positive  check (marks > 0),
    constraint questions_negative_marks_non_negative check (negative_marks >= 0)
);

create index questions_quiz_idx on questions (quiz_id, display_order);

create table question_options (
    id            bigserial     primary key,
    question_id   bigint        not null references questions (id) on delete cascade,
    text          varchar(1000) not null,
    is_correct    boolean       not null default false,
    display_order integer       not null default 0
);

create index question_options_question_idx on question_options (question_id, display_order);

create table quiz_attempts (
    id           bigserial   primary key,
    user_id      bigint      not null references users (id) on delete cascade,
    quiz_id      bigint      not null references quizzes (material_id) on delete cascade,
    started_at   timestamptz not null default now(),
    submitted_at timestamptz,

    -- Null until submitted. Both are stored rather than recomputed, so a later
    -- edit to the quiz cannot silently rewrite someone's past result.
    score        numeric(7, 2),
    total_marks  numeric(7, 2),

    constraint quiz_attempts_scored_when_submitted
        check (submitted_at is null or (score is not null and total_marks is not null))
);

create index quiz_attempts_user_idx on quiz_attempts (user_id, quiz_id, started_at desc);

create table quiz_answers (
    id                 bigserial primary key,
    attempt_id         bigint    not null references quiz_attempts (id) on delete cascade,
    question_id        bigint    not null references questions (id) on delete cascade,

    -- Null means the question was left unanswered, which scores zero rather than
    -- attracting a negative mark.
    selected_option_id bigint    references question_options (id) on delete set null,

    is_correct         boolean   not null default false,

    -- One answer per question per attempt.
    constraint quiz_answers_once unique (attempt_id, question_id)
);

create index quiz_answers_attempt_idx on quiz_answers (attempt_id);
