package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.Payment;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.enums.PaymentStatus;
import com.parcelhub.logistics.repository.ParcelRepository;
import com.parcelhub.logistics.repository.PaymentRepository;
import com.parcelhub.logistics.service.GatewayPaymentService;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sslcommerz")
@RequiredArgsConstructor
public class GatewayCallbackController {
	public static final String TRAN_ID = "tran_id";
	public static final String INVALID_TRANSACTION_ID = "Invalid transaction ID";
	public static final String ERROR = "error";
	public static final String REDIRECT_USER_DASHBOARD = "redirect:/user/dashboard";

	private final PaymentRepository paymentRepository;
	private final ParcelRepository parcelRepository;
	private final GatewayPaymentService gatewayPaymentService;

	@Value("${sslcommerz.store-passwd}")
	private String storePassword;

	@PostMapping("/success")
	public String handlePaymentSuccess(
			@RequestParam Map<String, String> paramMap, RedirectAttributes redirectAttributes)
			throws IOException, NoSuchAlgorithmException {
		// Extract transaction details
		String transactionId = paramMap.get(TRAN_ID);

		// fetch payment information from database
		Payment payment = paymentRepository
				.findByTransactionId(transactionId)
				.orElseThrow(() -> new IllegalArgumentException(INVALID_TRANSACTION_ID));
		String marchentTransactionId = payment.getTransactionId();
		String marchentTransactionAmount = payment.getAmount().toString();
		String marchentTransactionCurrency = payment.getCurrency();

		// Verify payment
		if (!gatewayPaymentService.orderValidate(
				marchentTransactionId, marchentTransactionAmount, marchentTransactionCurrency, paramMap)) {
			redirectAttributes.addFlashAttribute(ERROR, "Payment verification failed.");
			return REDIRECT_USER_DASHBOARD;
		}

		// Update payment status
		payment.setPaymentStatus(PaymentStatus.SUCCESS);
		paymentRepository.save(payment);

		// upate parcel status
		Parcel parcel = payment.getParcel();
		parcel.setStatus(ParcelStatus.BOOKED);
		parcelRepository.save(parcel);

		redirectAttributes.addFlashAttribute("success", "Parcel booked successfully. Relax and wait for the delivery.");
		return REDIRECT_USER_DASHBOARD;
	}

	@PostMapping("/failure")
	public String handlePaymentFailure(
			@RequestParam Map<String, String> paramMap, RedirectAttributes redirectAttributes) {
		// Extract transaction details
		String transactionId = paramMap.get(TRAN_ID);

		// fetch payment information from database
		Payment payment = paymentRepository
				.findByTransactionId(transactionId)
				.orElseThrow(() -> new IllegalArgumentException(INVALID_TRANSACTION_ID));

		// Update payment status
		payment.setPaymentStatus(PaymentStatus.FAILED);
		paymentRepository.save(payment);

		redirectAttributes.addFlashAttribute(ERROR, "Booking failed. Please try again.");
		return REDIRECT_USER_DASHBOARD;
	}

	@PostMapping("/cancel")
	public String handlePaymentCancel(
			@RequestParam Map<String, String> paramMap, RedirectAttributes redirectAttributes) {
		// Extract transaction details
		String transactionId = paramMap.get(TRAN_ID);

		// fetch payment information from database
		Payment payment = paymentRepository
				.findByTransactionId(transactionId)
				.orElseThrow(() -> new IllegalArgumentException(INVALID_TRANSACTION_ID));

		// Delete the booking from database along with payment information
		UUID parcelId = payment.getParcel().getId();
		paymentRepository.delete(payment);
		parcelRepository.deleteById(parcelId);

		redirectAttributes.addFlashAttribute(ERROR, "Booking Canceled.");
		return REDIRECT_USER_DASHBOARD;
	}
}
