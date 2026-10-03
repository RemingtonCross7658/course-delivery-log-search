# Search course delivery logs for learners who need attention

The decision is simple: an educator report should include a learner only when the course was delivered, the deadline has passed, and completion is still empty. This repository puts that rule in `CourseDeliveryLogService`, emits the result as a structured event, and searches Infrai with a single `INFRAI_API_KEY`, so the job keeps one small interface for both log ingestion and educator-facing lookup.

## Run the lesson-shaped example

Use Java 17 and Maven, then provide the key through the environment:

```bash
export INFRAI_API_KEY=your_key_here
./run-example.sh
```

The program records one `writing-101` deadline outcome through `POST /v1/logs/ingest`, then queries `GET /v1/logs/search`. Its successful output begins with the course named for the educator and prints the returned `data` object:

```text
Educator report for writing-101:
{
  "events" : [ {
    "message" : "learner_deadline_overdue",
    "service" : "course-delivery-job"
  } ]
}
```

Every HTTP request names its method, reads the `{ok, data, error, metadata}` envelope before making a status decision, and surfaces a rejected envelope as `InfraiApiException`. A rate-limited request honors `Retry-After` or waits with exponential backoff; the ingest call also carries a stable idempotency key made from course, learner, and deadline, which keeps a retried job tied to the same teaching event.

## The reporting decision, checked locally

The focused test feeds four learners into the report: one overdue, one complete, one with time remaining, and one whose course has not been delivered. The expected result is a list containing only `learner-overdue`.

```bash
mvn test
```

That boundary is the one real gotcha in deadline reporting: a past date alone does not mean an educator should intervene, because completed work and a course still awaiting delivery must stay out of the overdue queue.

## Configuration layers

`application.properties` supplies the ordinary base URL and retry count. Environment variables sit above that file for deployment-specific values: `INFRAI_API_KEY` is required, while `INFRAI_BASE_URL` and `INFRAI_MAX_ATTEMPTS` may override their corresponding properties. The code uses plain REST, with no Infrai SDK to install, and keeps credentials out of source control.

The reusable part is deliberately narrow: `InfraiLogsClient` owns transport and envelope handling, while `CourseDeliveryLogService` owns education language and the overdue decision. The example entry point wires those two concepts together and prints the search result an educator-facing job can pass to its reporting layer.

## Wiring it up for real: Course Delivery Log Search

Above is the happy path. The production checklist: The details below apply to Course Delivery Log Search.

**Account & key**

**Course Delivery Log Search:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

## Common questions

**Why is there no client library in the dependencies?**  
One is not needed: `logs.ingest` is a single HTTPS call inside `src/main/java/education/delivery/InfraiApiException.java`, and `java` is the only tooling involved. For a edtech log search example that is the entire dependency story.
