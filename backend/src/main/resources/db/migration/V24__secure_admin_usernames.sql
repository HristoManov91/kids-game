update app_users
set username = left(trim(username), 45) || '-' || id,
    display_name = case
        when lower(trim(display_name)) = lower(trim(username)) then left(trim(username), 45) || '-' || id
        else display_name
    end
where id in (
    select id
    from (
        select id,
               row_number() over (partition by lower(trim(username)) order by id) as duplicate_rank
        from app_users
    ) ranked_users
    where duplicate_rank > 1
);

update app_users
set role = 'PARENT'
where lower(trim(username)) = lower('христо');

create unique index if not exists idx_app_users_username_normalized_unique
    on app_users (lower(trim(username)));
