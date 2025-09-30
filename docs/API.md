# Multi-Agent Security — API Reference

This document lists the backend HTTP APIs implemented by the Java prototype (Spark Java). Base URL (default): `http://localhost:4567`

Notes
- The Java backend is an embedded prototype using Spark Java. The UI in `ui-angular/` calls these endpoints. When the backend is not available the UI falls back to mocked data.

---

Common endpoints (exact routes implemented)

- GET /agents
  - Description: List registered agents
  - Response: 200
  ```json
  [ { "id": "hunter", "name": "Hunter" }, ... ]
  ```

- POST /agents/:id/trigger
  - Description: Trigger a single agent with a JSON payload. Example body: `{ "type": "custom", "foo":"bar" }`
  - Response: `{ "status":"triggered","agent":"<id>" }`

- POST /publish
  - Description: Publish a raw event to the EventBus (broadcast to all agents)
  - Request example:
  ```json
  { "type": "login", "user": "j.doe", "failedCount": 60, "source": "10.0.0.5" }
  ```
  - Response example:
  ```json
  { "status": "published", "type": "login" }
  ```

- GET /cases
  - Description: List CaseFiles (investigation queue)
  - Response: array of case summaries (id, title, summary, riskScore, assets, user, timestamp)

- GET /cases/{id}
  - Description: Get full CaseFile details (events, timeline, metadata)

- POST /cases/{id}/action
  - Description: Trigger a response action for the case (isolate host, lock account, run scan, execute playbook)
  - Request example:
  ```json
  { "action": "isolate_host", "parameters": { "asset": "FS01" } }
  ```

---

Ingest endpoints

- POST /ingest/nuclei
  - Accepts a Nuclei-style JSON payload (key `vulnerabilities`). The endpoint counts critical vuln entries and publishes a `vuln_scan` event containing a `critical` field.

- POST /ingest/elasticsearch
  - Accepts an Elasticsearch-style batch payload (the prototype reads `errorCount`) and publishes a `log_batch` event.

---

Playbooks / Orchestration (implemented paths)

- POST /playbooks
  - Create or upsert a playbook. Body should include `name` and `definition` (JSON). Returns `{ "id": "<playbookId>" }`.

- POST /playbooks/:id/run
  - Start a playbook run for the given playbook id. Body may contain `target` or contextual data. Returns `{ "status":"running","runId":"..." }`.

- GET /playbook_runs
  - List recent playbook runs with id, playbookId, status and createdAt.

- GET /playbook_runs/:id
  - Get run summary and steps for the given run id.

- GET /playbook_runs/:id/steps
  - List steps (index, name, status, requiresApproval, approverRole, definition).

- POST /playbook_runs/:id/steps/:index/approve
  - Approve a manual step. If OIDC/JWKS is configured the endpoint enforces a token and checks roles (`manager` or `orchestrator`). Query param `role` can override approver role. Returns `{ "status":"approved" }` on success.

---

Notes about auth and roles
- If environment variable `KEYCLOAK_JWKS` is set the Main class creates a JwtValidator and an AuthFilter. Requests will include `roles` in the request attribute (used by playbook endpoints). The implemented endpoints perform role checks where indicated in code (for example, starting a playbook run requires `soc_analyst` role when auth is enabled).

---

Agent-specific helper paths (convenience / aliases)

- GET /hunter/cases — convenience alias to filter `/cases` for hunter-created cases (UI may call this)
- POST /hunter/search — convenience for hunter natural language hunting; the UI may post `{ "nl":"..." }` which the HunterAgent can handle as a published event.

The rest of the agent-specific management endpoints are currently not implemented as first-class REST routes in `Main.java`. Use the common endpoints above for case actions and the ingest routes for telemetry.

---

Appendix: Notes for UI implementers
- The UI `ui-angular/` expects the backend at the same origin (``). If the backend is hosted on a different host/port set the `base` variable in `ui-angular/app.js` or proxy requests from your dev server.

If you'd like, I can add any missing agent-specific endpoints or expand the hunter endpoints to return richer map and telemetry data. The next step I recommend is wiring the Hunter UI (`ui-angular/`) to call `/cases`, `/playbooks` and `/publish`.
