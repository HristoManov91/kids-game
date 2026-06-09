update reward_catalog_items item
set image = mapping.image,
    image_asset_id = null,
    updated_at = now()
from (values
    ('sea-seahorse-yellow', '/reward-assets/polished/sea/seahorse-yellow.png'),
    ('sea-seahorse-purple', '/reward-assets/polished/sea/seahorse-purple.png')
) as mapping(id, image)
where item.id = mapping.id;
