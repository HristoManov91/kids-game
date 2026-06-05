alter table reward_crystal_spends
    add column placed_item_id varchar(80);

create index idx_reward_crystal_spends_user_picture_item
    on reward_crystal_spends(user_id, picture_id, item_id, created_at desc);

create index idx_reward_crystal_spends_user_picture_placed
    on reward_crystal_spends(user_id, picture_id, placed_item_id, created_at desc);
