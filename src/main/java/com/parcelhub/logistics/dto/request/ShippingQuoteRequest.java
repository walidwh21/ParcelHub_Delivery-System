package com.parcelhub.logistics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteRequest {

	@NotNull(message = "Size cannot be null")
	@Min(value = 0, message = "Size must be greater than or equal to 0")
	private Double size;

	@NotNull(message = "Weight cannot be null")
	@Min(value = 0, message = "Weight must be greater than or equal to 0")
	private Double weight;
}
