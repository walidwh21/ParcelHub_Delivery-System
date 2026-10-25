package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.dto.request.ParcelBookingRequest;
import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.repository.ParcelCategoryRepository;
import com.parcelhub.logistics.service.ShipmentService;
import com.parcelhub.logistics.service.GatewayPaymentService;
import com.parcelhub.logistics.service.UserService;
import com.parcelhub.logistics.util.ProfileFormUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;
	private static final String DASHBOARD_PROFILE_SETTINGS_PAGE = "dashboard/account";
	private final ParcelCategoryRepository categoryRepository;
	private final ShipmentService shipmentService;
	private final GatewayPaymentService gatewayPaymentService;

	@GetMapping("/dashboard")
	public String userDashboard(Model model) {
		List<Parcel> bookedParcels = shipmentService.findAwaitingPickup();
		model.addAttribute("bookedParcels", bookedParcels);
		return "dashboard/home";
	}

	@GetMapping("/profile-settings")
	public String basicProfileSettings(Model model) {
		model.addAttribute("profileUpdateRequest", userService.userProfileUpdateGet());
		return DASHBOARD_PROFILE_SETTINGS_PAGE;
	}

	@PostMapping("/profile-settings")
	public String updateAccountSettings(
			@Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest dto,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {

		return ProfileFormUtil.handleUserProfileUpdate(
				dto,
				bindingResult,
				redirectAttributes,
				model,
				userService,
				DASHBOARD_PROFILE_SETTINGS_PAGE,
				"/user/profile-settings?success");
	}

	@GetMapping("/book-parcel")
	public String createBooking(Model model) {
		model.addAttribute("productCategories", categoryRepository.findAll());
		model.addAttribute("title", "Book Parcel");
		model.addAttribute("bookingRequest", new ParcelBookingRequest());
		return "dashboard/book-parcel";
	}

	@PostMapping("/book-parcel")
	public String handleParcelBooking(
			@ModelAttribute @Valid ParcelBookingRequest bookingRequest,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("productCategories", categoryRepository.findAll());
			model.addAttribute("title", "Book Parcel");
			return "dashboard/book-parcel";
		}

		// save the parcel booking information
		bookingRequest.setTransactionId(shipmentService.issueTransactionRef());
		Parcel parcel = shipmentService.createBooking(bookingRequest);

		// save the payment information
		shipmentService.recordPendingPayment(parcel, bookingRequest);

		// fetch payment gateway url
		String paymentOperationUrl =
				gatewayPaymentService.getPaymentUrl(bookingRequest, parcel.getSender());

		return "redirect:" + paymentOperationUrl;
	}

	@GetMapping("/parcel-history")
	public String getParcelHistory(Model model) {
		List<Parcel> parcelHistory = shipmentService.findSenderHistory();
		model.addAttribute("parcelHistory", parcelHistory);
		return "dashboard/parcel-history";
	}
}
