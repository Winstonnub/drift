# Drift — Project Master Document

> **What this file is.** A single source of truth for the Drift project. Hand this to any new coding agent (Claude, Cursor, etc.) as the first message so they know the product, the architecture, the constraints, **and especially how the human wants to be taught** (tutorial-style, no autonomous code generation). Read every section before writing a single line of code.

---

## 0. TL;DR for a New Agent

You are helping **Winston Liang** (3rd-year UofT CS+DS student, cGPA 3.95, incoming Citi Early Identification Program candidate) build a personal-learning side project called **Drift**.

The single most important thing about this engagement is the **learning methodology**: Winston writes all the code himself. You produce tutorial-style documentation that shows every line of code with full annotation and explanation of syntax + concepts, but Winston types it. You do not run the code, do not edit his files, do not "just fix it for him." See Section 9 for the exact rules.

The project is shaped to maximize learning of the **Citi senior-developer tech stack**: Java 21, Spring Boot 3, MongoDB, Spring Kafka, JWT/Spring Security, Docker, OpenShift-flavored deployment, Swagger, JMeter, SonarCloud (Checkmarx-style). See Section 6 for the exact stack-to-JD mapping.

---

## 1. Personal Context (Why This Project Exists)

- Winston was selected for the **Citi Early Identification Program (1 of 163 students in North America)**, which fast-tracks him to the final round of summer-2026 Citi interviews.
- The Citi team he's targeting is **Par Loan Trading in Fixed Income Trading Services** (Mississauga, ON). The senior-dev JD calls out: Java, Spring Boot, MongoDB, Kafka/Tibco/Solace, OpenShift, Docker, JMeter, Swagger, JIRA, Checkmarx, Oracle/MSSQL, Git, Agile.
- His mentor's prior Citi summer-analyst bullets emphasize: Spring Boot REST APIs, MongoDB, asynchronous workflows, Checkmarx vulnerability remediation, encryption, OpenShift, Harness, JMeter, Swagger, Postman.
- Winston's existing résumé is **strong on Python / ML / Next.js / AWS / full-stack** but has **zero Java / Spring / Kafka** experience. Drift exists to close that gap with a real, deployable product — not a toy simulation.
- Winston has limited time. Target scope: **one weekend for an MVP that real users can sign up to**, plus **~2 weeks of evenings** to layer in the rest of the Citi stack.

---

## 2. Product Overview

### 2.1 Elevator Pitch
Drift is daily micro-learning that comes to you. You tell it what you want to learn ("System Design", "Quant Finance", "Fixed Income Basics", "Rust Ownership Model") and how long you want to read each day (2 / 5 / 10 min). Every day at your chosen time, Drift delivers one new lesson — in the web app and optionally to your inbox — that picks up exactly where you left off. After reading, you tap one feedback button (Got it / Go deeper / Too basic / Confused / Skip ahead) and optionally type a follow-up question. Tomorrow's lesson adapts to that signal. Multiple "tracks" run in parallel.

Think: **Duolingo's daily cadence + Wikipedia's depth + TikTok's "this knows what I like" feel**, but for genuine intellectual topics, text + images only (no video).

### 2.2 Why It's Useful (and Why Winston Would Actually Use It)
- He's a CS+DS student preparing for a finance internship and wants daily exposure to fixed-income concepts.
- Friends in his program would sign up to learn system design, quant finance, ML interview prep, etc.
- It solves a real "I keep meaning to learn X but never block off the time" problem.
- Deployable: real URL, real signup, real users — not a simulation.

### 2.3 Core User Flows

1. **Sign up / log in** (email + password, JWT-secured).
2. **Create a track**: pick or write a topic, set target length (2/5/10 min), set delivery time + timezone, choose channels (web, email, both). On creation, the system generates a ~30-item syllabus outline for that topic via LLM.
3. **Daily delivery**: at the scheduled time, the system generates the next lesson for each active track and pushes it to the feed + email.
4. **Read a lesson** in the web app (markdown rendered, code blocks, optional inline images).
5. **Submit feedback** at the bottom of each lesson: one of {Got it ✓ / Go deeper / Too basic / Confused / Skip ahead} + optional free-text follow-up question.
6. **Manage tracks**: pause, resume, archive, view progress (X/30 lessons), or "give me today's lesson now."
7. **Browse past lessons** in a per-track archive.

