package com.parcelhub.logistics.service;

import static com.parcelhub.logistics.enums.ParcelStatus.*;

import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.dto.request.ShippingQuoteRequest;
import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.ParcelCategory;
import com.parcelhub.logistics.entity.Payment;
import com.parcelhub.logistics.entity.Rider;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.enums.PaymentStatus;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import com.parcelhub.logistics.repository.ParcelRepository;
import com.parcelhub.logistics.repository.PaymentRepository;
import com.parcelhub.logistics.repository.RiderRepository;
import com.parcelhub.logistics.repository.UserRepository;
import com.parcelhub.logistics.util.SessionUtil;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Core booking and delivery-lifecycle logic: creating parcels, recording the pending
 * payment, quoting prices and moving a parcel through its allowed status transitions.
 */
@RequiredArgsConstructor
@Service
public class ShipmentService {

	private static final String UNKNOWN_SENDER = "Invalid sender";
	private static final String TRANSACTION_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int TRANSACTION_REF_LENGTH = 30;
	private static final int TRACKING_CODE_FLOOR = 100_000;
	private static final int TRACKING_CODE_SPAN = 900_000;
	private static final String CURRENCY = "BDT";

	/** Which statuses a parcel may move to from a given status. */
	private static final Map<ParcelStatus, Set<ParcelStatus>> ALLOWED_MOVES = buildTransitionTable();

	private static Map<ParcelStatus, Set<ParcelStatus>> buildTransitionTable() {
		Map<ParcelStatus, Set<ParcelStatus>> table = new EnumMap<>(ParcelStatus.class);
		table.put(ASSIGNED, EnumSet.of(PICKED_UP, POSTPONED));
		table.put(PICKED_UP, EnumSet.of(IN_TRANSIT, DELIVERED, POSTPONED));
		table.put(IN_TRANSIT, EnumSet.of(DELIVERED, POSTPONED));
		table.put(POSTPONED, EnumSet.of(PICKED_UP, ASSIGNED));
		return table;
	}

	private final SecureRandom random = new SecureRandom();
	private final ParcelCategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final RiderRepository riderRepository;
	private final ParcelRepository parcelRepository;
	private final PaymentRepository paymentRepository;
	private final RiderService riderService;

	@Value("${parcelhub.pricing.base-charge:100}")
	private double baseCharge = 100;

	@Value("${parcelhub.pricing.per-size-unit:0.2}")
	private double perSizeUnit = 0.2;

	@Value("${parcelhub.pricing.per-weight-unit:0.4}")
	private double perWeightUnit = 0.4;

	// ------------------------------------------------------------------ booking

	public Parcel createBooking(ParcelBookingRequest request) {
		return parcelRepository.save(assembleParcel(request));
	}

