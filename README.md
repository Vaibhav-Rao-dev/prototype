AngularJS UI for Multi-Agent Security prototype

How to run locally

Prerequisites: any static file server (or open index.html in a browser).

Using PowerShell (recommended):

# From project root
# 1) Serve using Python simple http server (if Python installed):
python -m http.server 8000 -d ui-angular

# 2) Or use npx http-server (if Node.js installed):
npx http-server ui-angular -p 8000

Open http://localhost:8000

Notes
- The UI expects the backend API to be available on the same host ("/cases", "/events", "/playbooks"). If your backend runs on a different host/port, edit `ui-angular/app.js` and set `base` in the Api factory to the full backend URL.
- This is a minimal demo: the hunter view shows a static network map, a case list fetched from the API, and buttons to execute playbooks or run actions. Add more components to visualize telemetry, search, and playbook runs.

