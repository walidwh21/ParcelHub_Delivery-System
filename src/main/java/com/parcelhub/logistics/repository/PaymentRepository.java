package com.parcelhub.logistics.repository;

import com.parcelhub.logistics.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
	Optional<Payment> findByTransactionId(String transactionId);
}
