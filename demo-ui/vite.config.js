import { defineConfig } from 'vite';

// Each /<service>-api prefix is forwarded to that service's /api base path, so the
// browser only talks to the Vite server and the backend needs no CORS configuration.
const services = {
  '/catalog-api': 'http://localhost:8081',
  '/inventory-api': 'http://localhost:8082',
  '/order-api': 'http://localhost:8083',
  '/account-api': 'http://localhost:8084',
  '/recommendation-api': 'http://localhost:8085',
};

const proxy = Object.fromEntries(
  Object.entries(services).map(([prefix, target]) => [
    prefix,
    {
      target,
      changeOrigin: true,
      rewrite: (path) => path.replace(prefix, '/api'),
      configure: (p) => {
        // Requests then look same-origin to Spring.
        p.on('proxyReq', (req) => req.removeHeader('origin'));
      },
    },
  ]),
);

export default defineConfig({
  server: { port: 5173, proxy },
  preview: { port: 4173, proxy },
});
