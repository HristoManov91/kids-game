alter table quiz_attempts
    add column included_modes_json text not null default '[]',
    add column leaderboard_eligible boolean not null default true;

