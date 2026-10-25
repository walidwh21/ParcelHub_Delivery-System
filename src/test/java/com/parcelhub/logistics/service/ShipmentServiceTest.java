package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.dto.request.ShippingQuoteRequest;
import com.parcelhub.logistics.entity.*;
import com.parcelhub.logistics.entity.Rider;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import com.parcelhub.logistics.repository.ParcelRepository;
import com.parcelhub.logistics.repository.PaymentRepository;
import com.parcelhub.logistics.repository.RiderRepository;
import com.parcelhub.logistics.repository.UserRepository;
import com.parcelhub.logistics.util.SessionUtil;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

	@Mock
	private ParcelCategoryRepository categoryRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RiderRepository riderRepository;

	@Mock
	private ParcelRepository parcelRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private RiderService riderService;

	@Mock
	private ModelMapper modelMapper;

	@InjectMocks
	private ShipmentService shipmentService;

	private Account sender;
	private ParcelCategory category;
	private Rider rider;
	private MockedStatic<SessionUtil> authUtilMockedStatic;

	@BeforeEach
	void setUp() {
		sender = new Account();
		sender.setId(UUID.randomUUID());
		sender.setEmail("sender@example.com");

		category = new ParcelCategory();
		category.setId(UUID.randomUUID());
		category.setCategory("Electronics");

		rider = new Rider();
		rider.setId(UUID.randomUUID());
		rider.setRiderBalance(0.0);
		rider.setIsAssigned(true);

		authUtilMockedStatic = mockStatic(SessionUtil.class);
	}

	@AfterEach
	void tearDown() {
		authUtilMockedStatic.close();
	}

	@Test
	void testBookParcel_Success() {
		ParcelBookingRequest dto = createParcelBookingRequestDTO();
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
		when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(category));
		when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Parcel result = shipmentService.createBooking(dto);

		assertNotNull(result);
		assertEquals(sender, result.getSender());
		assertEquals(category, result.getCategory());
		verify(parcelRepository, times(1)).save(any(Parcel.class));
	}

	@Test
	void testBookParcel_InvalidSender() {
		ParcelBookingRequest dto = createParcelBookingRequestDTO();
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.of(category));
		when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> shipmentService.createBooking(dto));
	}

	@Test
	void testBookParcel_InvalidCategory() {
		ParcelBookingRequest dto = createParcelBookingRequestDTO();
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(categoryRepository.findById(dto.getCategoryId())).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> shipmentService.createBooking(dto));
	}

	@Test
	void testGenerateTransactionId() {
		String transactionId = shipmentService.issueTransactionRef();
		assertNotNull(transactionId);
		assertEquals(30, transactionId.length());
	}

	@Test
	void testSavePayment_Success() {
		Parcel parcel = new Parcel();
		parcel.setSender(sender);
		ParcelBookingRequest dto = createParcelBookingRequestDTO();
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		shipmentService.recordPendingPayment(parcel, dto);

		verify(paymentRepository, times(1)).save(any(Payment.class));
	}

	@Test
	void testGetBookedButNotDeliveredParcels_Success() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
		List<Parcel> parcels = Collections.singletonList(new Parcel());
		when(parcelRepository.findBySenderAndStatus(sender.getId(), ParcelStatus.BOOKED))
				.thenReturn(parcels);

		List<Parcel> result = shipmentService.findAwaitingPickup();

		assertEquals(parcels, result);
		verify(parcelRepository, times(1)).findBySenderAndStatus(sender.getId(), ParcelStatus.BOOKED);
	}

	@Test
	void testGetBookedButNotDeliveredParcels_InvalidSender() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> shipmentService.findAwaitingPickup());
	}

	@Test
	void testGenerateUniqueTrackingId() {
		when(parcelRepository.existsByTrackingId(anyString())).thenReturn(false);

		String trackingId = shipmentService.issueTrackingCode();

		assertNotNull(trackingId);
		assertEquals(6, trackingId.length());
		verify(parcelRepository, atLeastOnce()).existsByTrackingId(anyString());
	}

	@Test
	void testGetParcelList_Success() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn("sender@example.com");
		when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
		List<Parcel> parcels = Collections.singletonList(new Parcel());
		when(parcelRepository.getAllBySender(sender.getId())).thenReturn(parcels);

		List<Parcel> result = shipmentService.findSenderHistory();

		assertEquals(parcels, result);
		verify(parcelRepository, times(1)).getAllBySender(sender.getId());
	}

	@Test
	void testGetParcelById_Success() {
		UUID parcelId = UUID.randomUUID();
		Parcel parcel = new Parcel();
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));

		Parcel result = shipmentService.getParcelById(parcelId);

		assertEquals(parcel, result);
		verify(parcelRepository, times(1)).findById(parcelId);
	}

	@Test
	void testGetParcelById_NotFound() {
		UUID parcelId = UUID.randomUUID();
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> shipmentService.getParcelById(parcelId));
	}

	@Test
	void testUpdateParcelStatus_Delivered_Success() {
		UUID parcelId = UUID.randomUUID();
		Parcel parcel = new Parcel();
		parcel.setStatus(ParcelStatus.IN_TRANSIT);
		parcel.setRider(rider);
		parcel.setPrice(BigDecimal.valueOf(100.0));
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
		when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);
		when(riderRepository.save(any(Rider.class))).thenReturn(rider);

		shipmentService.updateParcelStatus(parcelId, ParcelStatus.DELIVERED);

		assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
		assertNotNull(parcel.getDeliveredAt());
		assertEquals(100.0, rider.getRiderBalance());
		assertFalse(rider.getIsAssigned());
		verify(parcelRepository, times(1)).save(parcel);
		verify(riderRepository, times(1)).save(rider);
	}

	@Test
	void testUpdateParcelStatus_PickedUp_Success() {
		UUID parcelId = UUID.randomUUID();
		Parcel parcel = new Parcel();
		parcel.setStatus(ParcelStatus.ASSIGNED);
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
		when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);

		shipmentService.updateParcelStatus(parcelId, ParcelStatus.PICKED_UP);

		assertEquals(ParcelStatus.PICKED_UP, parcel.getStatus());
		assertNotNull(parcel.getPickupTime());
		verify(parcelRepository, times(1)).save(parcel);
	}

	@Test
	void testUpdateParcelStatus_InvalidTransition() {
		UUID parcelId = UUID.randomUUID();
		Parcel parcel = new Parcel();
		parcel.setStatus(ParcelStatus.BOOKED);
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));

		assertThrows(
				IllegalStateException.class,
				() -> shipmentService.updateParcelStatus(parcelId, ParcelStatus.DELIVERED));
	}

	@Test
	void testUpdateParcelStatus_Delivered_NoRider() {
		UUID parcelId = UUID.randomUUID();
		Parcel parcel = new Parcel();
		parcel.setStatus(ParcelStatus.IN_TRANSIT);
		parcel.setRider(null);
		when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));

		assertThrows(
				IllegalStateException.class,
				() -> shipmentService.updateParcelStatus(parcelId, ParcelStatus.DELIVERED));
	}

	@Test
	void testCalculateShippingCost() {
		ShippingQuoteRequest dto = new ShippingQuoteRequest(10.0, 5.0);
		double expectedCost = (0.2 * 10.0) + (0.4 * 5.0) + 100.0; // 2 + 2 + 100 = 104

		double result = shipmentService.estimateShippingCost(dto);

		assertEquals(expectedCost, result, 0.001);
	}

	@Test
	void testGetRelatedParcelListOfCurrentRider_Success() {
		List<Parcel> parcels = Collections.singletonList(new Parcel());
		when(riderService.getAuthenticatedRider()).thenReturn(rider);
		when(parcelRepository.findByRider(rider)).thenReturn(parcels);

		List<Parcel> result = shipmentService.findCurrentRiderParcels();

		assertEquals(parcels, result);
		verify(riderService, times(1)).getAuthenticatedRider();
		verify(parcelRepository, times(1)).findByRider(rider);
	}

	private ParcelBookingRequest createParcelBookingRequestDTO() {
		ParcelBookingRequest dto = new ParcelBookingRequest();
		dto.setCategoryId(category.getId());
		dto.setDescription("Test Parcel");
		dto.setWeight(5.0);
		dto.setSize(10.0);
		dto.setPickupDivision("Division");
		dto.setPickupDistrict("District");
		dto.setPickupUpazila("Upazila");
		dto.setPickupVillage("Village");
		dto.setReceiverName("Receiver");
		dto.setReceiverPhone("+8801741543475");
		dto.setReceiverEmail("receiver@example.com");
		dto.setReceiverDivision("Receiver Division");
		dto.setReceiverDistrict("Receiver District");
		dto.setReceiverUpazila("Receiver Upazila");
		dto.setReceiverVillage("Receiver Village");
		dto.setReceiverAddress("Receiver Address");
		dto.setPrice(BigDecimal.valueOf(100.0));
		dto.setDistance(50.0);
		dto.setPaymentMethod("Cash");
		dto.setTransactionId("TX123");
		return dto;
	}
}
