import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const urls = (__ENV.BASE_URLS || 'http://localhost:18081,http://localhost:18082,http://localhost:18083').split(',');
const prefix = __ENV.RUN_PREFIX;
const chargeIterations = Number(__ENV.CHARGE_ITERATIONS || 60);
const spendIterations = Number(__ENV.SPEND_ITERATIONS || 80);
const idempotentIterations = Number(__ENV.IDEMPOTENT_ITERATIONS || 30);
const conflictIterations = Number(__ENV.CONFLICT_ITERATIONS || 30);
const mixedIterations = Number(__ENV.MIXED_ITERATIONS || 120);
const chargeAmount = Number(__ENV.CHARGE_AMOUNT || 10);

const unexpected5xx = new Counter('unexpected_5xx');
const businessErrors = new Counter('business_errors');
const functionalFailures = new Counter('functional_failures');
const applicationLatency = new Trend('application_latency', true);

function scenario(vus, iterations, exec, startTime, maxDuration) {
  if (iterations <= 0) return null;
  return {
    executor: 'shared-iterations',
    exec,
    vus: Math.max(1, Math.min(vus, iterations)),
    iterations: Math.max(1, iterations),
    startTime,
    maxDuration,
  };
}

const scenarios = {};
for (const [name, config] of Object.entries({
  concurrent_charge: scenario(20, chargeIterations, 'charge', '0s', '30s'),
  concurrent_spend: scenario(30, spendIterations, 'spend', '2s', '45s'),
  same_token: scenario(20, idempotentIterations, 'sameToken', '4s', '30s'),
  conflicting_token: scenario(20, conflictIterations, 'conflictingToken', '6s', '30s'),
  mixed_orders: scenario(30, mixedIterations, 'mixedOrder', '8s', '60s'),
})) {
  if (config) scenarios[name] = config;
}

export const options = {
  scenarios,
  thresholds: {
    unexpected_5xx: ['count==0'],
    ...( __ENV.SKIP_FUNCTIONAL_THRESHOLD === 'true' ? {} : { functional_failures: ['count==0'] }),
  },
};

function url() {
  const vu = typeof __VU === 'number' ? __VU : 0;
  const iteration = typeof __ITER === 'number' ? __ITER : 0;
  return urls[(vu + iteration) % urls.length];
}
function headers(user) { return { headers: { 'Content-Type': 'application/json', 'X-Load-Test-User': user } }; }
function observe(response, accepted) {
  applicationLatency.add(response.timings.duration);
  if (response.status >= 500) unexpected5xx.add(1);
  const passed = accepted.includes(response.status);
  if (!passed) functionalFailures.add(1);
  return passed;
}
function issueToken(user) {
  const response = http.post(`${url()}/order-tokens`, null, headers(user));
  observe(response, [201]);
  return response.status === 201 ? response.json('orderToken') : null;
}
function order(user, token, menuId) {
  return http.post(`${url()}/orders`, JSON.stringify({ items: [{ menuId, quantity: 1 }] }), {
    headers: { 'Content-Type': 'application/json', 'X-Load-Test-User': user, 'Order-Token': token },
  });
}

export function setup() {
  return {
    idempotentToken: issueToken(`${prefix}-idempotent`),
    conflictToken: issueToken(`${prefix}-conflict`),
  };
}

export function charge() {
  const response = http.post(`${url()}/point-charges`, JSON.stringify({ amount: chargeAmount }), headers(`${prefix}-hot-charge`));
  check(response, { 'charge accepted': (r) => observe(r, [200]) });
}

export function spend() {
  const user = `${prefix}-hot-order`;
  const token = issueToken(user);
  if (!token) return;
  const response = order(user, token, `${prefix}-menu-a`);
  if (response.status === 422) businessErrors.add(1);
  check(response, { 'spend completed or rejected as business error': (r) => observe(r, [200, 422]) });
}

export function sameToken(data) {
  const response = order(`${prefix}-idempotent`, data.idempotentToken, `${prefix}-menu-a`);
  check(response, { 'same token returns the same successful outcome': (r) => observe(r, [200]) });
}

export function conflictingToken(data) {
  const menuId = __ITER % 2 === 0 ? `${prefix}-menu-a` : `${prefix}-menu-b`;
  const response = order(`${prefix}-conflict`, data.conflictToken, menuId);
  check(response, { 'conflicting token has one winner or conflict': (r) => observe(r, [200, 409]) });
}

export function mixedOrder() {
  const user = `${prefix}-mixed-${((__VU - 1) % 100) + 1}`;
  const token = issueToken(user);
  if (!token) return;
  const menu = [`${prefix}-menu-a`, `${prefix}-menu-b`, `${prefix}-menu-c`][(__VU + __ITER) % 3];
  const response = order(user, token, menu);
  check(response, { 'mixed order completed': (r) => observe(r, [200]) });
}
