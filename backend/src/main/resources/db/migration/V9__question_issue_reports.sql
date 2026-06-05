create table question_issue_reports (
    id bigserial primary key,
    attempt_id uuid not null references quiz_attempts(id) on delete cascade,
    user_id bigint not null references app_users(id),
    question_id integer not null,
    message varchar(1000) not null,
    status varchar(30) not null,
    admin_note varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_question_issue_reports_status_created
    on question_issue_reports(status, created_at desc);

create index idx_question_issue_reports_attempt
    on question_issue_reports(attempt_id, question_id);
