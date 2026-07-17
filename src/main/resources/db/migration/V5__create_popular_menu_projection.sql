create table popular_menu_processed_events (
    event_id varchar(255) primary key,
    order_id varchar(255) not null,
    completed_at timestamptz not null,
    processed_at timestamptz not null
);

create table popular_menu_contributions (
    event_id varchar(255) not null references popular_menu_processed_events (event_id),
    order_id varchar(255) not null,
    menu_id varchar(255) not null references menus (menu_id),
    quantity bigint not null,
    completed_at timestamptz not null,
    primary key (event_id, menu_id),
    constraint popular_menu_contributions_quantity_positive check (quantity >= 1)
);

create index popular_menu_contributions_completed_at_idx
    on popular_menu_contributions (completed_at, menu_id) include (quantity);

create table popular_menu_hourly_stats (
    bucket_start timestamptz not null,
    menu_id varchar(255) not null references menus (menu_id),
    total_quantity numeric not null,
    order_count numeric not null,
    primary key (bucket_start, menu_id),
    constraint popular_menu_hourly_quantity_non_negative check (total_quantity >= 0),
    constraint popular_menu_hourly_order_count_non_negative check (order_count >= 0)
);
