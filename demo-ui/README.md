# demo-ui

A one-page demo dashboard for the PC Builder microservices (Vite + vanilla JavaScript + plain CSS).
It is a demonstration tool, not a production frontend: every value it shows comes from the real backend; there is no mock data.

## Run

```bash
cd demo-ui
npm install
npm run dev        # http://localhost:5173
```

`npm run build` creates a static build in `dist/` (`npm run preview` serves it with the same proxy).

## What must be running

From the repository root (see the root `README.md` and each service's README):

1. Kafka on `localhost:9092`: `docker compose up -d`
2. All five services, each with the same `JWT_SECRET`:

| Service | Port | Extra settings |
|---|---|---|
| catalog-service | 8081 | – |
| inventory-service | 8082 | – |
| order-service | 8083 | – |
| account-service | 8084 | `ADMIN_PASSWORD` (admin login; username `admin` by default) |
| recommendation-service | 8085 | `LLM_API_KEY`, `LLM_MODEL_NAME` (otherwise R1 returns 503) |

```bash
JWT_SECRET='<at least 32 bytes>' ./mvnw spring-boot:run -pl catalog-service
# … the same for the other services; account-service also needs ADMIN_PASSWORD
```

The Vite dev server forwards API calls, so the backend needs no CORS configuration:

| Prefix used by the UI | Forwarded to |
|---|---|
| `/catalog-api` | `http://localhost:8081/api` |
| `/inventory-api` | `http://localhost:8082/api` |
| `/order-api` | `http://localhost:8083/api` |
| `/account-api` | `http://localhost:8084/api` |
| `/recommendation-api` | `http://localhost:8085/api` |

## Demo flow

The databases are in-memory H2 and start empty (except the admin account), so a demo starts like this:

1. **Login**: log in as admin; register a customer and log in as that customer. Both JWTs are kept (in `localStorage`, until they expire).
2. **Admin**: create the categories `CPU`, `Motherboard`, `RAM`, `GPU`, `Storage`, `PSU`, `Case` and products in them (with `Socket`, `Memory Type`, `Form Factor` specifications for compatibility), then give each product stock.
3. **Catalog**: open product details (stock status, reviews); viewed products appear under *Trending* (via Kafka).
4. **Stock alerts**: subscribe to a product, then lower its stock below 10 or restock it from 0 as admin; the alert appears within a few seconds (auto-refresh every 3 s).
5. **Recommendation**: enter budget / purpose / preferences, generate a build (R1), optionally add it to the cart (R3).
6. **Order**: create a cart, add products, add/select a saved address, check out. The UI polls the order for up to 20 s so `PLACED → PROCESSING` (Kafka round trip through inventory-service) is visible. Then record `PAID` / `FAILED` or cancel.
7. **Activity / API log** (bottom): every request with method, service, path, HTTP status, duration and the request/response bodies. Passwords are hidden and JWTs shortened.

If a service is down or data is missing, the section shows the backend's error (`code (status): message`) or "…-service did not answer. Is it running on port …?".
