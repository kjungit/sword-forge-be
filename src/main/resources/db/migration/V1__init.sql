create table if not exists schema_version_marker (
    id bigserial primary key,
    created_at timestamptz not null default now()
);

