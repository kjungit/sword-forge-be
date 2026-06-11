create table if not exists reward_grant_logs (
    id bigserial primary key,
    user_id varchar(128) not null,
    source_type varchar(64) not null,
    source_id varchar(128) not null,
    reward_kind varchar(32) not null,
    reward_id varchar(128) not null,
    amount int not null,
    details_json text not null,
    created_at timestamp not null
);