### 2.4 Explicitly Out of Scope for V1
- Video, audio, TTS.
- Mobile native apps (PWA is fine if time allows).
- Social features (sharing, comments, public profiles).
- Payments / subscriptions.
- Anything multi-tenant beyond per-user isolation.

---

## 3. Why This Project Is the Right Shape for Citi

Every architectural decision below is dual-purpose: it makes the product work **and** it directly maps to a Citi senior-dev JD line item. When the interviewer asks "why did you build it this way?" Winston needs to be able to answer in product-engineering language, not "to put it on my résumé."

| Citi JD requirement | How Drift exercises it (and why the use is honest) |
|---|---|
| Java / Spring Boot REST APIs | Entire backend |
| MongoDB | Five collections; document-shaped reads (track + syllabus + recent lessons in one query) genuinely fit |
| Real-time messaging (Kafka / Tibco / Solace) | Async LLM jobs decoupled from HTTP requests; daily scheduler fan-out; feedback event stream |
| JWT / Spring Security | Auth across all protected endpoints, with refresh-token rotation |
| Docker / containerization | Multi-stage builds; docker-compose for local stack |
| Swagger / OpenAPI | springdoc-openapi auto-generates docs |
| JMeter load testing | Load test the feed endpoint and feedback POST |
| Checkmarx / static analysis | SonarCloud in GitHub Actions CI |
| Agile / Jira | Track work in a public GitHub Projects board, weekly self-standups in README |
| Git source control | Whole project is a public repo with clean commit history |
| **Fixed Income / Par Loan domain knowledge (nice-to-have)** | **One of the demo tracks is literally "Fixed Income & Par Loans" — the interviewer reading the demo learns about their own desk** |

The Oracle/MSSQL line item is intentionally **not** addressed — MongoDB is the genuinely better fit for this data, and "I chose Mongo because the content shape is flexible and reads are document-shaped" is a stronger interview answer than shoehorning Postgres in.

---

## 4. Architecture

### 4.1 High-Level Topology

```
                                    ┌──────────────────────────┐
                                    │   Next.js Frontend       │
                                    │   (Vercel)               │
                                    └────────────┬─────────────┘
                                                 │ HTTPS / JWT
                                                 ▼
                                    ┌──────────────────────────┐
                                    │   Spring Boot Monolith   │
                                    │   (Railway)              │
                                    │                          │
                                    │   ┌──────────────────┐   │
                                    │   │ Controllers      │   │
                                    │   │ Services         │   │
                                    │   │ Repositories     │   │
                                    │   │ Kafka producers/ │   │
                                    │   │   listeners      │   │
                                    │   │ Scheduler        │   │
                                    │   └──────────────────┘   │
                                    └──┬─────────┬─────────┬───┘
                                       │         │         │
                       ┌───────────────┘         │         └───────────────┐
                       ▼                         ▼                         ▼
              ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
              │  MongoDB Atlas   │    │  Kafka (Railway  │    │  Anthropic API   │
              │  (free M0)       │    │  or Upstash)     │    │  (LLM calls)     │
              └──────────────────┘    └──────────────────┘    └──────────────────┘
                                                                       │
                                                              ┌────────┴────────┐
                                                              │  Resend API     │
                                                              │  (email)        │
                                                              └─────────────────┘
```

**One Spring Boot app, not microservices.** Microservices for this scope would be résumé theater and slow you down. Use clean package boundaries (`auth`, `tracks`, `syllabus`, `lessons`, `feedback`, `delivery`, `llm`, `email`, `common`) so the *option* to split later is preserved. If the interview conversation goes there, "I kept it a modular monolith because the team is one person and the request volume doesn't justify the operational cost of separate services" is a senior-engineer answer.

