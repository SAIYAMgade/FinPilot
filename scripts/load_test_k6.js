import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up to 20 users
    { duration: '1m', target: 50 },   // Sustained load at 50 users
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<250', 'p(99)<500'], // 95% of requests must complete under 250ms
    http_req_failed: ['rate<0.01'],                 // Error rate below 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  // 1. Authenticate and retrieve token
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    usernameOrEmail: 'analyst',
    password: 'Password@123',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'has token': (r) => r.json('data.accessToken') !== undefined,
  });

  const token = loginRes.json('data.accessToken');
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // 2. Ingest transaction with unique idempotency key
  const uniqueKey = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const txnPayload = JSON.stringify({
    externalReference: `TXN-K6-${uniqueKey}`,
    sourceSystem: 'K6_LOAD_RUNNER',
    amount: (Math.random() * 5000 + 100).toFixed(2),
    currency: 'INR',
    transactionType: 'PAYMENT',
    idempotencyKey: uniqueKey,
    description: 'High-throughput k6 ingestion benchmark test',
  });

  const ingestRes = http.post(`${BASE_URL}/transactions`, txnPayload, { headers: authHeaders });
  check(ingestRes, {
    'transaction ingestion is 201': (r) => r.status === 201,
  });

  // 3. Query transactions with pagination
  const listRes = http.get(`${BASE_URL}/transactions?page=0&size=20`, { headers: authHeaders });
  check(listRes, {
    'query transactions is 200': (r) => r.status === 200,
  });

  sleep(0.5);
}
