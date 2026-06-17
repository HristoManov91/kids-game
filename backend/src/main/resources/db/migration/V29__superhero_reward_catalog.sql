insert into reward_album_themes (theme_id, active)
values ('superheroes', true)
on conflict (theme_id) do update
set active = true,
    updated_at = now();

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
    ('super-captain-america', 'superheroes', 'superheroes', 'Герои', 'Капитан Америка', 120, '🛡️', 1.05, 0.50, 2.25, true, now(), now()),
    ('super-iron-man', 'superheroes', 'superheroes', 'Герои', 'Железният човек', 130, '🤖', 1.05, 0.50, 2.25, true, now(), now()),
    ('super-thor', 'superheroes', 'superheroes', 'Герои', 'Тор', 125, '⚡', 1.05, 0.50, 2.25, true, now(), now()),
    ('super-hulk', 'superheroes', 'superheroes', 'Герои', 'Хълк', 125, '💚', 1.10, 0.55, 2.35, true, now(), now()),
    ('super-spider-hero', 'superheroes', 'superheroes', 'Герои', 'Човекът паяк', 115, '🕸️', 1.00, 0.45, 2.15, true, now(), now()),
    ('super-doctor-strange', 'superheroes', 'superheroes', 'Герои', 'Доктор Стрейндж', 120, '✨', 1.00, 0.45, 2.15, true, now(), now()),
    ('super-black-widow', 'superheroes', 'superheroes', 'Герои', 'Черната вдовица', 105, '🕶️', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-hawkeye', 'superheroes', 'superheroes', 'Герои', 'Ястребово око', 105, '🏹', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-wolverine', 'superheroes', 'superheroes', 'Герои', 'Върколакът', 110, '⚔️', 1.00, 0.45, 2.15, true, now(), now()),
    ('super-star-hero', 'superheroes', 'superheroes', 'Герои', 'Звезден герой', 90, '🌟', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-star-shield', 'superheroes', 'superheroes', 'Екипировка', 'Звезден щит', 70, '🛡️', 0.90, 0.40, 2.00, true, now(), now()),
    ('super-arc-core', 'superheroes', 'superheroes', 'Екипировка', 'Светещ реактор', 75, '🔵', 0.85, 0.40, 1.90, true, now(), now()),
    ('super-hammer', 'superheroes', 'superheroes', 'Екипировка', 'Гръмотевичен чук', 80, '🔨', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-power-fist', 'superheroes', 'superheroes', 'Сили', 'Супер юмрук', 65, '✊', 0.90, 0.40, 2.00, true, now(), now()),
    ('super-web', 'superheroes', 'superheroes', 'Сили', 'Паяжина', 45, '🕸️', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-magic-portal', 'superheroes', 'superheroes', 'Сили', 'Магически портал', 85, '🌀', 1.05, 0.50, 2.25, true, now(), now()),
    ('super-cape', 'superheroes', 'superheroes', 'Екипировка', 'Геройско наметало', 55, '🦸', 0.95, 0.45, 2.05, true, now(), now()),
    ('super-mask', 'superheroes', 'superheroes', 'Екипировка', 'Супер маска', 40, '🎭', 0.85, 0.40, 1.90, true, now(), now()),
    ('super-city', 'superheroes', 'superheroes', 'Град', 'Градски силует', 90, '🏙️', 1.25, 0.60, 2.70, true, now(), now()),
    ('super-headquarters', 'superheroes', 'superheroes', 'Град', 'База на героите', 110, '🏢', 1.20, 0.55, 2.55, true, now(), now()),
    ('super-energy-crystal', 'superheroes', 'superheroes', 'Украси', 'Енергиен кристал', 35, '💎', 0.70, 0.35, 1.60, true, now(), now()),
    ('super-comic-burst', 'superheroes', 'superheroes', 'Украси', 'Комикс взрив', 30, '💥', 0.80, 0.38, 1.75, true, now(), now())
on conflict (id) do update
set theme_id = excluded.theme_id,
    theme_ids = excluded.theme_ids,
    category = excluded.category,
    name = excluded.name,
    price = excluded.price,
    image = excluded.image,
    default_scale = excluded.default_scale,
    min_scale = excluded.min_scale,
    max_scale = excluded.max_scale,
    active = true,
    updated_at = now();
