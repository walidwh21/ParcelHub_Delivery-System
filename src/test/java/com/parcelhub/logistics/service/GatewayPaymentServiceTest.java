package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.paymentapiresponse.GatewayInitResponse;
import com.parcelhub.logistics.dto.paymentapiresponse.GatewayValidationResponse;
import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.util.GatewayHttpUtil;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GatewayPaymentServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private HashVerificationService hashVerificationService;

	private GatewayPaymentService paymentService;

	private ParcelBookingRequest bookingRequest;
	private Account sender;
	private Map<String, String> requestParameters;

	private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@BeforeEach
	void setUp() throws Exception {
		paymentService = new GatewayPaymentService(restTemplate, hashVerificationService);
		setPrivateField(paymentService, "storeId", "testStoreId");
		setPrivateField(paymentService, "storePasswd", "testStorePasswd");
		setPrivateField(paymentService, "paymentInitializationUrl", "http://test.url/init");
		setPrivateField(paymentService, "baseUrl", "http://base.url");
		setPrivateField(paymentService, "sslczURL", "http://sslcommerz.url");

		bookingRequest = new ParcelBookingRequest();
		bookingRequest.setPrice(new BigDecimal("100.00"));
		bookingRequest.setTransactionId("tran123");
		bookingRequest.setPaymentMethod("visa");
		bookingRequest.setCategoryId(UUID.randomUUID());

		sender = new Account();
		sender.setFullName("John Doe");
		sender.setEmail("john@example.com");

		requestParameters = new HashMap<>();
		requestParameters.put("val_id", "testValId");
	}

	// Tests for getPaymentUrl

	@Test
	void testGetPaymentUrl_Success() {
		GatewayInitResponse responseDto = new GatewayInitResponse();
		responseDto.setStatus("SUCCESS");
		GatewayInitResponse.Desc desc = new GatewayInitResponse.Desc();
		desc.setGw("visa");
		desc.setRedirectGatewayURL("http://redirect.url");
		responseDto.setDesc(Collections.singletonList(desc));

		ResponseEntity<GatewayInitResponse> responseEntity =
				new ResponseEntity<>(responseDto, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GatewayInitResponse.class)))
				.thenReturn(responseEntity);

		String result = paymentService.getPaymentUrl(bookingRequest, sender);
		assertEquals("http://redirect.url", result);
	}

	@Test
	void testGetPaymentUrl_Failure() {
		GatewayInitResponse responseDto = new GatewayInitResponse();
		responseDto.setStatus("FAILED");
		responseDto.setFailedreason("Invalid data");

		ResponseEntity<GatewayInitResponse> responseEntity =
				new ResponseEntity<>(responseDto, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GatewayInitResponse.class)))
				.thenReturn(responseEntity);

		Exception exception =
				assertThrows(Exception.class, () -> paymentService.getPaymentUrl(bookingRequest, sender));
		assertTrue(exception.getMessage().contains("Failed: Invalid data"));
	}

	@Test
	void testGetPaymentUrl_NoMatchingGateway_ThrowsException() {
		GatewayInitResponse responseDto = new GatewayInitResponse();
		responseDto.setStatus("SUCCESS");
		GatewayInitResponse.Desc desc = new GatewayInitResponse.Desc();
		desc.setGw("mastercard");
		desc.setRedirectGatewayURL("http://redirect.url");
		responseDto.setDesc(Collections.singletonList(desc));

		ResponseEntity<GatewayInitResponse> responseEntity =
				new ResponseEntity<>(responseDto, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GatewayInitResponse.class)))
				.thenReturn(responseEntity);

		bookingRequest.setPaymentMethod("visa");
		RuntimeException exception = assertThrows(
				RuntimeException.class, () -> paymentService.getPaymentUrl(bookingRequest, sender));
		assertEquals("Payment method not found", exception.getMessage());
	}

	@Test
	void testGetPaymentUrl_NonOkStatusCode_ThrowsException() {
		ResponseEntity<GatewayInitResponse> responseEntity =
				new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GatewayInitResponse.class)))
				.thenReturn(responseEntity);

		Exception exception =
				assertThrows(Exception.class, () -> paymentService.getPaymentUrl(bookingRequest, sender));
		assertTrue(exception.getMessage().contains("Failed to send payment request"));
	}

	@Test
	void testGetPaymentUrl_NullResponseBody_ThrowsException() {
		ResponseEntity<GatewayInitResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(GatewayInitResponse.class)))
				.thenReturn(responseEntity);

		Exception exception =
				assertThrows(Exception.class, () -> paymentService.getPaymentUrl(bookingRequest, sender));
		assertTrue(exception.getMessage().contains("Failed: No response body"));
	}

	@Test
	void testOrderValidate_HashVerificationFails() throws Exception {
		Map<String, String> inputParameters = new HashMap<>();
		requestParameters.put("val_id", "val123");

		when(hashVerificationService.verifyIPNHash(anyMap(), eq("testStorePasswd")))
				.thenReturn(false);

		boolean result = paymentService.orderValidate("tran123", "100.00", "BDT", inputParameters);
		assertFalse(result);
	}

	@Test
	void buildValidationUrl_ValidParameters_ReturnsCorrectlyEncodedUrl() {
		// Act
		String result = invokeBuildValidationUrl(requestParameters);

		// Assert
		String expectedUrl = "http://sslcommerz.url/validator/api/validationserverAPI.php?val_id=testValId"
				+ "&store_id=testStoreId&store_passwd=testStorePasswd&v=1&format=json";
		assertEquals(expectedUrl, result);
	}

	@Test
	void getValidatedResponse_EmptyJsonResponse_ReturnsNull() {
		// Arrange
		try (MockedStatic<GatewayHttpUtil> mockedUtil = Mockito.mockStatic(GatewayHttpUtil.class)) {
			mockedUtil
					.when(() -> GatewayHttpUtil.getByOpeningJavaUrlConnection(anyString()))
					.thenReturn("");

			// Act
			GatewayValidationResponse result = invokeGetValidatedResponse(requestParameters);

			// Assert
			assertNull(result);
			mockedUtil.verify(() -> GatewayHttpUtil.extractValidatorResponse(anyString()), Mockito.never());
		}
	}

	// Helper methods to invoke private methods using reflection
	private String invokeBuildValidationUrl(Map<String, String> params) {
		try {
			java.lang.reflect.Method method =
					GatewayPaymentService.class.getDeclaredMethod("buildValidationUrl", Map.class);
			method.setAccessible(true);
			return (String) method.invoke(paymentService, params);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke buildValidationUrl", e);
		}
	}

	private GatewayValidationResponse invokeGetValidatedResponse(Map<String, String> params) {
		try {
			java.lang.reflect.Method method =
					GatewayPaymentService.class.getDeclaredMethod("getValidatedResponse", Map.class);
			method.setAccessible(true);
			return (GatewayValidationResponse) method.invoke(paymentService, params);
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke getValidatedResponse", e);
		}
	}
}
