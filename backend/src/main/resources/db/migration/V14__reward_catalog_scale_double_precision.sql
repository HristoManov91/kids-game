alter table reward_catalog_items
    alter column default_scale type double precision using default_scale::double precision,
    alter column min_scale type double precision using min_scale::double precision,
    alter column max_scale type double precision using max_scale::double precision;
