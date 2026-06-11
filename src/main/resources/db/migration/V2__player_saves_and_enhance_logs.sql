create table if not exists player_saves (
    user_id varchar(128) primary key,
    current_weapon_id varchar(128) not null,
    materials_json text not null,
    special_items_json text not null,
    owned_weapon_ids_json text not null,
    unlocked_weapon_shop_json text not null,
    discovered_weapon_ids_json text not null,
    highest_reached_weapon_id varchar(128) not null,
    updated_at timestamp not null
);

create table if not exists enhance_attempt_logs (
    id bigserial primary key,
    user_id varchar(128) not null,
    weapon_id varchar(128) not null,
    outcome varchar(32) not null,
    roll_value double precision not null,
    success_threshold double precision not null,
    protection_used boolean not null,
    created_at timestamp not null
);

