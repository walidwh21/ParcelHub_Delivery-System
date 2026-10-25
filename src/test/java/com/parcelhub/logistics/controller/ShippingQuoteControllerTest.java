package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.ShippingQuoteRequest;
import com.parcelhub.logistics.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ShippingQuoteControllerTest {

	@Mock
	private ShipmentService shipmentService;

	private ShippingQuoteController controller;

	@BeforeEach
	void setUp() {
		controller = new ShippingQuoteController(shipmentService);
	}

	@Test
	void calculateShippingCost_shouldReturnExpectedCost() {
		ShippingQuoteRequest dto = new ShippingQuoteRequest(10.0, 5.0);
		double expectedCost = 107.0;
		when(shipmentService.estimateShippingCost(dto)).thenReturn(expectedCost);

		ResponseEntity<Double> response = controller.estimateShippingCost(dto);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(expectedCost, response.getBody());
		verify(shipmentService, times(1)).estimateShippingCost(dto);
	}

	@Test
	void calculateShippingCost_withZeroSizeAndWeight_shouldReturnBaseCharge() {
		ShippingQuoteRequest dto = new ShippingQuoteRequest(0.0, 0.0);
		double expectedCost = 100.0; // only delivery charge

		when(shipmentService.estimateShippingCost(dto)).thenReturn(expectedCost);

		ResponseEntity<Double> response = controller.estimateShippingCost(dto);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(expectedCost, response.getBody());
	}

	@Test
	void calculateShippingCost_withLargeValues_shouldReturnExpectedCost() {
		ShippingQuoteRequest dto = new ShippingQuoteRequest(1000.0, 500.0);
		double expectedCost = 0.2 * 1000 + 0.4 * 500 + 100; // 200 + 200 + 100 = 500

		when(shipmentService.estimateShippingCost(dto)).thenReturn(expectedCost);

		ResponseEntity<Double> response = controller.estimateShippingCost(dto);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(expectedCost, response.getBody());
	}

	@Test
	void calculateShippingCost_shouldThrowException_whenServiceFails() {
		ShippingQuoteRequest dto = new ShippingQuoteRequest(10.0, 5.0);
		when(shipmentService.estimateShippingCost(dto)).thenThrow(new RuntimeException("Internal error"));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			controller.estimateShippingCost(dto);
		});

		assertEquals("Internal error", exception.getMessage());
	}
}
