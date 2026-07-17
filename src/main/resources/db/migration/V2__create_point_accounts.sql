create table point_accounts (
    user_id varchar(255) primary key,
    balance bigint not null,
    constraint point_accounts_user_id_not_blank check (length(trim(user_id)) > 0),
    constraint point_accounts_balance_non_negative check (balance >= 0)
);
