package at.platemate.payment;

import java.util.UUID;

import at.platemate.order.CustomerOrder;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment approve(CustomerOrder order) {
        return paymentRepository.save(new Payment(
                order,
                order.getTotalPrice(),
                PaymentStatus.APPROVED,
                "mock-" + UUID.randomUUID()));
    }

    public Payment decline(CustomerOrder order) {
        return paymentRepository.save(new Payment(
                order,
                order.getTotalPrice(),
                PaymentStatus.DECLINED,
                "mock-" + UUID.randomUUID()));
    }
}
