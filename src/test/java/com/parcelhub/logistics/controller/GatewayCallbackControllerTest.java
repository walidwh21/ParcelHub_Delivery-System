package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.Payment;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.enums.PaymentStatus;
import com.parcelhub.logistics.repository.ParcelRepository;
import com.parcelhub.logistics.repository.PaymentRepository;
import com.parcelhub.logistics.service.GatewayPaymentService;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
class GatewayCallbackControllerTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private ParcelRepository parcelRepository;

	@Mock
	private GatewayPaymentService gatewayPaymentService;

	@Mock
	private RedirectAttributes redirectAttributes;

	@InjectMocks
	private GatewayCallbackController controller;

	private Payment paymentEntity;
	private Parcel parcelEntity;
	private Map<String, String> paramMap;
	private UUID parcelId;
	private String transactionId;

	@BeforeEach
	void setUp() {
		// Set the storePassword field using reflection
		setField(controller, "storePassword", "test-password");

		// Initialize test data
		transactionId = "TRANS123";
		parcelId = UUID.randomUUID();
		paymentEntity = new Payment();
		paymentEntity.setTransactionId(transactionId);
		paymentEntity.setAmount(new BigDecimal("100.00"));
		paymentEntity.setCurrency("BDT");
		paymentEntity.setPaymentStatus(PaymentStatus.PENDING);

		parcelEntity = new Parcel();
		parcelEntity.setId(parcelId);
		parcelEntity.setStatus(ParcelStatus.UNPAID);
		paymentEntity.setParcel(parcelEntity);

		paramMap = new HashMap<>();
		paramMap.put("tran_id", transactionId);
	}

	@Test
	void handlePaymentSuccess_ValidTransactionAndVerification_ReturnsRedirectWithSuccessMessage()
			throws IOException, NoSuchAlgorithmException {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentEntity));
		when(gatewayPaymentService.orderValidate(transactionId, "100.00", "BDT", paramMap))
				.thenReturn(true);
		when(paymentRepository.save(any(Payment.class))).thenReturn(paymentEntity);
		when(parcelRepository.save(any(Parcel.class))).thenReturn(parcelEntity);
		when(redirectAttributes.addFlashAttribute(eq("success"), anyString())).thenReturn(redirectAttributes);

		// Act
		String result = controller.handlePaymentSuccess(paramMap, redirectAttributes);

		// Assert
		assertEquals("redirect:/user/dashboard", result);
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(gatewayPaymentService).orderValidate(transactionId, "100.00", "BDT", paramMap);
		verify(paymentRepository).save(paymentEntity);
		verify(parcelRepository).save(parcelEntity);
		verify(redirectAttributes)
				.addFlashAttribute("success", "Parcel booked successfully. Relax and wait for the delivery.");
		assertEquals(PaymentStatus.SUCCESS, paymentEntity.getPaymentStatus());
		assertEquals(ParcelStatus.BOOKED, parcelEntity.getStatus());
	}

	@Test
	void handlePaymentSuccess_VerificationFails_ReturnsRedirectWithErrorMessage()
			throws IOException, NoSuchAlgorithmException {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentEntity));
		when(gatewayPaymentService.orderValidate(transactionId, "100.00", "BDT", paramMap))
				.thenReturn(false);
		when(redirectAttributes.addFlashAttribute(eq("error"), anyString())).thenReturn(redirectAttributes);

		// Act
		String result = controller.handlePaymentSuccess(paramMap, redirectAttributes);

		// Assert
		assertEquals("redirect:/user/dashboard", result);
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(gatewayPaymentService).orderValidate(transactionId, "100.00", "BDT", paramMap);
		verify(paymentRepository, never()).save(any());
		verify(parcelRepository, never()).save(any());
		verify(redirectAttributes).addFlashAttribute("error", "Payment verification failed.");
	}

	@Test
	void handlePaymentSuccess_InvalidTransactionId_ThrowsIllegalArgumentException()
			throws IOException, NoSuchAlgorithmException {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

		// Act & Assert
		try {
			controller.handlePaymentSuccess(paramMap, redirectAttributes);
		} catch (IllegalArgumentException | IOException | NoSuchAlgorithmException e) {
			assertEquals("Invalid transaction ID", e.getMessage());
		}
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(gatewayPaymentService, never()).orderValidate(anyString(), anyString(), anyString(), anyMap());
		verify(paymentRepository, never()).save(any());
		verify(parcelRepository, never()).save(any());
		verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
	}

	@Test
	void handlePaymentFailure_ValidTransactionId_UpdatesPaymentStatusAndRedirects() {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentEntity));
		when(paymentRepository.save(any(Payment.class))).thenReturn(paymentEntity);
		when(redirectAttributes.addFlashAttribute(eq("error"), anyString())).thenReturn(redirectAttributes);

		// Act
		String result = controller.handlePaymentFailure(paramMap, redirectAttributes);

		// Assert
		assertEquals("redirect:/user/dashboard", result);
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(paymentRepository).save(paymentEntity);
		verify(parcelRepository, never()).save(any());
		verify(redirectAttributes).addFlashAttribute("error", "Booking failed. Please try again.");
		assertEquals(PaymentStatus.FAILED, paymentEntity.getPaymentStatus());
	}

	@Test
	void handlePaymentFailure_InvalidTransactionId_ThrowsIllegalArgumentException() {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

		// Act & Assert
		try {
			controller.handlePaymentFailure(paramMap, redirectAttributes);
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid transaction ID", e.getMessage());
		}
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(paymentRepository, never()).save(any());
		verify(parcelRepository, never()).save(any());
		verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
	}

	@Test
	void handlePaymentCancel_ValidTransactionId_DeletesPaymentAndParcelAndRedirects() {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentEntity));
		doNothing().when(paymentRepository).delete(paymentEntity);
		doNothing().when(parcelRepository).deleteById(parcelId);
		when(redirectAttributes.addFlashAttribute(eq("error"), anyString())).thenReturn(redirectAttributes);

		// Act
		String result = controller.handlePaymentCancel(paramMap, redirectAttributes);

		// Assert
		assertEquals("redirect:/user/dashboard", result);
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(paymentRepository).delete(paymentEntity);
		verify(parcelRepository).deleteById(parcelId);
		verify(redirectAttributes).addFlashAttribute("error", "Booking Canceled.");
	}

	@Test
	void handlePaymentCancel_InvalidTransactionId_ThrowsIllegalArgumentException() {
		// Arrange
		when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

		// Act & Assert
		try {
			controller.handlePaymentCancel(paramMap, redirectAttributes);
		} catch (IllegalArgumentException e) {
			assertEquals("Invalid transaction ID", e.getMessage());
		}
		verify(paymentRepository).findByTransactionId(transactionId);
		verify(paymentRepository, never()).delete(any());
		verify(parcelRepository, never()).deleteById(any());
		verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
	}
}
