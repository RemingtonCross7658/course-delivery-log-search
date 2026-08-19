package education.delivery;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Instant;

public final class CourseDeliveryExample {
    private CourseDeliveryExample() {
    }

    public static void main(String[] args) {
        InfraiLogsClient client = new InfraiLogsClient(InfraiConfiguration.load());
        CourseDeliveryLogService delivery = new CourseDeliveryLogService(client, Clock.systemUTC());

        CourseDeliveryLogService.LearnerDeadline deadline =
                new CourseDeliveryLogService.LearnerDeadline(
                        "writing-101", "learner-204", Instant.parse("2026-08-18T09:00:00Z"),
                        null, true);
        delivery.recordDeadlineOutcome(deadline);
        JsonNode report = delivery.findOverdueLearners(deadline.courseId());

        System.out.println("Educator report for " + deadline.courseId() + ":");
        System.out.println(report.toPrettyString());
    }
}