### 4.2 MongoDB Collections

Five collections. The fact that each has a different access pattern is itself a talking point on schema design.

**`users`**
- `_id` (ObjectId), `email` (indexed, unique), `passwordHash` (bcrypt), `displayName`, `timezone`, `defaultDeliveryTime` (e.g. "08:00"), `defaultChannels` (["web", "email"]), `refreshTokens` (array of {token, issuedAt, revokedAt}), `createdAt`, `updatedAt`.

**`tracks`**
- `_id`, `userId` (indexed), `topic` (string, user-supplied), `targetMinutes` (2|5|10), `status` ("active"|"paused"|"archived"), `deliveryTime`, `timezone`, `channels`, `nextDeliveryAt` (UTC, indexed), `syllabusPointer` (int — next item index to deliver), `createdAt`.
- Compound index: `(status, nextDeliveryAt)` so the scheduler query is fast.

**`syllabi`**
- One doc per track. `_id`, `trackId` (indexed, unique), `items` (array of `{ index, title, summary, prerequisites, status: "pending"|"delivered"|"skipped" }`), `version` (int — bumped when "go deeper" detours mutate the outline), `generatedAt`.
- Stored as a single document because it's always read whole.

**`lessons`**
- `_id`, `trackId` (indexed), `userId` (indexed), `syllabusItemIndex`, `title`, `contentMarkdown` (the actual lesson), `estimatedMinutes`, `deliveredAt` (indexed DESC), `readAt` (nullable), `feedback` (embedded subdoc: `{ rating, freeText, submittedAt }`).
- Compound index: `(trackId, deliveredAt DESC)` for feed queries.

**`feedback_events`** (append-only)
- `_id`, `userId`, `trackId`, `lessonId`, `signal` ("got_it"|"go_deeper"|"too_basic"|"confused"|"skip_ahead"), `freeText` (nullable), `createdAt`.
- Why separate from the embedded `feedback` on `lessons`? Because future analytics queries ("show me all `go_deeper` events across users for topic X") want an append-only event log, not a join across embedded subdocs. Honest design choice, not over-engineering.

### 4.3 Kafka Topics

Four topics. Each one exists for a real reason. The Citi interviewer will ask "why Kafka?" — these are the answers.

**`syllabus.generate`**
- *Producer:* `TrackController` after a track is created.
- *Consumer:* `SyllabusGenerationListener` — calls Anthropic API (10–30s), writes to `syllabi`, marks track ready.
- *Why Kafka and not a synchronous call?* LLM calls are slow and flaky. Don't block the POST `/tracks` HTTP request. Also: retry-on-failure semantics for free.

**`lesson.generate`**
- *Producer:* (a) the daily `@Scheduled` cron, one message per due track; (b) manual "generate now" button.
- *Consumer:* `LessonGenerationListener` — picks next syllabus item, factors in last 3 feedback signals, calls LLM, writes to `lessons`, optionally produces a `lesson.deliver.email` message.
- *Why Kafka?* Same reason as above, plus clean fan-out when 1000 tracks are due at 08:00 — they queue up and consumers process in parallel, instead of 1000 simultaneous LLM calls.

**`lesson.deliver.email`**
- *Producer:* `LessonGenerationListener` (if user opted into email).
- *Consumer:* `EmailDeliveryListener` — calls Resend API.
- *Why a separate topic?* Email delivery failures must not block lesson generation. Independent retry semantics. Dead-letter queue for permanently-failed sends.

**`feedback.submitted`**
- *Producer:* `FeedbackController`.
- *Consumer:* `FeedbackProcessingListener` — appends to `feedback_events`, advances the syllabus pointer, on "go_deeper" inserts a detour syllabus item (bumping `syllabi.version`).
- *Why Kafka?* The HTTP response should return immediately ("got it, thanks"). The downstream curriculum mutation can be async. Also: an event log is the natural shape for feedback analytics.

All consumers use **consumer groups** for parallelism and **dead-letter topics** (`<topic>.dlq`) for poison messages.

