update word_catalog_entries
set image = 'focus:👩|👧|0',
    updated_at = now()
where category = 'BULGARIAN'
  and lower(word) = 'мама'
  and image in ('👩', '👩‍👧');

update word_catalog_entries
set image = 'focus:👧|👦|0',
    updated_at = now()
where category = 'BULGARIAN'
  and lower(word) = 'кака'
  and image in ('👧', '👧👶');
