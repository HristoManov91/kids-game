update word_catalog_entries
set image = '👩‍👧',
    updated_at = now()
where category = 'BULGARIAN'
  and lower(word) = 'мама'
  and image = '👩';

update word_catalog_entries
set image = '👧👶',
    updated_at = now()
where category = 'BULGARIAN'
  and lower(word) = 'кака'
  and image = '👧';
