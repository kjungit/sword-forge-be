alter table player_saves
    add column if not exists pity_stacks_json text not null default '{}';

alter table player_saves
    add column if not exists version bigint not null default 0;

alter table enhance_attempt_logs
    add column if not exists details_json text not null default '{}';
