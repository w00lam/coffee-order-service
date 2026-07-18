\set ON_ERROR_STOP on

insert into menus (menu_id, name, current_price, availability) values
    (:'prefix' || '-menu-a', 'Load Test Americano', 1000, 'ORDERABLE'),
    (:'prefix' || '-menu-b', 'Load Test Latte', 1500, 'ORDERABLE'),
    (:'prefix' || '-menu-c', 'Load Test Mocha', 2000, 'ORDERABLE')
on conflict (menu_id) do nothing;

insert into point_accounts (user_id, balance) values
    (:'prefix' || '-hot-charge', 0),
    (:'prefix' || '-hot-order', 50000),
    (:'prefix' || '-idempotent', 10000),
    (:'prefix' || '-conflict', 10000)
on conflict (user_id) do nothing;

insert into point_accounts (user_id, balance)
select :'prefix' || '-mixed-' || value, 100000
  from generate_series(1, 100) value
on conflict (user_id) do nothing;
