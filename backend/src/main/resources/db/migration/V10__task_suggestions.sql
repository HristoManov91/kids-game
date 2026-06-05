create table task_suggestions (
    id bigserial primary key,
    user_id bigint not null references app_users(id),
    category varchar(30) not null,
    mode varchar(60),
    difficulty integer not null,
    message varchar(1500) not null,
    status varchar(30) not null,
    admin_note varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_task_suggestions_status_created
    on task_suggestions(status, created_at desc);

create index idx_task_suggestions_category_mode
    on task_suggestions(category, mode, difficulty);
