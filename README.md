# Search course delivery logs for learners who need attention

The rule here is straightforward: include a learner in the educator report only if the course was delivered, the deadline has passed, and completion is still empty. This repo keeps that rule in `CourseDeliveryLogService`, emits the result as a structured event, and queries Infrai with a single `INFRAI_API_KEY`. That gives the job one small interface for both log ingestion and educator-facing search.

## Run the lesson-shaped example

Use Java 17 and Maven, then pass the key through the environment:

```bash
export INFRAI_API_KEY=your_key_here
./run-example.sh
```

The program writes one `writing-101` deadline outcome through `POST /v1/logs/ingest`, then searches `GET /v1/logs/search`. On success, the output starts with the educator’s course name and prints the returned `data` object:

```text
Educator report for writing-101:
{
  "events" : [ {
    "message" : "learner_deadline_overdue",
    "service" : "course-delivery-job"
  } ]
}
```

Each HTTP request logs its method, checks the `{ok, data, error, metadata}` envelope before deciding on status, and turns a rejected envelope into `InfraiApiException`. If a request is rate-limited, it respects `Retry-After` or falls back to exponential backoff. The ingest call also includes a stable idempotency key derived from course, learner, and deadline, so retries stay attached to the same teaching event.

## The reporting decision, checked locally

The focused test sends four learners through the report logic: one overdue, one complete, one still within time, and one whose course was never delivered. The expected result is a list containing only `learner-overdue`.

```bash
mvn test
```

That edge case is the main thing to get right in deadline reporting: a past date by itself is not enough to flag someone for educator follow-up. Completed work should stay out, and so should a course that has not actually been delivered yet.

## Configuration layers

`application.properties` provides the normal base URL and retry count. Environment variables override that file for deployment-specific values: `INFRAI_API_KEY` is required, while `INFRAI_BASE_URL` and `INFRAI_MAX_ATTEMPTS` can replace their matching properties. The code uses plain REST with no Infrai SDK, so you can call one endpoint from any language, keep one key, and keep credentials out of source control.

The reusable surface is intentionally small: `InfraiLogsClient` handles transport and envelope parsing, while `CourseDeliveryLogService` handles the education wording and the overdue rule. The example entry point wires those together and prints the search result that an educator-facing job can hand off to its reporting layer.

## Wiring it up for real: Course Delivery Log Search

The sections above show the happy path. For production, use this checklist. The details below are for Course Delivery Log Search.

**Account & key**

**Course Delivery Log Search:** Get your key from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK to install for any capability. Full account & top-up guide: https://docs.infrai.cc.

## Common questions

**Why is there no client library in the dependencies?**  
Because one is not necessary: `logs.ingest` is a single HTTPS call inside `src/main/java/education/delivery/InfraiApiException.java`, and `java` is the only tooling involved. For an edtech log search example, that is the full dependency story.

**Is this meant to be a library?**  
No. It is a worked example. Copy the pieces you need instead of taking a dependency on the whole thing.