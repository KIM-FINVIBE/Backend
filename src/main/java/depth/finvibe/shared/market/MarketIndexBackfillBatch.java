package depth.finvibe.shared.market;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketIndexBackfillBatch {
    private static final Logger log = LoggerFactory.getLogger(MarketIndexBackfillBatch.class);
    private static final List<Map<String, String>> DOMESTIC_INDEXES = List.of(
            Map.of("code", "0001", "name", "코스피종합"),
            Map.of("code", "1001", "name", "코스닥")
    );

    private final MarketService marketService;
    private final AtomicBoolean startupExecuted = new AtomicBoolean(false);

    @Value("${finvibe.market.index-backfill.enabled:true}")
    private boolean enabled;

    @Value("${finvibe.market.index-backfill.run-on-startup:true}")
    private boolean runOnStartup;

    @Value("${finvibe.market.index-backfill.schedule-enabled:true}")
    private boolean scheduleEnabled;

    @Value("${finvibe.market.index-backfill.points:260}")
    private int points;

    @Value("${finvibe.market.index-backfill.minimum-points:20}")
    private int minimumPoints;

    @Value("${finvibe.market.index-backfill.request-delay-ms:500}")
    private long requestDelayMs;

    @Value("${finvibe.market.index-backfill.startup-delay-ms:12000}")
    private long startupDelayMs;

    public MarketIndexBackfillBatch(MarketService marketService) {
        this.marketService = marketService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        if (!enabled || !runOnStartup || !startupExecuted.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            sleepBeforeStartup();
            backfillIndexCandles("startup");
        }).exceptionally(e -> {
            log.warn("지수 백필 시작 작업 실패 message={}", e.getMessage(), e);
            return null;
        });
    }

    @Scheduled(cron = "${finvibe.market.index-backfill.cron:0 7/10 * * * *}", zone = "Asia/Seoul")
    public void backfillOnSchedule() {
        if (!enabled || !scheduleEnabled) {
            return;
        }
        backfillIndexCandles("schedule");
    }

    public void backfillIndexCandles(String trigger) {
        if (!marketService.kisEnabled()) {
            log.info("지수 백필을 건너뜁니다. trigger={}, reason=kis-disabled", trigger);
            return;
        }

        int refreshed = 0;
        int skipped = 0;
        int failed = 0;
        int resolvedPoints = Math.max(minimumPoints, points);

        for (Map<String, String> index : DOMESTIC_INDEXES) {
            String code = index.get("code");
            String name = index.get("name");

            try {
                List<Map<String, Object>> candles = marketService.refreshStoredIndexCandles(
                        code,
                        name,
                        "day",
                        resolvedPoints
                );
                if (candles.isEmpty()) {
                    skipped++;
                } else {
                    refreshed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("지수 백필 실패 indexCode={}, indexName={}, message={}", code, name, e.getMessage());
            }

            if (requestDelayMs > 0) {
                LockSupport.parkNanos(Duration.ofMillis(requestDelayMs).toNanos());
            }
        }

        log.info("지수 백필 완료 trigger={}, refreshed={}, skipped={}, failed={}, points={}, minimumPoints={}",
                trigger, refreshed, skipped, failed, points, minimumPoints);
    }

    private void sleepBeforeStartup() {
        if (startupDelayMs <= 0) {
            return;
        }
        try {
            log.info("index-backfill startup 작업을 {}ms 뒤에 시작합니다.", startupDelayMs);
            Thread.sleep(startupDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
