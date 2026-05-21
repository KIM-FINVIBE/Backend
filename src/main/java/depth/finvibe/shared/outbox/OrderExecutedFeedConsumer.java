package depth.finvibe.shared.outbox;

import depth.finvibe.shared.json.Json;
import depth.finvibe.shared.persistence.mongo.feed.UserActivityFeedDocument;
import depth.finvibe.shared.persistence.mongo.feed.UserActivityFeedRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderExecutedFeedConsumer {
    private static final String CONSUMER_NAME = "activity-feed";

    private final ConsumedEventJdbcRepository consumedEventJdbcRepository;
    private final UserActivityFeedRepository userActivityFeedRepository;

    public OrderExecutedFeedConsumer(
            ConsumedEventJdbcRepository consumedEventJdbcRepository,
            UserActivityFeedRepository userActivityFeedRepository
    ) {
        this.consumedEventJdbcRepository = consumedEventJdbcRepository;
        this.userActivityFeedRepository = userActivityFeedRepository;
    }

    @KafkaListener(
            topics = "finvibe.order.executed",
            groupId = "finvibe-activity-feed"
    )
    public void onMessage(String payload, Acknowledgment acknowledgment) {
        Map<String, Object> envelope = Json.parseObject(payload);
        long outboxEventId = ((Number) envelope.get("outboxEventId")).longValue();

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) envelope.get("payload");

        UserActivityFeedDocument feed = new UserActivityFeedDocument();
        feed.setId("order-executed-" + outboxEventId);
        feed.setUserId(String.valueOf(event.get("userId")));
        feed.setType("TRADE_EXECUTED");
        feed.setTitle("buy".equals(String.valueOf(event.get("side"))) ? "매수 체결" : "매도 체결");
        feed.setDescription(description(event));
        feed.setStockId(String.valueOf(event.get("stockId")));
        feed.setOrderId(String.valueOf(event.get("orderId")));
        feed.setCreatedAt(parseTime(event.get("completedAt")));

        userActivityFeedRepository.save(feed);

        if (!consumedEventJdbcRepository.tryMarkConsumed(CONSUMER_NAME, outboxEventId)) {
            acknowledgment.acknowledge();
            return;
        }
        acknowledgment.acknowledge();
    }

    private String description(Map<String, Object> event) {
        String stockName = String.valueOf(event.get("stockName"));
        String side = String.valueOf(event.get("side"));
        String action = "buy".equals(side) ? "매수 체결" : "매도 체결";
        BigDecimal quantity = BigDecimal.valueOf(number(event.get("executedQuantity")).doubleValue()).stripTrailingZeros();
        return stockName + " " + quantity.toPlainString() + "주 " + action;
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return BigDecimal.valueOf(Double.parseDouble(String.valueOf(value)));
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }
}
