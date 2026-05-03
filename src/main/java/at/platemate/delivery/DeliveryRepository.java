package at.platemate.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import at.platemate.order.CustomerOrder;
import at.platemate.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @EntityGraph(attributePaths = {"order", "order.restaurant", "order.customer", "order.assignedDriver", "order.items", "driver"})
    Optional<Delivery> findByOrder(CustomerOrder order);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "order.customer", "driver"})
    List<Delivery> findByDriverOrderByAssignedAtDesc(User driver);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "order.customer", "driver"})
    List<Delivery> findByDriverAndStatusInOrderByAssignedAtDesc(User driver, List<DeliveryStatus> statuses);

    long countByDriverAndStatusIn(User driver, List<DeliveryStatus> statuses);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "order.customer", "driver"})
    List<Delivery> findByDriverAndStatusOrderByDeliveredAtDesc(User driver, DeliveryStatus status);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "order.customer", "driver"})
    List<Delivery> findByDriverAndStatusAndDeliveredAtGreaterThanEqualOrderByDeliveredAtDesc(
            User driver,
            DeliveryStatus status,
            LocalDateTime deliveredAt);

    @Query("""
            select coalesce(sum(d.deliveryFee), 0)
            from Delivery d
            where d.driver = :driver
              and d.status = at.platemate.delivery.DeliveryStatus.DELIVERED
            """)
    BigDecimal sumDeliveredFeesByDriver(User driver);

    @Query("""
            select coalesce(sum(d.deliveryFee), 0)
            from Delivery d
            where d.driver = :driver
              and d.status = at.platemate.delivery.DeliveryStatus.DELIVERED
              and d.deliveredAt >= :deliveredAt
            """)
    BigDecimal sumDeliveredFeesByDriverSince(User driver, LocalDateTime deliveredAt);
}
