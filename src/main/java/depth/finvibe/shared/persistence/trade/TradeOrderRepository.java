package depth.finvibe.shared.persistence.trade;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOrderRepository extends JpaRepository<TradeOrderEntity, String> {
    List<TradeOrderEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<TradeOrderEntity> findByOrderIdAndUserId(String orderId, String userId);
    List<TradeOrderEntity> findAllByOrderStatusOrderByAcceptedAtAscCreatedAtAscOrderIdAsc(String orderStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from TradeOrderEntity o
            where o.orderId = :orderId
              and o.orderStatus = 'pending'
            """)
    Optional<TradeOrderEntity> lockPendingByOrderId(@Param("orderId") String orderId);
}
