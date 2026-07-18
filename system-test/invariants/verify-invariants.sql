\set ON_ERROR_STOP on

create temporary table invariant_results (
    invariant text primary key,
    passed boolean not null,
    details text not null
);

insert into invariant_results
select 'point_charge_no_lost_update', balance = :expected_charge_balance,
       format('balance=%s expected=%s', balance, :expected_charge_balance)
  from point_accounts where user_id = :'prefix' || '-hot-charge';

insert into invariant_results
select 'all_balances_non_negative', coalesce(min(balance), 0) >= 0,
       format('minimum_balance=%s', coalesce(min(balance), 0))
  from point_accounts where user_id like :'prefix' || '-%';

insert into invariant_results
select 'point_order_equation', bool_and(account.balance = seeded.initial_balance - coalesce(paid.total, 0)),
       string_agg(format('%s actual=%s expected=%s', account.user_id, account.balance,
                         seeded.initial_balance - coalesce(paid.total, 0)), '; ')
  from point_accounts account
  join lateral (select case
      when account.user_id = :'prefix' || '-hot-order' then 50000
      when account.user_id in (:'prefix' || '-idempotent', :'prefix' || '-conflict') then 10000
      else 100000 end as initial_balance) seeded on true
  left join lateral (select sum(total_payment_amount) total from orders where user_id = account.user_id) paid on true
 where account.user_id like :'prefix' || '-%'
   and account.user_id <> :'prefix' || '-hot-charge';

insert into invariant_results
select 'one_confirmed_order_per_token', coalesce(max(order_count), 0) <= 1,
       format('maximum_orders_per_token=%s', coalesce(max(order_count), 0))
  from (
      select token.order_token, count(orders.order_id) order_count
        from order_tokens token
        left join orders on orders.order_id = token.order_id
       where token.user_id like :'prefix' || '-%'
       group by token.order_token
  ) counts;

insert into invariant_results
select 'one_outbox_event_per_successful_order', count(*) = count(event.event_id),
       format('orders=%s outbox_events=%s', count(*), count(event.event_id))
  from orders
  left join order_event_intents event on event.order_id = orders.order_id
 where orders.user_id like :'prefix' || '-%';

insert into invariant_results
select 'order_total_matches_items', count(*) filter (where orders.total_payment_amount <> items.total) = 0,
       format('mismatches=%s', count(*) filter (where orders.total_payment_amount <> items.total))
  from orders
  join lateral (select sum(line_amount) total from order_items where order_id = orders.order_id) items on true
 where orders.user_id like :'prefix' || '-%';

insert into invariant_results
select 'outbox_payload_matches_order', count(*) filter (
           where event.payload ->> 'orderId' <> event.order_id
              or (event.payload ->> 'totalPaymentAmount')::bigint <> orders.total_payment_amount) = 0,
       format('mismatches=%s', count(*) filter (
           where event.payload ->> 'orderId' <> event.order_id
              or (event.payload ->> 'totalPaymentAmount')::bigint <> orders.total_payment_amount))
  from order_event_intents event
  join orders on orders.order_id = event.order_id
 where orders.user_id like :'prefix' || '-%';

insert into invariant_results
select 'consumer_event_applied_at_most_once', count(*) = count(distinct processed.event_id),
       format('rows=%s distinct_events=%s', count(*), count(distinct processed.event_id))
  from popular_menu_processed_events processed
  join orders on orders.order_id = processed.order_id
 where orders.user_id like :'prefix' || '-%';

insert into invariant_results
with expected as (
    select items.menu_id, sum(items.quantity)::numeric total_quantity, count(*)::numeric order_count
      from orders join order_items items using (order_id)
     where orders.user_id like :'prefix' || '-%'
     group by items.menu_id
), actual as (
    select contribution.menu_id, sum(contribution.quantity)::numeric total_quantity, count(*)::numeric order_count
      from popular_menu_contributions contribution
      join orders on orders.order_id = contribution.order_id
     where orders.user_id like :'prefix' || '-%'
     group by contribution.menu_id
), differences as (
    (select * from expected except select * from actual)
    union all
    (select * from actual except select * from expected)
)
select 'popular_projection_matches_orders', count(*) = 0,
       format('difference_rows=%s', count(*)) from differences;

insert into invariant_results
select 'outbox_recovered_without_residue', count(*) = 0,
       format('pending_or_processing=%s', count(*))
  from order_event_intents event join orders using (order_id)
 where orders.user_id like :'prefix' || '-%'
   and event.delivery_state in ('PENDING', 'PROCESSING');

insert into invariant_results
select 'no_failed_outbox', count(*) = 0, format('failed=%s', count(*))
  from order_event_intents event join orders using (order_id)
 where orders.user_id like :'prefix' || '-%' and event.delivery_state = 'FAILED';

select invariant, passed, details from invariant_results order by invariant;

do $$
begin
    if exists (select 1 from invariant_results where not passed) then
        raise exception 'One or more system-test invariants failed';
    end if;
end $$;
