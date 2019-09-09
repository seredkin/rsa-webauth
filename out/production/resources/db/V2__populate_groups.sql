CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

insert into account_group (
    "uuid",
    "name",
    "created_at"
) values
(uuid_generate_v4(), 'web_users', now()),
(uuid_generate_v4(), 'staff', now()),
(uuid_generate_v4(), 'integrations', now()),
(uuid_generate_v4(), 'admins', now());