	public Parcel assembleParcel(ParcelBookingRequest request) {
		ParcelCategory category = categoryRepository
				.findById(request.getCategoryId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
		Account sender = loadCurrentSender();

		Parcel parcel = new Parcel();
		parcel.setCategory(category);
		parcel.setSender(sender);
		parcel.setTrackingId(issueTrackingCode());
		copyCargoDetails(request, parcel);
		copyPickupLocation(request, parcel);
		copyReceiverDetails(request, parcel);
		return parcel;
	}

	private void copyCargoDetails(ParcelBookingRequest request, Parcel parcel) {
		parcel.setDescription(request.getDescription());
		parcel.setWeight(request.getWeight());
		parcel.setSize(request.getSize());
		parcel.setPrice(request.getPrice());
		parcel.setDistance(request.getDistance());
	}

	private void copyPickupLocation(ParcelBookingRequest request, Parcel parcel) {
		parcel.setPickupDivision(request.getPickupDivision());
		parcel.setPickupDistrict(request.getPickupDistrict());
		parcel.setPickupUpazila(request.getPickupUpazila());
		parcel.setPickupVillage(request.getPickupVillage());
	}

	private void copyReceiverDetails(ParcelBookingRequest request, Parcel parcel) {
		parcel.setReceiverName(request.getReceiverName());
		parcel.setReceiverPhone(request.getReceiverPhone());
		parcel.setReceiverEmail(request.getReceiverEmail());
		parcel.setReceiverDivision(request.getReceiverDivision());
		parcel.setReceiverDistrict(request.getReceiverDistrict());
		parcel.setReceiverUpazila(request.getReceiverUpazila());
		parcel.setReceiverVillage(request.getReceiverVillage());
		parcel.setReceiverAddress(request.getReceiverAddress());
	}

	public void recordPendingPayment(Parcel parcel, ParcelBookingRequest request) {
		Payment payment = new Payment();
		payment.setAmount(request.getPrice());
		payment.setTransactionId(request.getTransactionId());
		payment.setPaymentMethod(request.getPaymentMethod());
		payment.setCurrency(CURRENCY);
		payment.setParcel(parcel);
		payment.setUser(parcel.getSender());
		payment.setPaymentStatus(PaymentStatus.PENDING);
		paymentRepository.save(payment);
	}

	// --------------------------------------------------------------- identifiers

	/** Builds a 30 character reference: base-36 timestamp followed by random alphanumerics. */
	public String issueTransactionRef() {
		String timestamp = Long.toString(System.currentTimeMillis(), 36);
		StringBuilder ref = new StringBuilder(TRANSACTION_REF_LENGTH).append(timestamp);
		while (ref.length() < TRANSACTION_REF_LENGTH) {
			ref.append(TRANSACTION_ALPHABET.charAt(random.nextInt(TRANSACTION_ALPHABET.length())));
		}
		return ref.toString();
	}

	/** Returns a six digit tracking code that is not yet used by any parcel. */
	public String issueTrackingCode() {
		String candidate = nextTrackingCandidate();
		while (parcelRepository.existsByTrackingId(candidate)) {
			candidate = nextTrackingCandidate();
		}
		return candidate;
	}

	private String nextTrackingCandidate() {
		return String.valueOf(TRACKING_CODE_FLOOR + random.nextInt(TRACKING_CODE_SPAN));
	}

	// ------------------------------------------------------------------- queries

	public List<Parcel> findAwaitingPickup() {
		return parcelRepository.findBySenderAndStatus(loadCurrentSender().getId(), ParcelStatus.BOOKED);
	}

	public List<Parcel> findSenderHistory() {
		return parcelRepository.getAllBySender(loadCurrentSender().getId());
	}

	public Parcel getParcelById(UUID id) {
		return parcelRepository.findById(id).orElseThrow(() -> new RuntimeException("Parcel not found"));
	}

	public List<Parcel> findCurrentRiderParcels() {
		return parcelRepository.findByRider(riderService.getAuthenticatedRider());
	}

	private Account loadCurrentSender() {
		return userRepository
				.findByEmail(SessionUtil.currentUserEmail())
				.orElseThrow(() -> new IllegalArgumentException(UNKNOWN_SENDER));
	}

	// ------------------------------------------------------------------ lifecycle

	@Transactional
	public void updateParcelStatus(UUID id, ParcelStatus target) {
		Parcel parcel = getParcelById(id);
		ParcelStatus current = parcel.getStatus();

		if (!canMove(current, target)) {
			throw new IllegalStateException("Invalid status transition from " + current + " to " + target);
		}

		parcel.setStatus(target);
		parcel.setUpdatedAt(LocalDateTime.now());
		switch (target) {
			case DELIVERED -> settleDelivery(parcel);
			case PICKED_UP -> parcel.setPickupTime(LocalDateTime.now());
			default -> {
				// no side effects for the remaining statuses
			}
		}
		parcelRepository.save(parcel);
	}

	private boolean canMove(ParcelStatus from, ParcelStatus to) {
		return from != to && ALLOWED_MOVES.getOrDefault(from, Set.of()).contains(to);
	}

	/** Stamps delivery time, credits the rider with the parcel price and frees the rider. */
	private void settleDelivery(Parcel parcel) {
		Rider rider = parcel.getRider();
		if (rider == null) {
			throw new IllegalStateException("No rider assigned to parcel with ID: " + parcel.getId());
		}

		LocalDateTime now = LocalDateTime.now();
		parcel.setDeliveryTime(now);
		parcel.setDeliveredAt(now);

		rider.setRiderBalance(rider.getRiderBalance() + parcel.getPrice().doubleValue());
		rider.setIsAssigned(false);
		riderRepository.save(rider);
	}

	// ------------------------------------------------------------------- pricing

	public double estimateShippingCost(ShippingQuoteRequest request) {
		return baseCharge + perSizeUnit * request.getSize() + perWeightUnit * request.getWeight();
	}
}
