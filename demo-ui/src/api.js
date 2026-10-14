// Thin fetch wrapper: every call goes through the Vite proxy prefix of its service
// and is reported to the activity log.

export const SERVICES = {
  catalog: { base: '/catalog-api', port: 8081 },
  inventory: { base: '/inventory-api', port: 8082 },
  order: { base: '/order-api', port: 8083 },
  account: { base: '/account-api', port: 8084 },
  recommendation: { base: '/recommendation-api', port: 8085 },
};

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

const listeners = [];
export function onApiCall(fn) {
  listeners.push(fn);
}

// Last response body per poll key; unchanged poll responses are not logged.
const lastPoll = new Map();

/**
 * @param {string} service key of SERVICES
 * @param {string} method HTTP method
 * @param {string} path path relative to the service's /api base
 * @param {{body?: any, token?: string, poll?: boolean}} [options]
 *   poll: the call is repeated by a timer; it is logged only when the response changes.
 */
export async function api(service, method, path, { body, token, poll = false } = {}) {
  const { base, port } = SERVICES[service];
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const started = performance.now();
  let status = 0;
  let data = null;
  let error = null;

  try {
    const res = await fetch(base + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    status = res.status;
    const text = await res.text();
    data = parseBody(text);

    if (!res.ok) {
      if (data && typeof data === 'object' && data.code) {
        error = new ApiError(status, data.code, data.message || data.code);
      } else if (status >= 500 && !data) {
        // The Vite proxy answers 500/502/504 with an empty body when the service is not running.
        error = new ApiError(status, 'Unreachable',
          `${service}-service did not answer. Is it running on port ${port}?`);
      } else {
        error = new ApiError(status, `HTTP ${status}`,
          typeof data === 'string' && data ? data.slice(0, 200) : `Request failed with HTTP ${status}`);
      }
    }
  } catch (e) {
    error = new ApiError(0, 'NetworkError', `Network error: ${e.message}. Is the Vite dev server running?`);
  }

  const entry = {
    time: new Date(),
    service,
    method,
    path,
    status,
    ms: Math.round(performance.now() - started),
    request: body,
    response: data,
    auth: Boolean(token),
    poll,
  };

  let shouldLog = true;
  if (poll) {
    const key = `${service} ${method} ${path}`;
    const snapshot = JSON.stringify([status, data]);
    shouldLog = lastPoll.get(key) !== snapshot;
    lastPoll.set(key, snapshot);
  }
  if (shouldLog) listeners.forEach((fn) => fn(entry));

  if (error) throw error;
  return data;
}

function parseBody(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
