create table reward_assets (
    id varchar(120) primary key,
    file_name varchar(180) not null,
    content_type varchar(80) not null,
    data bytea not null,
    byte_size integer not null check (byte_size > 0),
    checksum_sha256 varchar(64) not null,
    source varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

alter table reward_catalog_items
    add column image_asset_id varchar(120);

alter table reward_catalog_items
    add constraint fk_reward_catalog_items_image_asset
        foreign key (image_asset_id) references reward_assets(id);

create index idx_reward_catalog_items_image_asset on reward_catalog_items(image_asset_id);
