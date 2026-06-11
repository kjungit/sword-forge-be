alter table player_saves
    add column if not exists weapon_inventory_json text not null default '{}';