### 4.4 Scheduler

A single `@Scheduled(cron = "0 */5 * * * *")` method runs every 5 minutes:
1. Queries `tracks` where `status = "active"` AND `nextDeliveryAt <= now()`.
2. For each match, publishes a `lesson.generate` Kafka message.
3. Updates the track's `nextDeliveryAt` to tomorrow at the user's `deliveryTime` in their timezone.

This handles all timezones cleanly because `nextDeliveryAt` is stored in UTC and recalculated per-user on each delivery.

### 4.5 Prompt Strategy (Brief)

- **Syllabus generation prompt** asks the LLM to return a JSON array of 30 items, each with title, 1-sentence summary, prerequisites. Use structured-output prompting and validate with Jackson against a schema before persisting.
- **Lesson generation prompt** receives: the topic, the target minutes, the current syllabus item, the user's last 3 feedback signals, and any "go deeper" detour. Asks for markdown output bounded to a word count derived from target minutes (~200 words/min reading speed).
- **All prompts** version-controlled in `src/main/resources/prompts/` so changes are reviewable.

---

## 5. The Tech Stack (Exhaustive)

### 5.1 Backend
- **Language:** Java 21 (LTS, modern syntax — records, pattern matching, virtual threads available)
- **Framework:** Spring Boot 3.2+ (Jakarta EE namespace, not javax)
- **Build:** Maven (more common at banks than Gradle)
- **Persistence:** Spring Data MongoDB
- **Security:** Spring Security 6 + JWT (jjwt-api / jjwt-impl / jjwt-jackson)
- **Messaging:** Spring Kafka
- **Scheduling:** Spring `@Scheduled`
- **HTTP client (LLM, Resend):** Spring `RestClient` (synchronous, simple)
- **Validation:** Jakarta Bean Validation (`@Valid`, `@NotBlank`, etc.)
- **Logging:** SLF4J + Logback (default)
- **API docs:** springdoc-openapi (Swagger UI at `/swagger-ui.html`)
- **Mapping:** MapStruct (DTO ↔ entity) or plain manual mappers if MapStruct feels heavy
- **Boilerplate reduction:** Lombok
- **Testing:** JUnit 5, Mockito, Testcontainers (real MongoDB + Kafka in tests), AssertJ
- **Coverage:** JaCoCo

### 5.2 Frontend
- **Framework:** Next.js 14+ (App Router)
- **Styling:** Tailwind CSS + shadcn/ui
- **Markdown rendering:** `react-markdown` + `remark-gfm` + `rehype-highlight`
- **Auth client:** plain `fetch` with JWT in `Authorization` header, refresh token in httpOnly cookie

### 5.3 Infrastructure
- **MongoDB:** Atlas free M0 cluster
- **Kafka:** Railway Kafka template (or Upstash Kafka if Railway gets fussy on free tier)
- **Email:** Resend (3000 free emails/month, no credit card)
- **LLM:** Anthropic API (Claude Sonnet for production lessons, Haiku for cheaper syllabus generation)
- **Backend host:** Railway (free $5/month credit, deploys Spring Boot from GitHub directly)
- **Frontend host:** Vercel (free hobby tier)
- **CI:** GitHub Actions
- **Static analysis:** SonarCloud (free for public repos)
- **Load testing:** JMeter (local install)
- **IDE:** VSCode with extensions: Extension Pack for Java, Spring Boot Extension Pack, MongoDB for VS Code, Docker
- **Monitoring:** Spring Boot Actuator endpoints + Railway built-in metrics (skip Grafana for V1)

### 5.4 Why Not...
- **Postgres / MySQL:** Mongo is a better fit for flexible content, and Citi uses Mongo per the mentor.
- **Gradle:** Maven is more common at banks.
- **Microservices:** Operational overhead not justified at this scope; modular monolith preserves the option to split.
- **GraphQL:** REST is what Citi uses; don't fight the JD.
- **NextAuth / Auth0:** Hand-rolling Spring Security JWT is the *point* — that's the Citi skill.

---

## 6. Resume Bullets (Templates)

