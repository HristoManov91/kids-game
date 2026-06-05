create table reward_album_pictures (
    id uuid primary key,
    user_id bigint not null references app_users(id) on delete cascade,
    name varchar(40) not null,
    theme_id varchar(80) not null,
    placed_items_json text not null default '[]',
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_reward_album_pictures_user_updated on reward_album_pictures(user_id, updated_at desc);

create table reward_crystal_spends (
    id bigserial primary key,
    user_id bigint not null references app_users(id) on delete cascade,
    picture_id uuid,
    item_id varchar(80) not null,
    item_name varchar(120) not null,
    amount integer not null check (amount > 0),
    created_at timestamptz not null
);

create index idx_reward_crystal_spends_user_created on reward_crystal_spends(user_id, created_at desc);
