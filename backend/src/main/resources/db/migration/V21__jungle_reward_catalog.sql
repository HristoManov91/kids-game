delete from reward_crystal_spends
where picture_id in (
    select id
    from reward_album_pictures
    where theme_id in ('castle', 'city')
);

delete from reward_album_pictures
where theme_id in ('castle', 'city');

delete from reward_catalog_items
where theme_id in ('castle', 'city')
   or theme_ids ~ '(^|,)(castle|city)(,|$)';

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
    ('jungle-palm-tree', 'jungle', 'jungle', 'Растения', 'Палма', 45, '/reward-assets/jungle/palm-tree.svg', 1.10, 0.55, 2.35, true, now(), now()),
    ('jungle-banana-tree', 'jungle', 'jungle', 'Растения', 'Бананово дърво', 55, '/reward-assets/jungle/banana-tree.svg', 1.10, 0.55, 2.35, true, now(), now()),
    ('jungle-giant-leaf', 'jungle', 'jungle', 'Растения', 'Голямо листо', 20, '/reward-assets/jungle/giant-leaf.svg', 0.90, 0.45, 2.00, true, now(), now()),
    ('jungle-fern', 'jungle', 'jungle', 'Растения', 'Папрат', 25, '/reward-assets/jungle/fern.svg', 0.95, 0.45, 2.05, true, now(), now()),
    ('jungle-liana', 'jungle', 'jungle', 'Растения', 'Лиана', 30, '/reward-assets/jungle/liana.svg', 1.05, 0.50, 2.20, true, now(), now()),
    ('jungle-red-flower', 'jungle', 'jungle', 'Растения', 'Червено цвете', 18, '/reward-assets/jungle/jungle-flower-red.svg', 0.80, 0.38, 1.70, true, now(), now()),
    ('jungle-purple-flower', 'jungle', 'jungle', 'Растения', 'Лилаво цвете', 18, '/reward-assets/jungle/jungle-flower-purple.svg', 0.80, 0.38, 1.70, true, now(), now()),
    ('jungle-waterfall', 'jungle', 'jungle', 'Вода', 'Водопад', 90, '/reward-assets/jungle/waterfall.svg', 1.25, 0.60, 2.55, true, now(), now()),
    ('jungle-rock', 'jungle', 'jungle', 'Природа', 'Камък', 18, '/reward-assets/jungle/jungle-rock.svg', 0.85, 0.40, 1.80, true, now(), now()),
    ('jungle-log', 'jungle', 'jungle', 'Природа', 'Паднало дърво', 28, '/reward-assets/jungle/jungle-log.svg', 1.00, 0.48, 2.10, true, now(), now()),
    ('jungle-banana-bunch', 'jungle', 'jungle', 'Плодове', 'Банани', 15, '/reward-assets/jungle/banana-bunch.svg', 0.75, 0.35, 1.60, true, now(), now()),
    ('jungle-coconut', 'jungle', 'jungle', 'Плодове', 'Кокос', 15, '/reward-assets/jungle/coconut.svg', 0.70, 0.35, 1.50, true, now(), now()),
    ('jungle-monkey', 'jungle', 'jungle', 'Животни', 'Маймуна', 70, '/reward-assets/jungle/monkey.svg', 0.95, 0.45, 2.00, true, now(), now()),
    ('jungle-baby-monkey', 'jungle', 'jungle', 'Животни', 'Малка маймуна', 55, '/reward-assets/jungle/baby-monkey.svg', 0.80, 0.38, 1.75, true, now(), now()),
    ('jungle-parrot', 'jungle', 'jungle', 'Птици', 'Папагал', 55, '/reward-assets/jungle/parrot.svg', 0.85, 0.40, 1.85, true, now(), now()),
    ('jungle-toucan', 'jungle', 'jungle', 'Птици', 'Тукан', 60, '/reward-assets/jungle/toucan.svg', 0.90, 0.42, 1.95, true, now(), now()),
    ('jungle-bird', 'jungle', 'jungle', 'Птици', 'Пъстра птица', 45, '/reward-assets/jungle/jungle-bird.svg', 0.75, 0.35, 1.60, true, now(), now()),
    ('jungle-snake', 'jungle', 'jungle', 'Животни', 'Змия', 45, '/reward-assets/jungle/snake.svg', 0.90, 0.42, 1.95, true, now(), now()),
    ('jungle-crocodile', 'jungle', 'jungle', 'Животни', 'Крокодил', 90, '/reward-assets/jungle/crocodile.svg', 1.10, 0.55, 2.35, true, now(), now()),
    ('jungle-tiger', 'jungle', 'jungle', 'Животни', 'Тигър', 100, '/reward-assets/jungle/tiger.svg', 1.05, 0.50, 2.25, true, now(), now()),
    ('jungle-tiger-cub', 'jungle', 'jungle', 'Животни', 'Малко тигърче', 70, '/reward-assets/jungle/tiger-cub.svg', 0.82, 0.40, 1.80, true, now(), now()),
    ('jungle-elephant', 'jungle', 'jungle', 'Животни', 'Слон', 110, '/reward-assets/jungle/elephant.svg', 1.15, 0.55, 2.45, true, now(), now()),
    ('jungle-baby-elephant', 'jungle', 'jungle', 'Животни', 'Малко слонче', 75, '/reward-assets/jungle/baby-elephant.svg', 0.90, 0.45, 1.95, true, now(), now()),
    ('jungle-lion', 'jungle', 'jungle', 'Животни', 'Лъв', 100, '/reward-assets/jungle/lion.svg', 1.05, 0.50, 2.25, true, now(), now()),
    ('jungle-lion-cub', 'jungle', 'jungle', 'Животни', 'Малко лъвче', 70, '/reward-assets/jungle/lion-cub.svg', 0.82, 0.40, 1.80, true, now(), now()),
    ('jungle-leopard', 'jungle', 'jungle', 'Животни', 'Леопард', 95, '/reward-assets/jungle/leopard.svg', 1.00, 0.48, 2.15, true, now(), now()),
    ('jungle-zebra', 'jungle', 'jungle', 'Животни', 'Зебра', 80, '/reward-assets/jungle/zebra.svg', 1.00, 0.48, 2.15, true, now(), now()),
    ('jungle-giraffe', 'jungle', 'jungle', 'Животни', 'Жираф', 95, '/reward-assets/jungle/giraffe.svg', 1.05, 0.50, 2.25, true, now(), now()),
    ('jungle-hippo', 'jungle', 'jungle', 'Животни', 'Хипопотам', 90, '/reward-assets/jungle/hippo.svg', 1.05, 0.50, 2.25, true, now(), now()),
    ('jungle-frog', 'jungle', 'jungle', 'Животни', 'Жаба', 35, '/reward-assets/jungle/frog.svg', 0.70, 0.35, 1.55, true, now(), now()),
    ('jungle-butterfly', 'jungle', 'jungle', 'Животни', 'Пеперуда', 30, '/reward-assets/jungle/butterfly.svg', 0.65, 0.32, 1.45, true, now(), now())
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
