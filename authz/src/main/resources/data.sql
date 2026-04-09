insert into users (username, password, enabled) values ('jdoe', '{noop}password', true) on conflict do nothing;
insert into authorities (username, authority) values ('jdoe', 'ROLE_USER') on conflict do nothing;
