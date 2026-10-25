package com.parcelhub.logistics.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

class ProfileFormUtilTest {

	@Test
	void testHandleUserProfileUpdate_HasErrors() {
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = mock(Model.class);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		UserService userService = mock(UserService.class);
		ProfileUpdateRequest dto = new ProfileUpdateRequest();

		when(bindingResult.hasErrors()).thenReturn(true);

		String result = ProfileFormUtil.handleUserProfileUpdate(
				dto, bindingResult, redirectAttributes, model, userService, "profile-page", "/success");

		assertEquals("profile-page", result);
	}

	@Test
	void testHandleUserProfileUpdate_Success() {
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = mock(Model.class);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		UserService userService = mock(UserService.class);
		ProfileUpdateRequest dto = new ProfileUpdateRequest();

		when(bindingResult.hasErrors()).thenReturn(false);

		String result = ProfileFormUtil.handleUserProfileUpdate(
				dto,
				bindingResult,
				redirectAttributes,
				model,
				userService,
				"profile-page",
				"/profile-settings?success");

		assertEquals("redirect:/profile-settings?success", result);
		verify(userService, times(1)).updateUserProfile(dto);
		verify(redirectAttributes, times(1)).addFlashAttribute("successMessage", "Profile updated successfully!");
	}

	@Test
	void testHandleUserProfileUpdate_ExceptionThrown() {
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = mock(Model.class);
		RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
		UserService userService = mock(UserService.class);
		ProfileUpdateRequest dto = new ProfileUpdateRequest();

		when(bindingResult.hasErrors()).thenReturn(false);
		doThrow(new RuntimeException("update failed")).when(userService).updateUserProfile(dto);

		String result = ProfileFormUtil.handleUserProfileUpdate(
				dto, bindingResult, redirectAttributes, model, userService, "profile-page", "/success");

		assertEquals("profile-page", result);
		verify(model, times(1)).addAttribute(eq("updateError"), anyString());
	}
}
