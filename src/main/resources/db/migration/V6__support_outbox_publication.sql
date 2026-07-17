alter table order_event_intents drop constraint order_event_intents_state_valid;

alter table order_event_intents
    add column attempts integer not null default 0,
    add column available_at timestamptz not null default clock_timestamp(),
    add column lease_until timestamptz,
    add column published_at timestamptz,
    add column last_error text,
    add constraint order_event_intents_state_valid
        check (delivery_state in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    add constraint order_event_intents_attempts_non_negative check (attempts >= 0);

create index order_event_intents_publishable_idx
    on order_event_intents (available_at, occurred_at)
    where delivery_state in ('PENDING', 'PROCESSING');
