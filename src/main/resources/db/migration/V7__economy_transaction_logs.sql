create table if not exists economy_transaction_logs (
    id bigserial primary key,
    user_id varchar(128) not null,
    transaction_type varchar(64) not null,
    reference_id varchar(128) not null,
    resource_kind varchar(32) not null,
    resource_id varchar(128) not null,
    amount int not null,
    balance_after int,
    details_json text not null,
    created_at timestamp not null
);

create index if not exists idx_economy_logs_user_created_at
    on economy_transaction_logs (user_id, created_at desc);

create index if not exists idx_economy_logs_user_transaction_type
    on economy_transaction_logs (user_id, transaction_type);
