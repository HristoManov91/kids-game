update reward_catalog_items item
set name = source.name,
    image = source.image,
    updated_at = now()
from (
    values
        ('super-captain-america', 'Капитан Америка', 'https://cdn.marvel.com/content/2x/003cap_ons_cut_dsk_01.webp'),
        ('super-iron-man', 'Железният човек', 'https://cdn.marvel.com/content/2x/002irm_ons_cut_dsk_01_0.webp'),
        ('super-thor', 'Тор', 'https://cdn.marvel.com/content/2x/004tho_ons_cut_dsk_01_1.webp'),
        ('super-hulk', 'Хълк', 'https://cdn.marvel.com/content/2x/006hbb_ons_cut_dsk_01_2.webp'),
        ('super-spider-hero', 'Човекът паяк', 'https://cdn.marvel.com/content/2x/005smp_ons_cut_dsk_01_0.webp'),
        ('super-doctor-strange', 'Доктор Стрейндж', 'https://cdn.marvel.com/content/2x/009drs_ons_cut_dsk_01_0.jpg'),
        ('super-black-widow', 'Черната вдовица', 'https://cdn.marvel.com/content/2x/011blw_ons_cut_dsk_03.webp'),
        ('super-hawkeye', 'Ястребово око', 'https://cdn.marvel.com/content/2x/018hcb_ons_cut_dsk_01_0.webp'),
        ('super-wolverine', 'Черната пантера', 'https://cdn.marvel.com/content/2x/007blp_ons_cut_dsk_01_0.webp'),
        ('super-star-hero', 'Капитан Марвел', 'https://cdn.marvel.com/content/2x/008cmv_ons_cut_dsk_03_0.webp'),
        ('super-star-shield', 'Щитът на Капитан Америка', 'https://cdn.marvel.com/content/2x/003cap_ons_cut_dsk_01.webp'),
        ('super-arc-core', 'Бронята на Железния човек', 'https://cdn.marvel.com/content/2x/002irm_ons_cut_dsk_01_0.webp'),
        ('super-hammer', 'Тор с Мьолнир', 'https://cdn.marvel.com/content/2x/004tho_ons_cut_dsk_01_1.webp'),
        ('super-power-fist', 'Хълк смазва', 'https://cdn.marvel.com/content/2x/006hbb_ons_cut_dsk_01_2.webp'),
        ('super-web', 'Паяжината на Спайдърмен', 'https://cdn.marvel.com/content/2x/005smp_ons_cut_dsk_01_0.webp'),
        ('super-magic-portal', 'Магията на Доктор Стрейндж', 'https://cdn.marvel.com/content/2x/009drs_ons_cut_dsk_01_0.jpg'),
        ('super-cape', 'Космическа сила', 'https://cdn.marvel.com/content/2x/008cmv_ons_cut_dsk_03_0.webp'),
        ('super-mask', 'Маската на Черната пантера', 'https://cdn.marvel.com/content/2x/007blp_ons_cut_dsk_01_0.webp'),
        ('super-city', 'Отборът Отмъстителите', 'https://cdn.marvel.com/content/2x/013vis_ons_cut_dsk_01_0.webp'),
        ('super-headquarters', 'Мисията на Отмъстителите', 'https://cdn.marvel.com/content/2x/010ant_ons_cut_dsk_01_1.webp'),
        ('super-energy-crystal', 'Сила от Ваканда', 'https://cdn.marvel.com/content/2x/007blp_ons_cut_dsk_01_0.webp'),
        ('super-comic-burst', 'Отмъстителски удар', 'https://cdn.marvel.com/content/2x/018hcb_ons_cut_dsk_01_0.webp')
) as source(id, name, image)
where item.id = source.id;
