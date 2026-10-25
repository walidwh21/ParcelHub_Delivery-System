package com.parcelhub.logistics.entity;

import com.parcelhub.logistics.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment")
public class Payment {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	private String transactionId;
	private BigDecimal amount;
	private String currency;
	private String paymentMethod;
	private String bankTransactionId;

	@ManyToOne(cascade = CascadeType.ALL)
	private Account user;

	@OneToOne(cascade = CascadeType.ALL)
	private Parcel parcel;

	private PaymentStatus paymentStatus;
}
