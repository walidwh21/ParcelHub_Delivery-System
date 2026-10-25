package com.parcelhub.logistics.service;

import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.entity.Account;

public interface PaymentService {
	/*
	 * This method will initiate payment and fetch the payment url
	 * Then return the url to controller layer
	 */
	String getPaymentUrl(ParcelBookingRequest bookingRequest, Account sender);
}