Fill in real numbers after building. Do not invent numbers. Each bullet is calibrated for FAANG / bank standards (lead with action verb, specific tech, quantified outcome).

- *Built and deployed Drift, a personalized micro-learning platform (Java 21 / Spring Boot 3 + MongoDB + Kafka + Next.js), serving N active users across X topic tracks with adaptive daily LLM-generated lessons.*
- *Designed an event-driven content-generation pipeline with Spring Kafka — decoupling 10–30s LLM API calls from synchronous HTTP requests using 4 topics, consumer-group parallelism, and dead-letter queues for poison messages — reducing p95 track-creation API latency from ~20s to <200ms.*
- *Modeled the domain across 5 MongoDB collections with compound indexes supporting feed queries under X ms p95, and implemented adaptive curriculum sequencing driven by `feedback.submitted` events that dynamically mutate user syllabi.*
- *Implemented stateless JWT authentication with refresh-token rotation, BCrypt password hashing, Bean-Validation-guarded REST endpoints, and a Spring Security filter chain protecting N endpoints across the API.*
- *Auto-generated OpenAPI 3.0 documentation with springdoc, achieving 100% endpoint coverage in Swagger UI for downstream API consumers.*
- *Achieved ~85% line coverage with JUnit 5, Mockito, and Testcontainers-backed integration tests spinning real MongoDB and Kafka instances per test class.*
- *Hardened deployment with multi-stage Docker builds, GitHub Actions CI running SonarCloud static analysis on every PR, and JMeter load tests validating Y RPS on the feed endpoint with p99 latency under Z ms.*

When talking through these in an interview, **always tie the choice back to a real product reason**, never to "to learn the tech." The right framing is: "I chose Kafka because LLM latency would have made track creation feel broken — pushing it async with a queue dropped the response time from 20 seconds to under 200ms."

---

## 7. Phased Build Plan

Each phase has a clear "done" criterion and a corresponding CLAUDE.md tutorial doc (Section 8 explains the format).

### Phase 0 — Local Environment Setup
Install: JDK 21 via SDKMAN, Maven, Docker Desktop, VSCode (with extensions: Extension Pack for Java, Spring Boot Extension Pack, MongoDB for VS Code, Docker), MongoDB Compass, Node 20, pnpm. Verify each. Create the GitHub repo (public) and Railway / Vercel / Atlas / Resend / Anthropic / SonarCloud accounts.
**Done when:** `java -version` shows 21, `mvn -v` works, Docker can run hello-world, the empty repo is pushed.

### Phase 1 — First Spring Boot Endpoint
Generate the project at start.spring.io with dependencies: Web, Lombok, Validation, DevTools. Walk through every file: `pom.xml`, `Application.java`, `application.yml`. Write a `HealthController` with one GET endpoint. Run locally with `./mvnw spring-boot:run`. Hit it with curl.
**Done when:** `curl localhost:8080/health` returns `{"status":"ok"}`.

### Phase 2 — MongoDB Integration
Add `spring-boot-starter-data-mongodb`. Run Mongo via docker-compose. Define the `Track` `@Document`. Create `TrackRepository extends MongoRepository`. Build POST `/tracks` and GET `/tracks/{id}` (no auth yet). Use Compass to verify.
**Done when:** You can POST a track and see it in Compass.

### Phase 3 — Auth: JWT + Spring Security
Add `spring-boot-starter-security`, jjwt, BCrypt. Build `User` document, `AuthController` (register, login, refresh), JWT generation/validation, `SecurityFilterChain`, custom `OncePerRequestFilter` for JWT. Protect the `/tracks` endpoints. Refresh tokens stored on the user doc.
**Done when:** Register → login → use the access token to hit `/tracks` → use refresh token to get a new access token. All flows tested via curl.

### Phase 4 — Anthropic Integration (Synchronous First)
Add `RestClient` config. Write `LlmService.generateSyllabus(topic)`. Define request/response DTOs. Use a strict JSON-mode prompt. Validate the response with Jackson. POST `/tracks` now blocks while generating the syllabus (this is intentionally bad — Phase 5 fixes it, and the before/after is an interview story).
**Done when:** POSTing a track returns a 30-item syllabus stored in Mongo.

