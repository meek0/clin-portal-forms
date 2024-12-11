.ONESHELL:

# Docker
start:
	docker compose up -d $(c) --build

start_localstack:
	docker compose -f docker-compose.yml -f docker-compose-localstack.yml up -d $(c) --build

stop:
	docker compose down
