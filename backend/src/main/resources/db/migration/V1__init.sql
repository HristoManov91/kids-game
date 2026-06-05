create table app_users (
    id bigserial primary key,
    username varchar(60) not null unique,
    display_name varchar(120) not null,
    password_hash varchar(255) not null,
    role varchar(30) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);

create table quiz_attempts (
    id uuid primary key,
    user_id bigint not null references app_users(id),
    category varchar(40) not null,
    mode varchar(60) not null,
    difficulty integer not null,
    status varchar(30) not null,
    questions_json text not null,
    answers_json text not null default '[]',
    score integer,
    total_questions integer not null,
    grade numeric(4, 2),
    medal varchar(60),
    started_at timestamptz not null,
    completed_at timestamptz
);

create index idx_quiz_attempts_user_started on quiz_attempts(user_id, started_at desc);
create index idx_quiz_attempts_leaderboard on quiz_attempts(category, mode, difficulty, status, score desc, completed_at asc);
