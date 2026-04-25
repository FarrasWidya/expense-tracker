.DEFAULT_GOAL := help
.PHONY: run dev stop kill db-up db-down db-reset docker-build test build clean fmt fmt-check ps help

APP_PORT := 8080

# ── local dev ────────────────────────────────────────────────────────────────

run: db-up  ## Start DB + app (foreground — Ctrl+C to stop the app)
	./mvnw spring-boot:run

dev: run  ## Alias for run

stop: kill db-down  ## Kill the app and stop the DB container

kill:  ## Kill the JVM listening on :$(APP_PORT)
	@lsof -ti :$(APP_PORT) | xargs kill -9 2>/dev/null \
		&& echo "killed :$(APP_PORT)" || echo "nothing on :$(APP_PORT)"

ps:  ## Show process(es) on :$(APP_PORT)
	@lsof -ti :$(APP_PORT) | xargs -I{} ps -p {} -o pid,command 2>/dev/null \
		|| echo "nothing on :$(APP_PORT)"

# ── database / docker ────────────────────────────────────────────────────────

db-up:  ## Start the PostgreSQL container
	docker compose up -d db

db-down:  ## Stop containers (data volume kept)
	docker compose down

db-reset:  ## Destroy data volume and restart DB (⚠ wipes all local data)
	docker compose down -v
	docker compose up -d db

docker-build:  ## Build the Docker image locally
	docker compose build app

# ── maven ─────────────────────────────────────────────────────────────────────

test:  ## Run tests — uses H2 in-memory, no Docker needed
	./mvnw test

build:  ## Compile and package a jar (skips tests)
	./mvnw package -DskipTests

clean:  ## Remove target/
	./mvnw clean

# ── formatting ────────────────────────────────────────────────────────────────

fmt:  ## Auto-format index.html with Prettier
	npx --yes prettier --write src/main/resources/static/index.html

fmt-check:  ## Check index.html formatting without writing (CI-safe)
	npx --yes prettier --check src/main/resources/static/index.html

# ── help ──────────────────────────────────────────────────────────────────────

help:  ## Show this help
	@grep -E '^[a-z_-]+:.*##' Makefile \
		| awk 'BEGIN {FS = ":.*##"}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'