### Phase 5 — Kafka: Make It Async
Add Kafka to docker-compose. Add `spring-kafka`. Define the 4 topics. Refactor syllabus generation: POST `/tracks` returns 202 Accepted immediately, publishes a `syllabus.generate` message; `SyllabusGenerationListener` consumes and writes. Add a GET `/tracks/{id}/syllabus-status` endpoint for the frontend to poll. Add a DLQ for failed messages.
**Done when:** POST `/tracks` returns in <200ms, and the syllabus appears in Mongo seconds later.

### Phase 6 — Lessons + Scheduler
Define `Lesson` document and `LessonRepository`. Write `LessonGenerationListener` that consumes `lesson.generate`. Write the `@Scheduled` cron that publishes due tracks. Add timezone handling. Add manual "generate now" endpoint.
**Done when:** A test track set to deliver "1 minute from now" produces a lesson automatically.

### Phase 7 — Email Delivery
Add Resend client. Define `EmailDeliveryListener` consuming `lesson.deliver.email`. HTML email template with the lesson content + a deep-link back to the web app.
**Done when:** You receive an actual email in your inbox.

### Phase 8 — Feedback Loop
Build feedback endpoint. Publish to `feedback.submitted`. `FeedbackProcessingListener` advances the syllabus pointer and on `go_deeper` inserts a detour item. Verify next-day lesson reflects the feedback.
**Done when:** Submitting "go deeper" causes tomorrow's lesson to elaborate on today's topic.

### Phase 9 — Frontend
Next.js app: auth pages, tracks dashboard, create-track form, lesson reader (markdown + feedback widget), lessons archive. Deploy to Vercel.
**Done when:** A friend can sign up at your real URL and use it end-to-end.

### Phase 10 — Testing
Unit tests with Mockito for services. Integration tests with Testcontainers (MongoDB + Kafka). Hit ~85% line coverage. Generate JaCoCo report.
**Done when:** `mvn test` is green and the JaCoCo report shows the target coverage.

### Phase 11 — Docker, CI, Static Analysis, Load Test
Multi-stage Dockerfile. docker-compose for the full local stack. GitHub Actions workflow: build → test → SonarCloud scan → deploy on merge to main. JMeter test plan against the deployed instance. Add Swagger UI link to the README.
**Done when:** Push-to-deploy works end-to-end, SonarCloud quality gate passes, JMeter shows acceptable RPS.

**Stretch (if time allows):** PWA, public track templates, streak tracking, OpenTelemetry tracing, Prometheus + Grafana dashboards.

---

## 8. CLAUDE.md Format (How Tutorial Docs Should Be Written)

Each phase gets one CLAUDE.md file (e.g., `CLAUDE_phase_03_jwt_auth.md`). The agent writes these. Winston reads and types. Every doc follows this structure:

### 8.1 Document Skeleton

```
# Phase N — <Title>

## What you'll have at the end
<One paragraph + a "done when" criterion>

## Prerequisites
<What must work from prior phases. List the curl commands to verify.>

## Concepts you'll meet (skim before coding)
<For each new concept: 2–4 sentences. What it is. Why it exists. Where in this phase you'll see it. No code yet.>

## The build, step by step

### Step 1 — <smallest possible incremental change>
**What we're doing:** <one sentence>
**Why:** <one or two sentences>

**File:** `path/to/File.java` (NEW | EDIT)

```java
// Every line shown. Every line annotated where syntax is non-obvious.
package com.drift.tracks;          // <- package = directory; must match folder path

import lombok.Data;                 // <- Lombok generates getters/setters at compile time
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tracks")                 // <- maps this class to the "tracks" Mongo collection
@Data                               // <- Lombok: getters, setters, toString, equals, hashCode
public class Track {

    @Id                             // <- marks the Mongo _id; type ObjectId by default
    private String id;

