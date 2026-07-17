create table orders (
    order_id varchar(255) primary key,
    user_id varchar(255) not null references point_accounts (user_id),
    status varchar(32) not null,
    total_payment_amount bigint not null,
    remaining_point_balance bigint not null,
    completed_at timestamptz not null,
    constraint orders_status_valid check (status = 'COMPLETED'),
    constraint orders_amount_non_negative check (total_payment_amount >= 0),
    constraint orders_balance_non_negative check (remaining_point_balance >= 0)
);

create table order_items (
    order_id varchar(255) not null references orders (order_id),
    menu_id varchar(255) not null references menus (menu_id),
    quantity bigint not null,
    unit_price_snapshot bigint not null,
    line_amount bigint not null,
    primary key (order_id, menu_id),
    constraint order_items_quantity_positive check (quantity >= 1),
    constraint order_items_price_non_negative check (unit_price_snapshot >= 0),
    constraint order_items_amount_non_negative check (line_amount >= 0)
);

create table order_event_intents (
    event_id varchar(255) primary key,
    order_id varchar(255) not null unique references orders (order_id),
    event_name varchar(100) not null,
    payload jsonb not null,
    occurred_at timestamptz not null,
    delivery_state varchar(32) not null,
    constraint order_event_intents_name_valid check (event_name = 'OrderCompleted'),
    constraint order_event_intents_state_valid check (delivery_state = 'PENDING')
);
