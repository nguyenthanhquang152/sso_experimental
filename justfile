# justfile — Unified task runner for sso_experimental
# Usage: just <recipe>
# Run `just` or `just --list` to see all available recipes.

set quiet

maven_repo := ".m2/repository"

# Show available recipes
[private]
default:
    just --list

# === Build ===

# Build backend + frontend
build: backend-build frontend-build

# Build backend (skip tests)
backend-build:
    cd backend && rtk mvn -Dmaven.repo.local=../{{ maven_repo }} package -DskipTests -q

# Build frontend
frontend-build:
    cd frontend && rtk npm run build

# === Test ===

# Run all tests
test: backend-test frontend-test

# Run backend tests
backend-test:
    cd backend && rtk mvn -Dmaven.repo.local=../{{ maven_repo }} test

# Fail fast when the local app stack is unavailable for Playwright E2E
frontend-test-preflight:
    rtk curl -fsS http://localhost:8000 >/dev/null 2>&1 || { \
        echo "Playwright E2E expects the local stack at http://localhost:8000, but it is not reachable."; \
        echo "Run 'just up', wait for services to be ready, then rerun 'just frontend-test' or 'just check'."; \
        exit 1; \
    }

# Run frontend E2E tests
frontend-test: frontend-test-preflight
    cd frontend && CI=1 PLAYWRIGHT_HTML_OPEN=never rtk npm run test:e2e -- --reporter=line

# === Lint ===

# Run all linters
lint: frontend-lint

# Run frontend ESLint
frontend-lint:
    cd frontend && rtk npm run lint

# === Validation (full) ===

# Run all validation: lint + test
check: lint test

# === Local Copilot CLI ===

# Run a local Copilot CLI prompt file with a specific agent (safe/default)
copilot-agent agent_id prompt_file:
    cat "{{prompt_file}}" | rtk copilot --agent "{{agent_id}}"

# Run a local Copilot CLI prompt file with the default agent (safe/default)
copilot-prompt prompt_file:
    cat "{{prompt_file}}" | rtk copilot

# Run a local Copilot CLI prompt file with a specific agent (autonomous)
copilot-agent-auto agent_id prompt_file:
    cat "{{prompt_file}}" | rtk copilot --agent "{{agent_id}}" --allow-all-tools --no-ask-user

# Run a local Copilot CLI prompt file with the default agent (autonomous)
copilot-prompt-auto prompt_file:
    cat "{{prompt_file}}" | rtk copilot --allow-all-tools --no-ask-user

# === Docker Compose ===

# Start Docker Compose stack
up:
    rtk docker compose up -d --build

# Stop Docker Compose stack
down:
    rtk docker compose down

# === Clean ===

# Clean build artifacts
clean:
    cd backend && rtk mvn clean -q
    cd frontend && rtk rm -rf dist node_modules/.vite
