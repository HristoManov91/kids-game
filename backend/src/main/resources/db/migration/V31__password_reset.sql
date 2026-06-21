alter table app_users add column email varchar(254);
create unique index uq_app_users_email_lower on app_users (lower(email)) where email is not null;

create table password_reset_tokens (
    token_hash varchar(64) primary key,
    user_id bigint not null references app_users(id) on delete cascade,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_password_reset_tokens_user on password_reset_tokens(user_id);
create index idx_password_reset_tokens_expires on password_reset_tokens(expires_at);
