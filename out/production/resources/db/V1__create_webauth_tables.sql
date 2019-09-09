
create table account_group (
    "uuid" uuid not null primary key,
    "name" text  not null,
    "created_at" timestamp not null default now()
);

-- This table may be isolated from the others
create table account (
    "uuid" uuid not null primary key,
    "group_uuid" uuid not null references account_group,
    "email" text not null,
    "customer_name" text,
    "salt"  bytea not null,
    "hash"  bytea not null,
    "created_at" timestamp not null default now(),
    "last_password_change" timestamp not null default now()
);

create unique index idx_account_email on account(email);

create table account_login_attempt (
    "uuid" uuid not null primary key,
    "email" text not null,
    "success" boolean not null,
    "suspended" boolean not null,
    "created_at" timestamp not null default now()
);

create table account_password_reset (
    "uuid" uuid not null primary key,
    "email" uuid not null,
    "created_at" timestamp not null default now(),
    "token" text not null,
    "confirmed_at" timestamp not null default now()
);

-- This table may be isolated from the others
create table session_tokens (
    "uuid" uuid not null primary key,
    "salt" bytea not null,
    "hash" text not null,
    "account_uuid" uuid not null references account,
    "expires_at" timestamp not null default now(),
    "created_at" timestamp not null default now()
);
create unique index account_email on session_tokens(hash);

