# TracePilot AI frontend

React 19, TypeScript and Vite dashboard for the Spring Boot investigation API.

## Development

Requires Node.js 22 and a running TracePilot backend (postgres profile, port 8080).

```powershell
cd tracepilot-frontend
npm install
npm run dev
```

Open http://127.0.0.1:5173. Vite proxies /api to http://localhost:8080; override TRACEPILOT_API_URL if needed.

## Workflow behavior

Start investigation uses POST /api/v1/incidents/{incidentId}/workflows/async and receives a 202 response with a persisted workflow ID. The dashboard polls GET /api/v1/workflows/{id} and /steps every 2.5 seconds, displays live progress, and loads the latest persisted Root Cause and Reviewer reports on completion. The legacy synchronous POST endpoint remains available.

The active workflow ID is saved in browser localStorage so refreshing the page resumes monitoring without starting a second workflow. If a status request fails, the dashboard keeps the workflow ID and requires Reconnect rather than allowing another submission. The separate Workflow monitor can inspect saved workflow IDs.

The agent trace includes per-step persisted output and evidence references. Human approval is a local-only UI sandbox: decisions are not saved and never trigger infrastructure changes.

## Verification

```powershell
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

Playwright's smoke test uses mocked API data and does not execute Ollama workflows.

## Full Docker stack

From infrastructure:

```powershell
docker compose up -d --build
docker compose ps
```

Open http://localhost:5173. Nginx serves the built frontend and routes /api to the backend service. Set POSTGRES_PASSWORD using the existing infrastructure environment settings. Stop a separate Vite server before launching Docker to avoid port conflicts.
