create table if not exists tasks (
    id serial primary key,
    user_id integer not null,
    description varchar(256) not null,
    is_finished boolean not null,
    foreign key (user_id) references users (id)
)