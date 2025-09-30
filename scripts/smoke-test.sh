#!/usr/bin/env bash
set -euo pipefail

echo "Running smoke tests against http://localhost:4567"

AGENTS=$(curl -sS http://localhost:4567/agents)
echo "Agents: $AGENTS"

CASES=$(curl -sS http://localhost:4567/cases)
echo "Cases: ${CASES:0:200}..."

echo "Smoke tests passed"
