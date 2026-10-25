package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.AccountRegistrationRequest;
import com.parcelhub.logistics.service.UserService;
import com.parcelhub.logistics.util.SessionUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@InjectMocks
	private AuthController authController;

	@Mock
	private UserService userService;

	@Mock
	private Model model;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private RedirectAttributes redirectAttributes;

	// Tests for getRegistrationPage
	@Test
	void getRegistrationPage_authenticatedUser_redirectsToDashboard() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(true);
			String view = authController.getRegistrationPage(model);
			assertEquals("redirect:/user/dashboard", view);
			verifyNoInteractions(model);
			verifyNoInteractions(userService);
		}
	}

	@Test
	void getRegistrationPage_unauthenticatedUser_returnsRegisterView() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(false);
			String view = authController.getRegistrationPage(model);
			assertEquals("auth/register", view);
			verify(model).addAttribute(eq("userRegistrationRequestDTO"), any(AccountRegistrationRequest.class));
			verifyNoInteractions(userService);
		}
	}

	// Tests for submitRegistrationForm
	@Test
	void submitRegistrationForm_validationErrors_returnsRegisterViewWithErrors() {
		AccountRegistrationRequest dto = new AccountRegistrationRequest();
		when(bindingResult.hasErrors()).thenReturn(true);
		when(bindingResult.getAllErrors()).thenReturn(List.of());

		String view = authController.submitRegistrationForm(dto, bindingResult, model, redirectAttributes);

		assertEquals("auth/register", view);
		verify(model).addAttribute("error");
		verify(model).addAttribute(eq("validationErrors"), anyList());
		verifyNoInteractions(userService);
		verifyNoInteractions(redirectAttributes);
	}

	@Test
	void submitRegistrationForm_noValidationErrors_successfulRegistration_redirectsToLogin() {
		AccountRegistrationRequest dto = new AccountRegistrationRequest();
		when(bindingResult.hasErrors()).thenReturn(false);
		when(userService.registerUser(dto)).thenReturn("User registered successfully");

		String view = authController.submitRegistrationForm(dto, bindingResult, model, redirectAttributes);

		assertEquals("redirect:/auth/login", view);
		verify(redirectAttributes).addFlashAttribute("success", true);
		verifyNoInteractions(model);
	}

	@Test
	void submitRegistrationForm_noValidationErrors_registrationFails_returnsRegisterViewWithError() {
		AccountRegistrationRequest dto = new AccountRegistrationRequest();
		when(bindingResult.hasErrors()).thenReturn(false);
		when(userService.registerUser(dto)).thenThrow(new RuntimeException("Registration failed"));

		String view = authController.submitRegistrationForm(dto, bindingResult, model, redirectAttributes);

		assertEquals("auth/register", view);
		verify(model).addAttribute("errorMessage", "Registration failed");
		verifyNoInteractions(redirectAttributes);
	}

	// Tests for login
	@Test
	void login_authenticatedUser_redirectsToDashboard() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(true);
			String view = authController.login(model);
			assertEquals("redirect:/user/dashboard", view);
			verifyNoInteractions(model);
		}
	}

	@Test
	void login_unauthenticatedUser_returnsLoginView() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(false);
			String view = authController.login(model);
			assertEquals("auth/login", view);
			verifyNoInteractions(model);
		}
	}

	// Test for forgetPassword
	@Test
	void forgetPassword_returnsForgetPasswordView() {
		String view = authController.forgetPassword(model);
		assertEquals("auth/forget-password", view);
		verify(model).addAttribute("message", "Recover your account");
	}

	// Test for resetPassword
	@Test
	void resetPassword_returnsResetPasswordView() {
		String view = authController.resetPassword(model);
		assertEquals("auth/reset-password", view);
		verify(model).addAttribute("message", "Reset your password");
	}
}
