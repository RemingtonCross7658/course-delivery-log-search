package education.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class CourseDeliveryLogService {
    public record LearnerDeadline(String courseId, String learnerId, Instant dueAt,
                                  Instant completedAt, boolean courseDelivered) {
    }

    private final InfraiLogsClient logs;
    private final Clock clock;
    private final ObjectMapper json;

    public CourseDeliveryLogService(InfraiLogsClient logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
        this.json = new ObjectMapper();
    }

    public boolean needsEducatorAttention(LearnerDeadline deadline) {
        return deadline.courseDelivered()
                && deadline.completedAt() == null
                && deadline.dueAt().isBefore(clock.instant());
    }

    public void recordDeadlineOutcome(LearnerDeadline deadline) {
        boolean overdue = needsEducatorAttention(deadline);
        ObjectNode event = json.createObjectNode();
        event.put("timestamp", clock.instant().toString());
        event.put("level", overdue ? "warn" : "info");
        event.put("message", overdue ? "learner_deadline_overdue" : "learner_deadline_on_track");
        event.put("service", "course-delivery-job");
        ObjectNode context = event.putObject("context");
        context.put("course_id", deadline.courseId());
        context.put("learner_id", deadline.learnerId());
        context.put("due_at", deadline.dueAt().toString());
        context.put("course_delivered", deadline.courseDelivered());
        context.put("educator_attention", overdue);
        logs.ingest(event, deliveryId(deadline));
    }

    public JsonNode findOverdueLearners(String courseId) {
        return logs.search("service:course-delivery-job course_id:" + courseId
                + " educator_attention:true");
    }

    public List<LearnerDeadline> educatorReport(List<LearnerDeadline> deadlines) {
        return deadlines.stream().filter(this::needsEducatorAttention).toList();
    }

    private String deliveryId(LearnerDeadline deadline) {
        return "deadline:" + deadline.courseId() + ":" + deadline.learnerId()
                + ":" + deadline.dueAt();
    }
}
