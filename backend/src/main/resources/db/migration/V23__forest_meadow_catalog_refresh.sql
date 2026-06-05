update reward_catalog_items
set active = false,
    updated_at = now()
where id in (
    'forest-flower',
    'forest-tulip',
    'forest-gazebo',
    'forest-family',
    'forest-volleyball',
    'forest-mandarin'
);

update reward_catalog_items
set name = 'Франзела',
    updated_at = now()
where id = 'forest-breadsticks';

update reward_catalog_items
set name = 'Пейка',
    image = '/reward-assets/polished/forest/forest-bench.png',
    image_asset_id = null,
    updated_at = now()
where id = 'forest-bench';

update reward_catalog_items
set image = '/reward-assets/polished/forest/forest-rocks.png',
    image_asset_id = null,
    updated_at = now()
where id = 'forest-rock';

insert into reward_catalog_items (
    id,
    theme_id,
    theme_ids,
    category,
    name,
    price,
    image,
    default_scale,
    min_scale,
    max_scale,
    active,
    created_at,
    updated_at
) values
    ('forest-swing', 'forest-meadow', 'forest-meadow', 'Предмети', 'Люлка', 45, '/reward-assets/polished/forest/forest-swing.png', 1.05, 0.50, 2.20, true, now(), now()),
    ('forest-pond', 'forest-meadow', 'forest-meadow', 'Природа', 'Езерце', 35, '/reward-assets/polished/forest/forest-pond.png', 1.20, 0.55, 2.60, true, now(), now()),
    ('forest-bear', 'forest-meadow', 'forest-meadow', 'Животни', 'Мечка', 95, '/reward-assets/polished/forest/bear.png', 1.00, 0.50, 2.10, true, now(), now()),
    ('forest-fox', 'forest-meadow', 'forest-meadow', 'Животни', 'Лисица', 70, '/reward-assets/polished/forest/fox.png', 0.90, 0.45, 1.90, true, now(), now()),
    ('forest-wolf', 'forest-meadow', 'forest-meadow', 'Животни', 'Вълк', 85, '/reward-assets/polished/forest/wolf.png', 1.00, 0.50, 2.10, true, now(), now()),
    ('forest-hedgehog', 'forest-meadow', 'forest-meadow', 'Животни', 'Таралеж', 45, '/reward-assets/polished/forest/hedgehog.png', 0.72, 0.35, 1.55, true, now(), now()),
    ('forest-horse', 'forest-meadow', 'forest-meadow', 'Животни', 'Кон', 80, '/reward-assets/polished/forest/horse.png', 1.05, 0.50, 2.25, true, now(), now()),
    ('forest-donkey', 'forest-meadow', 'forest-meadow', 'Животни', 'Магаре', 70, '/reward-assets/polished/forest/donkey.png', 0.98, 0.48, 2.05, true, now(), now())
on conflict (id) do update
set theme_id = excluded.theme_id,
    theme_ids = excluded.theme_ids,
    category = excluded.category,
    name = excluded.name,
    price = excluded.price,
    image = excluded.image,
    image_asset_id = null,
    default_scale = excluded.default_scale,
    min_scale = excluded.min_scale,
    max_scale = excluded.max_scale,
    active = true,
    updated_at = now();
