package com.parcelhub.logistics.util;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.service.UserService;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class ProfileFormUtil {

	private ProfileFormUtil() {}

	public static String handleUserProfileUpdate(
			ProfileUpdateRequest dto,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model,
			UserService userService,
			String viewPage,
			String redirectUrl) {

		if (bindingResult.hasErrors()) return viewPage;

		try {
			userService.updateUserProfile(dto);
		} catch (Exception e) {
			model.addAttribute("updateError", "An error occurred while updating your profile. Please try again.");
			return viewPage;
		}
		redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
		return "redirect:" + redirectUrl;
	}
}
