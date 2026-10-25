package com.parcelhub.logistics.controller;

import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.ParcelCategory;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import com.parcelhub.logistics.service.GatewayPaymentService;
import com.parcelhub.logistics.service.ShipmentService;
import com.parcelhub.logistics.service.UserService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UserService userService;

	@Mock
	private ParcelCategoryRepository categoryRepository;

	@Mock
	private ShipmentService shipmentService;

	@Mock
	private GatewayPaymentService gatewayPaymentService;

	@InjectMocks
	private UserController userController;

	private ProfileUpdateRequest profileUpdateRequest;
	private ParcelBookingRequest bookingRequest;
	private List<Parcel> parcelEntities;
	private List<ParcelCategory> productCategories;

	@BeforeEach
	void setUp() {
		// Initialize MockMvc with standalone setup
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

		// Initialize test data
		profileUpdateRequest = new ProfileUpdateRequest();
		profileUpdateRequest.setFullName("John Doe");
		profileUpdateRequest.setPhoneNumber("01712345678");

		bookingRequest = new ParcelBookingRequest();
		bookingRequest.setCategoryId(UUID.randomUUID());
		bookingRequest.setWeight(5.0);
		bookingRequest.setSize(10.0);
		bookingRequest.setPickupDivision("Dhaka");
		bookingRequest.setPickupDistrict("Dhaka");
		bookingRequest.setPickupUpazila("Gulshan");
		bookingRequest.setPickupVillage("Banani");
		bookingRequest.setReceiverName("Jane Doe");
		bookingRequest.setReceiverPhone("+8801712345678");
		bookingRequest.setReceiverDivision("Chittagong");
		bookingRequest.setReceiverDistrict("Chittagong");
		bookingRequest.setReceiverUpazila("Pahartali");
		bookingRequest.setReceiverVillage("Kattali");
		bookingRequest.setPrice(new BigDecimal("100.00"));
		bookingRequest.setDistance(200.0);
		bookingRequest.setPaymentMethod("card");
		bookingRequest.setTransactionId("TX123456");

		Parcel parcel = new Parcel();
		parcelEntities = Arrays.asList(parcel);

		ParcelCategory category = new ParcelCategory();
		productCategories = Arrays.asList(category);
	}

	@Test
	void userDashboard_ReturnsDashboardViewWithParcels() throws Exception {
		when(shipmentService.findAwaitingPickup()).thenReturn(parcelEntities);

		mockMvc.perform(get("/user/dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/home"))
				.andExpect(model().attributeExists("bookedParcels"))
				.andExpect(model().attribute("bookedParcels", parcelEntities));

		verify(shipmentService).findAwaitingPickup();
	}

	@Test
	void basicProfileSettings_ReturnsProfileSettingsViewWithDto() throws Exception {
		when(userService.userProfileUpdateGet()).thenReturn(profileUpdateRequest);

		mockMvc.perform(get("/user/profile-settings"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/account"))
				.andExpect(model().attributeExists("profileUpdateRequest"))
				.andExpect(model().attribute("profileUpdateRequest", profileUpdateRequest));

		verify(userService).userProfileUpdateGet();
	}

	@Test
	void updateAccountSettings_Success_RedirectsWithSuccessMessage() throws Exception {
		when(userService.updateUserProfile(any(ProfileUpdateRequest.class))).thenReturn(true);

		mockMvc.perform(post("/user/profile-settings")
						.param("fullName", "John Doe")
						.param("phoneNumber", "01712345678"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user/profile-settings?success"))
				.andExpect(flash().attributeExists("successMessage"));

		verify(userService).updateUserProfile(any(ProfileUpdateRequest.class));
	}

	@Test
	void updateAccountSettings_ValidationError_ReturnsProfileSettingsView() throws Exception {
		mockMvc.perform(post("/user/profile-settings")
						.param("fullName", "") // Invalid: empty name
						.param("phoneNumber", "01712345678"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/account"))
				.andExpect(model().attributeHasFieldErrors("profileUpdateRequest", "fullName"));

		verify(userService, never()).updateUserProfile(any(ProfileUpdateRequest.class));
	}

	@Test
	void updateAccountSettings_ServiceException_ReturnsProfileSettingsViewWithError() throws Exception {
		when(userService.updateUserProfile(any(ProfileUpdateRequest.class)))
				.thenThrow(new RuntimeException("Update failed"));

		mockMvc.perform(post("/user/profile-settings")
						.param("fullName", "John Doe")
						.param("phoneNumber", "01712345678"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/account"))
				.andExpect(model().attributeExists("updateError"))
				.andExpect(model().attribute(
								"updateError", "An error occurred while updating your profile. Please try again."));

		verify(userService).updateUserProfile(any(ProfileUpdateRequest.class));
	}

	@Test
	void bookParcel_ReturnsBookParcelViewWithAttributes() throws Exception {
		when(categoryRepository.findAll()).thenReturn(productCategories);

		mockMvc.perform(get("/user/book-parcel"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/book-parcel"))
				.andExpect(model().attributeExists("productCategories"))
				.andExpect(model().attribute("productCategories", productCategories))
				.andExpect(model().attributeExists("title"))
				.andExpect(model().attribute("title", "Book Parcel"))
				.andExpect(model().attributeExists("bookingRequest"))
				.andExpect(model().attribute("bookingRequest", isA(ParcelBookingRequest.class)));

		verify(categoryRepository).findAll();
	}

	@Test
	void handleParcelBooking_ValidationError_ReturnsBookParcelView() throws Exception {
		when(categoryRepository.findAll()).thenReturn(productCategories);

		mockMvc.perform(post("/user/book-parcel")
						.param("categoryId", "") // Invalid: missing category
						.param("weight", "5.0")
						.param("size", "10.0")
						.param("pickupDivision", "Dhaka")
						.param("pickupDistrict", "Dhaka")
						.param("pickupUpazila", "Gulshan")
						.param("pickupVillage", "Banani")
						.param("receiverName", "Jane Doe")
						.param("receiverPhone", "+8801712345678")
						.param("receiverDivision", "Chittagong")
						.param("receiverDistrict", "Chittagong")
						.param("receiverUpazila", "Pahartali")
						.param("receiverVillage", "Kattali")
						.param("price", "100.00")
						.param("distance", "200.0")
						.param("paymentMethod", "card"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/book-parcel"))
				.andExpect(model().attributeExists("productCategories"))
				.andExpect(model().attributeExists("title"))
				.andExpect(model().attributeHasFieldErrors("bookingRequest", "categoryId"));

		verify(categoryRepository).findAll();
		verify(shipmentService, never()).createBooking(any(ParcelBookingRequest.class));
	}

	@Test
	void getParcelHistory_ReturnsParcelHistoryViewWithParcels() throws Exception {
		when(shipmentService.findSenderHistory()).thenReturn(parcelEntities);

		mockMvc.perform(get("/user/parcel-history"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/parcel-history"))
				.andExpect(model().attributeExists("parcelHistory"))
				.andExpect(model().attribute("parcelHistory", parcelEntities));

		verify(shipmentService).findSenderHistory();
	}

	@Test
	void handleParcelBooking_ValidationError_ReturnsFormWithErrors() throws Exception {
		// Setup mock data
		when(categoryRepository.findAll()).thenReturn(productCategories);

		// Perform the request with invalid data (missing required fields)
		mockMvc.perform(post("/user/book-parcel")
						.param("weight", "5.0") // Missing other required fields
						.param("paymentMethod", "card"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard/book-parcel"))
				.andExpect(model().attributeExists("productCategories"))
				.andExpect(model().attribute("productCategories", productCategories))
				.andExpect(model().attributeExists("title"))
				.andExpect(model().attributeHasErrors("bookingRequest"));

		// Verify no booking/payment processing occurred
		verify(shipmentService, never()).issueTransactionRef();
		verify(shipmentService, never()).createBooking(any());
		verify(gatewayPaymentService, never()).getPaymentUrl(any(), any());
	}
}
