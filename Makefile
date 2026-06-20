# Convenience targets for the local stack.

.PHONY: infra-up up down logs pull-models

# Just the datastores + Ollama. Works on day one, before app code exists.
infra-up:
	docker compose up -d postgres qdrant redis ollama

# Everything (needs web/ and api/ scaffolded by the Dev agent first).
up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

# Pull the local models into the running Ollama container.
pull-models:
	docker compose exec ollama ollama pull llama3.2
	docker compose exec ollama ollama pull nomic-embed-text
