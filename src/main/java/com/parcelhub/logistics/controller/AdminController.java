package com.parcelhub.logistics.controller;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.dto.response.RiderApprovalView;
import com.parcelhub.logistics.dto.response.RiderProfileView;
import com.parcelhub.logistics.enums.VerificationStatus;
import com.parcelhub.logistics.service.RiderService;
import com.parcelhub.logistics.service.UserService;
import com.parcelhub.logistics.util.SessionUtil;
import com.parcelhub.logistics.util.ProfileFormUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	private static final String DASHBOARD_PROFILE_SETTINGS_PAGE = "dashboard/admin-account";

	private final UserService userService;
	private final RiderService riderService;

	@GetMapping("/login")
	public String login() {
		if (SessionUtil.hasActiveSession()) return "redirect:/admin/dashboard";
		return "auth/admin-login";
	}

	@GetMapping("/dashboard")
	public String adminDashboard() {
		return "dashboard/admin-dashboard"; // Renders `admin-dashboard.html`
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
				"/admin/profile-settings?success");
	}

	@GetMapping("/riders/pending")
	public String approvalPendingRiders(Model model) {

		List<RiderApprovalView> pendingRiders = riderService.getPendingRiders();

		model.addAttribute("riders", pendingRiders);

		return "admin/pending-riders";
	}

	@PostMapping("/rider/approve")
	public String approveRider(@RequestParam("riderId") UUID riderId, RedirectAttributes redirectAttributes) {

		try {
			riderService.updateRiderVerificationStatus(riderId, VerificationStatus.VERIFIED);

			redirectAttributes.addFlashAttribute("message", "Rider approved successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to approve rider");
		}
		return "redirect:/admin/riders/pending";
	}

	@PostMapping("/rider/reject")
	public String rejectRider(@RequestParam("riderId") UUID riderId, RedirectAttributes redirectAttributes) {

		try {
			riderService.updateRiderVerificationStatus(riderId, VerificationStatus.REJECTED);

			redirectAttributes.addFlashAttribute("message", "Rider rejected successfully");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to reject rider");
		}
		return "redirect:/admin/riders/pending";
	}

	@GetMapping("/rider/view/{id}")
	public String viewRiderDetails(@PathVariable("id") UUID riderId, Model model) {
		RiderProfileView rider = riderService.getRiderDetails(riderId);
		model.addAttribute("rider", rider);
		return "admin/view-rider-details";
	}
}