    private String topic;
    // ... etc
}
```

**Type this yourself.** Don't paste. The typing is part of the learning.

**Verify:** <a concrete command to run + expected output>

### Step 2 — ...
```

### 8.2 Hard Rules for the Agent Producing These Docs

1. **Never run code on Winston's behalf.** No `bash` commands that compile or execute his app. He runs everything himself.
2. **Never edit his files.** Even if he pastes broken code, respond by *explaining the fix in the doc / chat* and pointing him at the line. He types the correction.
3. **Show every line.** No `// ... rest as before` ellipsis in tutorial code blocks. The whole file every time it changes meaningfully.
4. **Annotate every non-obvious token.** Annotations go in inline comments OR in a numbered "Notes on the code above" list directly under the block. The bar for "non-obvious" is: would a strong Python dev who's never touched Java need to think for >5 seconds? If yes, annotate it.
5. **Explain *why* before *how*.** Each step opens with one or two sentences of motivation before any code appears.
6. **One concept per step.** If you're tempted to introduce JWT *and* refresh tokens *and* the filter chain in one step, split it.
7. **End every step with a verification command.** A curl, a test, a Compass screenshot to take. Winston needs to confirm green before moving on.
8. **Concept boxes for landmines.** When something will bite him later if he doesn't internalize it (e.g., "Spring's `@Transactional` doesn't work on self-calls"), put it in a `> ⚠️ Gotcha:` block.
9. **No "leave this as an exercise."** If a piece of code is needed, write it. Exercises go in an explicit `## Practice (optional)` section at the end of the doc.
10. **Link to authoritative docs**, not random blogs. Spring docs, Mongo docs, Anthropic API reference, RFC 7519 for JWT, etc.
11. **Show the *bad* version first when it teaches something.** Phase 4 deliberately builds a blocking POST endpoint so Phase 5 can demonstrate why Kafka. Don't skip the bad version.
12. **Keep blocks copy-pasteable in case Winston is stuck**, but tell him every time: *the typing is the point*.

### 8.3 What "Tutorial-Style" Does NOT Mean

- Not a wall of theory. Concepts come in 2–4-sentence chunks, then immediately into code.
- Not handholding past the point of usefulness. By Phase 8 Winston should know what `@Service` means; don't re-explain it.
- Not toy code. Every line written is line that ships to the real app.

---

## 9. Interaction Rules for the Agent

When Winston messages you, you behave like this:

- **He asks "explain X":** explain. Use small examples. Don't dump a whole tutorial unless he asked for one.
- **He pastes an error:** diagnose, point at the exact line, explain the root cause, *tell him what to change*, do not change it.
- **He says "write the code for me":** decline gently. Remind him that the point of this project is to learn by typing, and offer to walk him through the section he's stuck on instead.
- **He asks for the next phase's CLAUDE.md doc:** produce the full document following Section 8.
- **He asks for an architectural decision:** lay out the trade-offs, recommend one, give one or two sentences of why. Don't hedge endlessly.
- **He asks how something maps to Citi:** be specific. Quote the JD line item and explain the mapping.
- **He asks for résumé-bullet wording:** use the templates in Section 6 as starting points; fill in only numbers he's actually measured.

**Do not:** invent metrics, fabricate Citi internal details, write code into his files autonomously, or skip steps in tutorials to be "more efficient."

---

## 10. Default Topics for Initial Tracks (Demo Content)

When Winston demos this in the Citi interview, these tracks should already exist and have a few delivered lessons so the interface looks lived-in:

1. **Fixed Income & Par Loans** ← *the interview move*
2. **System Design Fundamentals**
3. **Quant Finance Concepts**
4. **Java Concurrency Deep Dive**
5. **Kafka in Production**

Tracks 4 and 5 are double-duty: they're lessons *for Winston* on the actual Citi tech stack, and they're proof to the interviewer that Drift solves a real problem for its creator.

---

## 11. Glossary (Citi / Fixed-Income Terms Worth Knowing)

Quick reference so Winston isn't bluffing in the interview:

