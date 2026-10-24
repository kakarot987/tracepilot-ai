# TracePilot AI — Frontend

Milestone 5 dashboard scaffold (React 19, TypeScript, Vite).

## Local development

Prerequisites: Node.js 20.19+ or 22.12+ and the TracePilot Spring Boot backend running with the postgres profile on http://localhost:8080.

```powershell
cd tracepilot-frontend
npm install
npm run dev
```

Open http://127.0.0.1:5173. Vite proxies `/api` to the backend (default: http://localhost:8080). Override the target with `TRACEPILOT_API_URL` before starting Vite.

```powershell
npm run build
```

The current backend POST workflow endpoint is synchronous. The app intentionally shows a pending indicator until the HTTP response arrives. Polling/live progress and the detailed report are upcoming work; they are not implemented in this scaffold.
