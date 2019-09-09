CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

insert into account_group (
    "uuid",
    "group_name",
    "created_at"
) values
(uuid_generate_v4(), 'ROLE_USER', now()),
(uuid_generate_v4(), 'ROLE_STAFF', now()),
(uuid_generate_v4(), 'ROLE_SERVICE', now()),
(uuid_generate_v4(), 'ROLE_ADMIN', now());

