
# Multi-Agent Security Prototype (Java)

This repository contains a lightweight Java prototype of a multi-agent cybersecurity platform. It demonstrates an architecture and orchestration patterns for eight specialized agents, an in-memory case store, an EventBus, and a tiny HTTP API (Spark Java). The prototype is intentionally minimal and designed to be extended with real connectors, ML models, and a full UI.

Contents
 - `src/main/java` — Java sources (agents, models, EventBus, Main)
 - `pom.xml` — Maven build
 - `README.md` — this document

Quick goals
 - Provide a working local prototype so teams can iterate on agent logic and orchestration flows
 - Map architecture to the product requirements (detection, vuln mgmt, logs, email, IAM, cloud, endpoints, SOAR)
 - Provide CI to automatically build the project on push

How to build & run (PowerShell)
```powershell
cd 'D:\softwares\prototype\canal-sample-drawings\multi-agent-security'
```

App endpoints (HTTP)
 - GET /agents — list registered agents
 - POST /publish — publish an event to all agents (JSON body with `type` and payload)
 - POST /agents/{id}/trigger — trigger a single agent with a JSON payload
 - GET /cases — list generated investigation CaseFiles

Sample event examples
 - Login brute-force: `{ "type": "login", "failedCount": 60 }`
 - Vulnerability scan: `{ "type": "vuln_scan", "critical": 3 }`
 - Email phishing: `{ "type": "email", "maliciousLinks": 2 }`
 - Endpoint ransomware: `{ "type": "endpoint", "ransomwareSigns": true }`

Verification & Requirements mapping
The prototype implements one agent per core problem. Below is the verification matrix showing what's implemented in this repo (Simulation) and recommended next steps to convert simulations into production connectors.

1) Alert fatigue / MTTD (Agent: Hunter)
 - Implemented: Simulated anomaly detection on `login` events; creates CaseFiles and risk scores.
 - Next steps: integrate with SIEM (Wazuh/Elastic), add ML models for anomaly detection, implement automatic triage confidence thresholds and suppression rules.

2) Vulnerability management (Agent: Guardian)
 - Implemented: Accepts `vuln_scan` events, creates cases for critical vulns.
 - Next steps: integrate Nuclei/Trivy/OpenVAS results ingestion and implement patch orchestration (Ansible) and compliance reports.

3) Log analysis and correlation (Agent: Analyst)
 - Implemented: Accepts `log_batch` events and simulates correlation into cases.
 - Next steps: connect to Elasticsearch/Graylog/Kafka; implement Sigma rules; build story view generation using graph models and timeline visualizations.

4) Email security & user training (Agent: Phalanx)
 - Implemented: Receives `email` events with `maliciousLinks` and creates cases.
 - Next steps: connect to mail gateway, build quarantine management, automated training enrollment workflows.

5) Privileged access monitoring (Agent: Warden)
 - Implemented: Monitors `privileged_action` events and detects suspicious combinations.
 - Next steps: integrate with IAM systems (Keycloak, cloud IAM), implement automated access reviews, and behavioral MFA triggers.

6) Multi-cloud security (Agent: Stratus)
 - Implemented: Accepts `cloud_scan` events and creates cases for public resources.
 - Next steps: integrate Prowler/ScoutSuite/TFSec outputs; provide one-click remediation (CLI/auto-remediate via cloud SDKs).

7) Endpoint protection (Agent: Sentinel)
 - Implemented: Accepts `endpoint` events (ransomwareSigns) and creates high-risk cases.
 - Next steps: integrate Velociraptor/Wazuh/EDR, implement automated containment (network isolation), and process-tree visualizations.

8) SOAR / Orchestration (Agent: Orchestrator)
 - Implemented: Listens for `case_created` events and simulates running a playbook (creates follow-up case entry as evidence).
 - Next steps: integrate with Shuffle/n8n/StackStorm or implement a playbook engine; build the drag-and-drop UI and telemetry for playbook performance.

UI/UX design & product principles
 - Unified & Intuitive: the repo includes a simple REST interface designed to be the backend for a single-pane UI.
 - Action-Oriented: Cases are created with titles, summaries and risk scores to support triage and one-click responses.
 - Visual & Contextual: the next step is adding a front-end (Vue/React) to visualize maps, timelines and story flows.
 - Role-Based Access: the backend is stateless and can be extended with Keycloak or another IdP for RBAC.

CI: GitHub Actions
 - This repo includes a workflow at `.github/workflows/ci.yml` that builds the project on push and packages a shaded JAR. It runs `mvn -DskipTests package` and uploads the artifact for debugging.

How to publish this repository to GitHub (two options)

Option A — Using git and web UI (manual remote creation):
```powershell
cd 'D:\softwares\prototype\canal-sample-drawings\multi-agent-security'
git init
git add .
git commit -m "Initial prototype: multi-agent security"
# create a repo on GitHub in the web UI, then add remote URL below
git remote add origin https://github.com/<your-org-or-username>/multi-agent-security.git
git branch -M main
git push -u origin main
```

Option B — Using GitHub CLI (if installed):
```powershell
gh repo create <your-org-or-username>/multi-agent-security --public --source=. --remote=origin --push
```

After push: GitHub Actions will run the `ci.yml` workflow and produce a build artifact. You can enable branch protection, add issues, and invite collaborators.

CI & GitHub Pages (automatic)
 - This repo contains a GitHub Actions workflow `.github/workflows/ci-and-deploy.yml` which:
	 1) builds the backend with Maven,
	 2) starts the server and runs smoke tests (scripts/smoke-test.sh),
	 3) deploys the static `ui-angular` folder to GitHub Pages using the Pages deploy action.

To enable the automatic UI deployment simply push this repository to GitHub (main branch). The workflow will build, run smoke tests and publish the UI to Pages (no paid services required). If the smoke test fails the deploy step is not executed.

Contributing
 - See `CONTRIBUTING.md` for guidelines.

Security & data handling
 - This is a prototype: it does not collect or transmit telemetry externally. Before connecting to production assets, add secure credentials management (Vault/Secrets), TLS for the HTTP server, RBAC and audit logging.

License
 - This repository is licensed under the MIT License (see `LICENSE`).

Contact / next steps
 - Tell me which connector(s) or UI choices you want next (e.g., Wazuh -> Hunter, Elasticsearch -> Analyst, Vue front-end). I can implement connectors, add persistence, or scaffold a UI and push the resulting changes into the repo for you to review and push to GitHub.

Frontend (local dev)
 - A Vite + Vue prototype is available in the `ui/` folder. To run the dev server:
```powershell
cd ui
npm install
npm run dev
```
 - To build static files (outputs to `ui-dist/`):
```powershell
cd ui
npm run build
```

AngularJS demo UI

Example (PowerShell):
```powershell
# From repository root
python -m http.server 8000 -d ui-angular
# or, if Node.js is installed
npx http-server ui-angular -p 8000

# Open http://localhost:8000
```

If your backend is running on `http://localhost:4567` the AngularJS UI will call the correct endpoints by default. If your backend runs elsewhere, edit `ui-angular/app.js` and set the `base` variable in the Api factory to the backend URL (e.g. `http://127.0.0.1:4567`).

