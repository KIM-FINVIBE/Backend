package depth.finvibe.shared.persistence.market;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketIndexCandleRepository extends JpaRepository<MarketIndexCandleEntity, Long> {
    Optional<MarketIndexCandleEntity> findByIndexCodeAndTimeframeAndCandleAt(
            String indexCode,
            String timeframe,
            LocalDateTime candleAt
    );

    List<MarketIndexCandleEntity> findByIndexCodeAndTimeframeOrderByCandleAtAsc(String indexCode, String timeframe);

    long countByIndexCodeAndTimeframe(String indexCode, String timeframe);
}
