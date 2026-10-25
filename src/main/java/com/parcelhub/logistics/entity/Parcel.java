package com.parcelhub.logistics.entity;

import com.parcelhub.logistics.enums.ParcelStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
public class Parcel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private ParcelCategory category;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private Double weight;

	@Column(nullable = false)
	private Double size;

	@Column(name = "pickup_division")
	private String pickupDivision;

	@Column(name = "pickup_district")
	private String pickupDistrict;

	@Column(name = "pickup_upazila")
	private String pickupUpazila;

	@Column(name = "pickup_village")
	private String pickupVillage;

	@Column(name = "receiver_name", nullable = false)
	private String receiverName;

	@Column(name = "receiver_phone", nullable = false)
	private String receiverPhone;

	@Column(name = "receiver_email")
	private String receiverEmail;

	@Column(name = "receiver_address")
	private String receiverAddress;

	@Column(name = "receiver_division")
	private String receiverDivision;

	@Column(name = "receiver_district")
	private String receiverDistrict;

	@Column(name = "receiver_upazila")
	private String receiverUpazila;

	@Column(name = "receiver_village")
	private String receiverVillage;

	@Column(name = "receiver_otp")
	private String receiverOtp;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id", nullable = false)
	private Account sender;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "rider_id")
	private Rider rider;

	@Column(name = "rider_otp")
	private String riderOtp;

	@Column(name = "assigned_at")
	private LocalDateTime assignedAt;

	@Enumerated(EnumType.STRING)
	private ParcelStatus status;

	@Column(name = "pickup_time")
	private LocalDateTime pickupTime;

	@Column(name = "delivery_time")
	private LocalDateTime deliveryTime;

	@Column(nullable = false)
	private BigDecimal price;

	@Column(nullable = false)
	private Double distance;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(unique = true, length = 6)
	private String trackingId;

	@PrePersist
	protected void onCreate() {
		if (this.status == null) {
			this.status = ParcelStatus.UNPAID;
		}
		this.createdAt = LocalDateTime.now();
	}
}
