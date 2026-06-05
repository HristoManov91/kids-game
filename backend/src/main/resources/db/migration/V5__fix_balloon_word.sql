update word_catalog_entries
set word = 'балон',
    syllables_json = '["БА","ЛОН"]',
    updated_at = now()
where category = 'BULGARIAN'
  and lower(word) = 'балони'
  and image = '🎈'
  and not exists (
      select 1
      from word_catalog_entries existing
      where existing.category = 'BULGARIAN'
        and lower(existing.word) = 'балон'
  );
