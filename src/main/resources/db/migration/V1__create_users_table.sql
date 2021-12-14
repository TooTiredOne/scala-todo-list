create table if not exists users (
    id serial primary key,
    username varchar(30) unique not null,
    password_hash varchar not null
)