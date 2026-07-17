create table menus (
    menu_id varchar(255) primary key,
    name varchar(255) not null,
    current_price bigint not null,
    availability varchar(32) not null,
    constraint menus_menu_id_not_blank check (length(trim(menu_id)) > 0),
    constraint menus_name_not_blank check (length(trim(name)) > 0),
    constraint menus_current_price_non_negative check (current_price >= 0),
    constraint menus_availability_valid check (availability in ('ORDERABLE', 'NOT_ORDERABLE'))
);
