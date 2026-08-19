package education.delivery;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseDeliveryLogServiceTest {
    @Test
    void educatorReportContainsOnlyDeliveredUnfinishedWorkPastItsDeadline() {
        Instant now = Instant.parse("2026-08-19T12:00:00Z");
        CourseDeliveryLogService service = new CourseDeliveryLogService(
                null, Clock.fixed(now, ZoneOffset.UTC));
        CourseDeliveryLogService.LearnerDeadline overdue = deadline(
                "learner-overdue", now.minusSeconds(60), null, true);
        CourseDeliveryLogService.LearnerDeadline completed = deadline(
                "learner-complete", now.minusSeconds(60), now.minusSeconds(120), true);
        CourseDeliveryLogService.LearnerDeadline upcoming = deadline(
                "learner-upcoming", now.plusSeconds(60), null, true);
        CourseDeliveryLogService.LearnerDeadline awaitingDelivery = deadline(
                "learner-awaiting", now.minusSeconds(60), null, false);

        List<CourseDeliveryLogService.LearnerDeadline> report = service.educatorReport(
                List.of(overdue, completed, upcoming, awaitingDelivery));

        assertEquals(List.of(overdue), report);
    }

    private CourseDeliveryLogService.LearnerDeadline deadline(
            String learnerId, Instant dueAt, Instant completedAt, boolean delivered) {
        return new CourseDeliveryLogService.LearnerDeadline(
                "writing-101", learnerId, dueAt, completedAt, delivered);
    }
}
