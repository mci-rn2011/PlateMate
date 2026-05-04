package at.platemate.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import at.platemate.order.CustomerOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private CustomerOrder order;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String providerReferenceMock;
    private LocalDateTime createdAt;

    protected Payment() {
    }

    public Payment(CustomerOrder order, BigDecimal amount, PaymentStatus status, String providerReferenceMock) {
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.providerReferenceMock = providerReferenceMock;
        this.createdAt = LocalDateTime.now();
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
