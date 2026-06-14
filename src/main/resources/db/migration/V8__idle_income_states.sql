create table if not exists idle_income_states (
    user_id varchar(128) primary key,
    last_claimed_at timestamp not null
);
