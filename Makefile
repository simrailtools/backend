down_dev:
	docker compose -f .local/docker-compose.yml down -v

pull_dev:
	docker compose -f .local/docker-compose.yml pull

up_dev: down_dev pull_dev
	docker compose -f .local/docker-compose.yml up

up_dev_d: down_dev pull_dev
	docker compose -f .local/docker-compose.yml up -d
