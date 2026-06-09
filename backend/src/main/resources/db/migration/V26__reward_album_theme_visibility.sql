create table reward_album_themes (
    theme_id varchar(80) primary key,
    active boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

insert into reward_album_themes (theme_id, active)
values
    ('forest-meadow', true),
    ('farm', true),
    ('space', false),
    ('underwater', false),
    ('jungle', false);