- **Par Loan:** A syndicated loan trading at or near par (face value, 100). Distinct from "distressed" loans trading at a discount. Citi's Par Loan Trading desk makes markets in these.
- **CLO (Collateralized Loan Obligation):** A securitization vehicle that buys pools of leveraged loans (often par loans) and issues tranched debt against them. Major buyers of par loans.
- **Fixed Income:** Asset class of debt instruments — bonds, loans, notes, securitizations. Pays fixed or floating coupons.
- **OTC (Over-the-Counter):** Trades negotiated bilaterally, not on an exchange. Loans trade OTC. Hence the need for internal trading systems rather than reliance on exchange infrastructure.
- **T+N Settlement:** Trade date plus N business days until settlement. Loans historically settle T+7 or longer (operationally messy — hence demand for sophisticated post-trade systems, which is what the JD's "applications systems analysis and programming" implies).
- **Checkmarx:** Static application security testing (SAST) tool widely used at banks for code vulnerability scanning. SonarCloud's quality gate is the open-source-stack equivalent.
- **Tibco EMS / Solace:** Enterprise messaging systems used at banks instead of (or alongside) Kafka. Concepts transfer directly.
- **OpenShift:** Red Hat's enterprise Kubernetes distribution, the bank-flavored container platform. Skills from Docker + Kubernetes transfer.
- **Harness:** A CI/CD platform (alternative to Jenkins / GitHub Actions) used at Citi for deployment pipelines.

---

## 12. Definition of "Done"

The project is finished — for the purposes of putting it on the résumé and demoing it at Citi — when **all** of the following are true:

- [ ] Public GitHub repo with clean commit history and a README that includes architecture diagram, live demo URL, and "how to run locally."
- [ ] Deployed and accessible at a real URL (e.g., `drift.winstonl.vercel.app` or similar).
- [ ] At least 5 real users (friends count) have signed up and received at least one lesson.
- [ ] All 11 phases complete, including SonarCloud green quality gate and a JMeter report.
- [ ] Swagger UI is live and documents every endpoint.
- [ ] The "Fixed Income & Par Loans" demo track has at least 5 generated lessons.
- [ ] Winston can speak fluently for 3 minutes about each architectural choice without notes.

That last item is the real one. The repo is the artifact; the fluency is the deliverable.

---

---

## 13. Hour Estimation

Assumes you are learning Java / Spring from scratch. All other tools (Next.js, Docker, APIs) are already familiar.

| Phase | Topic | Estimate |
|---|---|---|
| 0 | Environment setup | 1–2h |
| 1 | First Spring Boot endpoint | 2–3h |
| 2 | MongoDB integration | 2–3h |
| 3 | JWT + Spring Security | 4–6h ← steepest learning curve |
| 4 | Anthropic integration (sync) | 2–3h |
| 5 | Kafka — async refactor | 4–6h ← second hardest |
| 6 | Lessons + Scheduler | 2–3h |
| 7 | Email delivery | 1–2h |
| 8 | Feedback loop | 2–3h |
| 9 | Frontend (Next.js) | 4–6h (fast — you know Next) |
| 10 | Testing (JUnit + Testcontainers) | 3–4h |
| 11 | Docker, CI, SonarCloud, JMeter | 3–4h |
| **Total** | | **~30–45 hours** |

### Translated to a realistic schedule

- **Weekend 1** (Sat + Sun, ~8h/day): Phases 0–4. Working MVP, synchronous, deployed. Friends can sign up.
- **Week 2 evenings** (~2h/night × 5): Phases 5–8. Full Citi stack in place.
- **Weekend 2** (~6–8h total): Phase 9 (frontend polish) + Phase 10 (testing).
- **A few evenings after**: Phase 11 (CI, Docker, JMeter).

**~3 weeks total** if you're disciplined. Phases 3 and 5 are the most likely to blow the estimate — budget an extra session for each.

The MVP — something real at a real URL — is end of Weekend 1.

---

*End of master doc. Hand this to the next agent. Then start with Phase 0.*
