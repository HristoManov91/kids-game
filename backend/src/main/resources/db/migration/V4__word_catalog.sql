create table word_catalog_entries (
    id bigserial primary key,
    category varchar(40) not null,
    word varchar(120) not null,
    image varchar(40) not null,
    syllables_json text not null,
    difficulty integer not null check (difficulty between 1 and 10),
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_word_catalog_entries_category_word unique (category, word)
);

create index idx_word_catalog_entries_category_active
    on word_catalog_entries(category, active, difficulty, word);
