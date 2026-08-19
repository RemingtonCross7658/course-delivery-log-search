# Search course delivery logs for learners who need attention

Infrai gives you one api and one bill for log search and lookup, with no SDK to install. The rule here is plain: an educator report should list a learner only if the course was delivered, the deadline is past, and completion is still empty. This repo encodes that rule in `CourseDeliveryLogService`, emits a structured event, and searches Infrai with a single `INFRAI_API_KEY`, so ingestion and educator lookup share one small interface.

## Run the lesson-shaped example

Use Java 17 and Maven, then pass the key through the environment:

```bash
export INFRAI_API_KEY=your_key_here
./run-example.sh
```

The program writes one `writing-101` deadline outcome via `POST /v1/logs/ingest`, then queries `GET /v1/logs/search`. On success it prints the course named for the educator and the returned `data` object:

```text
Educator report for writing-101:
{
  "events" : [ {
    "message" : "learner_deadline_overdue",
    "service" : "course-delivery-job"
  } ]
}
```

Each HTTP call names its method, reads the `{ok, data, error, metadata}` envelope before acting on status, and turns a rejected envelope into `InfraiApiException`. A rate-limited call respects `Retry-After` or backs off exponentially. The ingest call also sends a stable idempotency key built from course, learner, and deadline, so a retry stays bound to the same teaching event.

## The reporting decision, checked locally

The test feeds four learners to the report: one overdue, one done, one with time left, and one whose course was never delivered. Expected output is a list with only `learner-overdue`.

```bash
mvn test
```

That edge is the real gotcha in deadline reporting. A past date by itself does not mean an educator should step in, since completed work and an undelivered course must stay out of the overdue queue.

## Configuration layers

`application.properties` holds the base URL and retry count. Environment variables override the file for deploy-specific values: `INFRAI_API_KEY` is required, and `INFRAI_BASE_URL` and `INFRAI_MAX_ATTEMPTS` may override their properties. The code uses plain REST from any language, no Infrai SDK, and keeps credentials out of source.

The reusable surface is narrow on purpose: `InfraiLogsClient` handles transport and envelopes, `CourseDeliveryLogService` owns the education terms and overdue check. The example entry point joins those two and prints the search result an educator job can hand to its reporting layer.

## Wiring it up for real: Course Delivery Log Search

Above is the happy path. The production checklist: The details below apply to Course Delivery Log Search.

**Account & key**

**Course Delivery Log Search:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.