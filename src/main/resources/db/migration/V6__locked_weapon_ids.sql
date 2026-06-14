alter table player_saves
    add column if not exists locked_weapon_ids_json text not null default '[]';
