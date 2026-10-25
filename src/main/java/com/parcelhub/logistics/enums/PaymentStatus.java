package com.parcelhub.logistics.enums;

public enum PaymentStatus {
	PENDING, // unpaid
	SUCCESS, // paid and confirmed
	FAILED, // payment failed
	CANCELLED, // payment cancelled by payer
	REFUNDED, // payment refunded
	PARTIALLY_REFUNDED, // payment partially refunded
	ACCEPTED // payment accepted by gateway but not confirmed
}
