alter table reward_catalog_items
    add column theme_ids varchar(500);

update reward_catalog_items
set theme_ids = theme_id
where theme_ids is null or theme_ids = '';

alter table reward_catalog_items
    alter column theme_ids set not null;
