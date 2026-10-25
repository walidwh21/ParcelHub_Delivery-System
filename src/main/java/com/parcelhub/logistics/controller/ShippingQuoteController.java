package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.dto.request.ShippingQuoteRequest;
import com.parcelhub.logistics.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parcel")
@RequiredArgsConstructor
public class ShippingQuoteController {
	public final ShipmentService shipmentService;

	@PostMapping("/shipping-cost")
	public ResponseEntity<Double> estimateShippingCost(@Valid @RequestBody ShippingQuoteRequest quoteRequest) {
		return ResponseEntity.ok().body(shipmentService.estimateShippingCost(quoteRequest));
	}
}
