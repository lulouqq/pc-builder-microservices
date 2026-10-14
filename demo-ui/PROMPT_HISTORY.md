# Prompt History – demo-ui

This file records AI-assisted development prompts that relate **only to `demo-ui`** (the demo frontend).
Project-wide prompts are recorded in the root [`PROMPT_HISTORY.md`](../PROMPT_HISTORY.md).

For every substantial AI-assisted change to the demo frontend, append an entry with: date, goal, exact user prompt, short summary of changes, and files created/modified.

---

## Prompt 1

**Date:** 2026-10-14

**Goal:** Create a simple Vite + vanilla JavaScript demo frontend (`demo-ui/`) that exercises the finished backend services through a Vite proxy.

**Exact user prompt** (originally written in Russian; English translation):

````text
Create a simple, clean demo frontend for the current PC Builder Microservices project.
First study specs/api-endpoints.md, the root README.md and the existing REST contracts. The backend is already finished and working — do not change backend code, specs or existing services.
Create the frontend entirely in a separate folder demo-ui/.
Use Vite + vanilla JavaScript + plain CSS, without React, TypeScript, UI libraries, state-management libraries or unnecessary dependencies. This is not a production frontend, but a simple UI for demonstrating a university microservices project.
Make one nice dashboard-style page with clear sections:
Login: customer login;
admin login;
store the JWT for subsequent requests.

Catalog: show products;
product details;
trending products;
reviews.

Admin / Inventory: change a product's stock;
show quantity / reservedQuantity.

Recommendation: budget / purpose / preferences form;
call R1;
nicely show the AI recommendation, reasoning, total price and the 7 components;
show recommendation history.

Order: create a cart;
add a product;
choose a saved address;
checkout;
show the current status;
automatically poll the status for a few seconds so that PLACED → PROCESSING via Kafka is visible;
PAID, FAILED, Cancel buttons;
show order history.

Stock alerts: subscribe to stock notifications;
show /users/{userId}/stock-alerts;
poll alerts so that the appearance of STOCK_LOW / PRODUCT_RESTOCKED is visible during the demo.

Activity / API log: at the bottom of the page show the latest API requests/responses in a compact, readable form, so that the demo shows what is really happening.

Make the UI simple but visually pleasant: cards, proper spacing, badges for statuses, responsive layout. Don't spend time on complex design.
To avoid changing CORS in the backend, configure a Vite proxy:
/catalog-api → http://localhost:8081/api
/inventory-api → http://localhost:8082/api
/order-api → http://localhost:8083/api
/account-api → http://localhost:8084/api
/recommendation-api → http://localhost:8085/api
Use these proxy paths in the frontend fetch calls.
Add short instructions to demo-ui/README.md:
npm install
npm run dev
which backend services and Kafka must be running.
Do not add mock data instead of backend responses. If an endpoint requires existing data, the UI should show a clear error.
After the implementation:
run the frontend build;
check that it builds without errors;
give a short report of the created files and the implemented demo flows.
Do not commit/push the changes.

Record this prompt only in demo-ui/PROMPT_HISTORY.md. Do not change the root PROMPT_HISTORY.md.

Before starting, translate this prompt into English and record the English version in demo-ui/PROMPT_HISTORY.md. Do not change the root PROMPT_HISTORY.md.
````

**Summary of changes:**

- New standalone `demo-ui/` (Vite 5.4.21 as the only dependency, vanilla JS, plain CSS). No backend code, specs or root files were changed.
- `vite.config.js`: proxies `/catalog-api`, `/inventory-api`, `/order-api`, `/account-api`, `/recommendation-api` to the services' `/api` base paths (dev and preview servers); the `Origin` header is removed so requests look same-origin to Spring.
- `src/api.js`: fetch wrapper that adds the Bearer JWT, turns `{code, message}` error bodies (and empty proxy 5xx responses when a service is down) into readable errors, and reports every call to the activity log; polling calls are logged only when their response changes.
- `src/main.js` sections:
  - Login: customer and admin login (A2) with both JWTs kept in `localStorage` until expiry; customer registration (A1).
  - Catalog: categories + filters (C1), product details with `stockStatus` and specifications (C2), trending list (C3), reviews list and posting a review (C4).
  - Admin / Inventory: read stock (quantity / reserved / available) and set stock (I3); create categories and products (C5) so an empty H2 catalog can be filled for the demo.
  - Stock alerts: subscribe (I2), list `/users/{userId}/stock-alerts` with 3-second auto-refresh and highlighting of new alerts.
  - Recommendation: R1 form and result (total vs budget, description, reasoning, 7 components), R4 history, R3 view, "add build to cart" (R3 order endpoint).
  - Order: create cart (loads the existing DRAFT on `OrderStateViolation`), add/remove items, add/select saved address (A1/O2), checkout with 20-second status polling for `PLACED → PROCESSING`, PAID / FAILED / Cancel, order history (O5); buttons are enabled per order status.
  - Activity / API log: newest-first list with service, method, path, HTTP status, duration, short preview and expandable request/response bodies (passwords masked, JWTs shortened).
- `npm run build` succeeds. The proxy was smoke-tested with public GET endpoints and a failed login against the locally running services.
- Assumptions: the admin product/category forms and the customer registration / address forms are included because the in-memory databases start empty and the requested flows (login, checkout with a saved address, R1) need that data; the order poll stops after 20 seconds.

**Files created/modified:**

- `demo-ui/package.json`, `demo-ui/package-lock.json`
- `demo-ui/vite.config.js`
- `demo-ui/index.html`
- `demo-ui/src/main.js`, `demo-ui/src/api.js`, `demo-ui/src/style.css`
- `demo-ui/README.md`
- `demo-ui/PROMPT_HISTORY.md`
- `demo-ui/.gitignore`
