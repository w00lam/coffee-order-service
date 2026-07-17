create table order_tokens (
    order_token varchar(255) primary key,
    user_id varchar(255) not null references point_accounts (user_id),
    status varchar(32) not null,
    issued_at timestamptz not null,
    expires_at timestamptz not null,
    request_fingerprint varchar(255),
    confirmed_http_status integer,
    confirmed_body jsonb,
    confirmed_at timestamptz,
    order_id varchar(255),
    constraint order_tokens_value_not_blank check (length(trim(order_token)) > 0),
    constraint order_tokens_status_valid check (status in ('AVAILABLE', 'SUCCEEDED', 'BUSINESS_FAILED')),
    constraint order_tokens_expiry_valid check (expires_at > issued_at)
);
