create table reward_catalog_items (
    id varchar(80) primary key,
    theme_id varchar(80) not null,
    category varchar(80) not null,
    name varchar(120) not null,
    price integer not null check (price > 0),
    image text not null,
    default_scale numeric(5, 2) not null,
    min_scale numeric(5, 2) not null,
    max_scale numeric(5, 2) not null,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_reward_catalog_items_theme_active on reward_catalog_items(theme_id, active, category, name);
