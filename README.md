# Drift

Drift is a deployed micro-learning platform that creates adaptive daily lessons from a user-selected topic. Users register, create learning tracks, receive AI-generated syllabi and lessons, and submit feedback that shapes future learning.

![Drift landing page](docs/landing.png)

## Live Demo

- Frontend: https://driftlearning.vercel.app
- Backend health: https://api.winstonl.dev/health
- API docs: https://api.winstonl.dev/swagger-ui/index.html

## What It Demonstrates

- Full-stack product flow: authentication, track creation, AI syllabus generation, lesson generation, and feedback.
- Event-driven backend: Kafka handles slower background work such as syllabus generation, lesson generation, email delivery, and feedback processing.
- Production-style delivery: Dockerized Spring Boot backend on AWS EC2, Next.js frontend on Vercel, CI with GitHub Actions, static analysis with SonarCloud, and load testing with JMeter.

## Architecture

```text
Browser
  -> Next.js frontend on Vercel
  -> Spring Boot API on AWS EC2
      -> MongoDB container
      -> Kafka container
      -> Anthropic API
      -> Resend API
```

The AWS server runs Caddy in front of the backend so the API is available over HTTPS. MongoDB and Kafka are private Docker services inside the EC2 host.

## Tech Stack

| Area | Technology |
|---|---|
| Frontend | Next.js, React, TypeScript |
| Backend | Java 21, Spring Boot, Spring Security |
| Data | MongoDB |
| Messaging | Kafka |
| AI | Anthropic API |
| Email | Resend |
| Infrastructure | Docker, Docker Compose, AWS EC2, Caddy, Vercel |
| Quality | JUnit, Mockito, Testcontainers, JaCoCo, GitHub Actions, SonarCloud, JMeter |

## Validation Snapshot

- Backend test suite: 12 tests passing.
- Frontend checks: `pnpm lint` and `pnpm build` passing.
- Load test: 200 authenticated requests, 0 errors, 363.5 ms average response time, 858.8 ms 95th percentile, 8.9 transactions/sec.

## Local Setup

### Prerequisites

Install:

- Java 21
- Docker Desktop
- Node.js 20+
- pnpm

Optional:

- JMeter, only for load testing

### 1. Clone the repo

```bash
git clone https://github.com/Winstonnub/drift.git
cd drift
```

### 2. Create backend environment variables

Copy the sample env file:

```bash
cp .env.example .env
```

Edit `.env`:

```text
APP_JWT_SECRET=replace-with-output-from-openssl-rand-base64-48
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
APP_RESEND_APP_URL=http://localhost:3000
ANTHROPIC_API_KEY=replace-with-your-anthropic-api-key
RESEND_API_KEY=replace-with-your-resend-api-key
```

Generate a local JWT secret:

```bash
openssl rand -base64 48
```

Where to get API keys:

- Anthropic: create an API key in the Anthropic Console.
- Resend: create an API key in the Resend dashboard. Email delivery is optional for local testing unless you enable email lessons.

### 3. Start MongoDB and Kafka

```bash
docker compose up -d
```

This starts:

- MongoDB on `localhost:27017`
- Kafka on `localhost:9092`

The backend default config already points to these local services:

```text
MongoDB: mongodb://drift:drift@localhost:27017/drift?authSource=admin
Kafka: localhost:9092
```

### 4. Start the backend

```bash
set -a
source .env
set +a

cd backend
./mvnw spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/health
```

Expected:

```json
{"status":"ok"}
```

API docs:

```text
http://localhost:8080/swagger-ui.html
```

### 5. Start the frontend

In a second terminal:

```bash
cd frontend
pnpm install
echo "NEXT_PUBLIC_API_URL=http://localhost:8080" > .env.local
pnpm dev
```

Open:

```text
http://localhost:3000
```

## Running the Full Stack in Docker

To run MongoDB, Kafka, and the backend together:

```bash
set -a
source .env
set +a

docker compose -f docker-compose.full.yml up --build
```

Then run the frontend separately:

```bash
cd frontend
pnpm dev
```

## Testing

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
pnpm lint
pnpm build
```

Coverage report after backend tests:

```text
backend/target/site/jacoco/index.html
```

## Load Testing

The JMeter plan is stored at:

```text
load-tests/drift-login-tracks.jmx
```

The checked-in plan targets the deployed API and uses a test login flow. For local testing, update the HTTP Request Defaults in JMeter to:

```text
Protocol: http
Server Name or IP: localhost
Port: 8080
```

Run:

```bash
jmeter -n \
  -t load-tests/drift-login-tracks.jmx \
  -l load-tests/results/drift-login-tracks.jtl \
  -e \
  -o load-tests/results/html
```

Generated load-test output is intentionally ignored by Git.

## Deployment Overview

- Frontend: Vercel
- Backend: AWS EC2
- HTTPS reverse proxy: Caddy
- Database: MongoDB running in Docker on EC2
- Messaging: Kafka running in Docker on EC2

AWS deployment files:

```text
docker-compose.aws.yml
ops/Caddyfile
ops/aws.env.example
```

For production, the EC2 `.env.aws` file should include:

```text
DRIFT_API_DOMAIN=api.your-domain.com
MONGO_USERNAME=drift
MONGO_PASSWORD=replace-with-a-long-random-password
APP_JWT_SECRET=replace-with-output-from-openssl-rand-base64-48
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,https://*.vercel.app
APP_RESEND_APP_URL=https://your-vercel-app.vercel.app
ANTHROPIC_API_KEY=replace-with-your-anthropic-api-key
RESEND_API_KEY=replace-with-your-resend-api-key
```

The frontend deployment needs:

```text
NEXT_PUBLIC_API_URL=https://api.your-domain.com
```

## Notes

This is a portfolio deployment, not a high-availability production system. The current AWS setup prioritizes clarity and cost control by running the backend, MongoDB, Kafka, and reverse proxy on one EC2 instance. A larger production version would split these services, add managed backups, monitoring, alerting, and stronger infrastructure isolation.
