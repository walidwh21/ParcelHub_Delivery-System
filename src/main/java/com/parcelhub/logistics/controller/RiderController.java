package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.dto.request.RiderOnboardingRequest;
import com.parcelhub.logistics.dto.request.RiderRegistrationRequest;
import com.parcelhub.logistics.dto.response.OpenParcelView;
import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.Rider;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.service.RiderService;
import com.parcelhub.logistics.service.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/rider")
@RequiredArgsConstructor
public class RiderController {

	public static final String RIDER_REGISTERTION_URL = "auth/rider-register";
	public static final String ERROR = "error";
	public static final String SUCCESS = "success";
	public static final String REDIRECT_RIDER_ONBOARDING = "redirect:/rider/onboarding/";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String RIDER_ONBOARDING_DTO = "riderOnboardingDTO";
	public static final String PARCELS = "parcels";

	private final RiderService riderService;
	private final ShipmentService shipmentService;

	@GetMapping("/login")
	public String riderLogin(Model model) {
		return "auth/rider-login";
	}

	@GetMapping("/dashboard")
	public String riderDashboard(Model model) {
		model.addAttribute("riderDashboardResponseDTO", riderService.riderDashboardResponse());

		return "dashboard/rider-dashboard";
	}

	@GetMapping("/register")
	public String showRegistrationForm(Model model) {
		model.addAttribute("riderRegistrationRequestDTO", new RiderRegistrationRequest());
		return RIDER_REGISTERTION_URL;
	}

	@PostMapping("/register")
	public String submitRegistrationForm(
			@Valid @ModelAttribute("riderRegistrationRequestDTO") RiderRegistrationRequest riderRegistrationRequestDTO,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(ERROR);

			// Collect all validation error messages
			List<String> errorMessages = bindingResult.getAllErrors().stream()
					.map(error -> error.getDefaultMessage()) // Get default messages from dto
					.toList();

			// Add error messages to the model
			model.addAttribute("validationErrors", errorMessages);

			return RIDER_REGISTERTION_URL;
		}

		try {
			Rider savedRider = riderService.registerRider(riderRegistrationRequestDTO);

			redirectAttributes.addFlashAttribute(SUCCESS, "Rider Registration Successful"); // Add flash attribute
			return REDIRECT_RIDER_ONBOARDING + savedRider.getId();

		} catch (Exception e) {
			model.addAttribute(ERROR_MESSAGE, e.getMessage());
			return RIDER_REGISTERTION_URL;
		}
	}

	@GetMapping("/onboarding/{riderId}")
	public String showRiderOnboardingForm(@PathVariable UUID riderId, Model model) {

		try {
			Rider rider = riderService.findByRiderId(riderId);

			// sending rider to store rider ID
			model.addAttribute("rider", rider);

			// Check that previous any page redirect the DTO or Not, if not, then add new dto.
			if (!model.containsAttribute(RIDER_ONBOARDING_DTO)) {
				model.addAttribute(RIDER_ONBOARDING_DTO, new RiderOnboardingRequest()); // Initialize if missing
			}
			return "rider/onboarding";

		} catch (Exception e) {
			model.addAttribute(ERROR_MESSAGE, e.getMessage());
			return RIDER_REGISTERTION_URL;
		}
	}

	@PostMapping("/onboarding")
	public String submitRiderOnboardingForm(
			@RequestParam("riderId") UUID riderId,
			@Valid @ModelAttribute(RIDER_ONBOARDING_DTO) RiderOnboardingRequest riderOnboardingDTO,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute(ERROR);

			// Collect all validation error messages
			List<String> errorMessages = bindingResult.getAllErrors().stream()
					.map(error -> error.getDefaultMessage()) // Get default messages from dto
					.toList();

			// Add error messages to the model
			redirectAttributes.addFlashAttribute("validationErrors", errorMessages);

			// Return the DTO as FlashAttribute to next redirect get req. to retain the previous entered
			// data in field.
			redirectAttributes.addFlashAttribute(RIDER_ONBOARDING_DTO, riderOnboardingDTO);

			return REDIRECT_RIDER_ONBOARDING + riderId;
		}
		try {
			riderService.onboardRider(riderId, riderOnboardingDTO);

			redirectAttributes.addFlashAttribute(SUCCESS, "Rider Onboarded successfully. Verification Pending.");

			return "redirect:/rider/login";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Onboarding failed: " + e.getMessage());

			// Return the DTO as FlashAttribute to next redirect get req. to retain the previous entered
			// data in field.
			redirectAttributes.addFlashAttribute(RIDER_ONBOARDING_DTO, riderOnboardingDTO);

			return REDIRECT_RIDER_ONBOARDING + riderId;
		}
	}

	@PostMapping("/status")
	public String updateStatus(@RequestParam Boolean status) {
		riderService.updateRiderStatus(status);
		return "redirect:/rider/dashboard";
	}

	@GetMapping("/current-parcels")
	public String showCurrentParcelsRequest(Model model) {

		Rider rider = riderService.getAuthenticatedRider();
		if (rider.getIsAssigned()) {

			try {

				// A parcel is assigned to a rider with ASSIGNED, PICKED-UP, and IN_TRANSIT status
				List<Parcel> parcels = riderService.getAssignedParcelByRider(rider);
				if (parcels != null) {
					model.addAttribute(PARCELS, parcels);
				} else {
					model.addAttribute(ERROR, "No parcel assigned despite rider being marked as assigned");
				}

			} catch (Exception e) {
				model.addAttribute(ERROR, "Failed to load parcel : " + e.getMessage());
			}
			return "rider/view-assigned-parcels";
		} else {

			List<OpenParcelView> currentParcels = riderService.currentParcelsForRider();
			model.addAttribute(PARCELS, currentParcels);

			return "rider/view-current-parcels";
		}
	}

	@PostMapping("/Accept/{parcelId}")
	public String acceptParcel(@PathVariable UUID parcelId, RedirectAttributes redirectAttributes) {

		try {
			riderService.acceptParcelDelivery(parcelId);
			redirectAttributes.addFlashAttribute(SUCCESS, "Parcel accepted successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute(ERROR, "Failed to accept parcel: " + e.getMessage());
		}

		return "redirect:/rider/current-parcels";
	}

	@PostMapping("parcels/{parcelId}/update-status")
	public String updateAssignedParcelStatus(
			@PathVariable UUID parcelId,
			@RequestParam ParcelStatus status,
			RedirectAttributes redirectAttributes,
			Model model) {

		try {
			shipmentService.updateParcelStatus(parcelId, status);
			redirectAttributes.addFlashAttribute(SUCCESS, "Parcel status updated successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute(ERROR, "Failed to update status." + e.getMessage());
		}

		return "redirect:/rider/current-parcels";
	}

	@GetMapping("/parcels/history")
	public String showParcelHistory(Model model) {
		List<Parcel> parcelList = shipmentService.findCurrentRiderParcels();
		model.addAttribute(PARCELS, parcelList);
		return "rider/view-history";
	}
}
