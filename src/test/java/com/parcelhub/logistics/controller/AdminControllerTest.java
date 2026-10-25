package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.dto.response.RiderApprovalView;
import com.parcelhub.logistics.dto.response.RiderProfileView;
import com.parcelhub.logistics.enums.VerificationStatus;
import com.parcelhub.logistics.service.RiderService;
import com.parcelhub.logistics.service.UserService;
import com.parcelhub.logistics.util.ProfileFormUtil;
import com.parcelhub.logistics.util.SessionUtil;
import java.util.Collections;
import java.util.UUID;
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
class AdminControllerTest {

	@InjectMocks
	private AdminController adminController;

	@Mock
	private UserService userService;

	@Mock
	private RiderService riderService;

	@Mock
	private Model model;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private RedirectAttributes redirectAttributes;

	@Test
	void login_authenticatedUser_redirectsToDashboard() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(true);
			String view = adminController.login();
			assertEquals("redirect:/admin/dashboard", view);
			verifyNoInteractions(model);
			verifyNoInteractions(userService);
			verifyNoInteractions(riderService);
		}
	}

	@Test
	void login_unauthenticatedUser_returnsLoginPage() {
		try (MockedStatic<SessionUtil> mockedAuthUtil = mockStatic(SessionUtil.class)) {
			mockedAuthUtil.when(SessionUtil::hasActiveSession).thenReturn(false);
			String view = adminController.login();
			assertEquals("auth/admin-login", view);
			verifyNoInteractions(model);
			verifyNoInteractions(userService);
			verifyNoInteractions(riderService);
		}
	}

	@Test
	void adminDashboard_returnsDashboardView() {
		String view = adminController.adminDashboard();
		assertEquals("dashboard/admin-dashboard", view);
		verifyNoInteractions(model);
		verifyNoInteractions(userService);
		verifyNoInteractions(riderService);
	}

	@Test
	void basicProfileSettings_populatesModelAndReturnsView() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		when(userService.userProfileUpdateGet()).thenReturn(dto);
		String view = adminController.basicProfileSettings(model);
		assertEquals("dashboard/admin-account", view);
		verify(model).addAttribute("profileUpdateRequest", dto);
		verify(userService).userProfileUpdateGet();
		verifyNoInteractions(riderService);
	}

	@Test
	void updateAccountSettings_validationErrors_returnsProfileSettingsPage() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		when(bindingResult.hasErrors()).thenReturn(true);
		try (MockedStatic<ProfileFormUtil> mockedProfileUtil = mockStatic(ProfileFormUtil.class)) {
			mockedProfileUtil
					.when(() -> ProfileFormUtil.handleUserProfileUpdate(
							eq(dto),
							eq(bindingResult),
							eq(redirectAttributes),
							eq(model),
							eq(userService),
							eq("dashboard/admin-account"),
							anyString()))
					.thenCallRealMethod();
			String view = adminController.updateAccountSettings(dto, bindingResult, redirectAttributes, model);
			assertEquals("dashboard/admin-account", view);
			verify(bindingResult).hasErrors();
			verifyNoInteractions(userService);
			verifyNoInteractions(riderService);
		}
	}

	@Test
	void updateAccountSettings_noValidationErrors_successfulUpdate_redirects() {
		// Arrange
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		when(bindingResult.hasErrors()).thenReturn(false);

		// Act
		String view = adminController.updateAccountSettings(dto, bindingResult, redirectAttributes, model);

		// Assert
		assertEquals("redirect:/admin/profile-settings?success", view);
		verify(bindingResult).hasErrors();
		verify(userService).updateUserProfile(dto);
		verify(redirectAttributes).addFlashAttribute("successMessage", "Profile updated successfully!");
		verifyNoInteractions(riderService);
	}

	@Test
	void updateAccountSettings_updateFails_returnsProfileSettingsPageWithError() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		when(bindingResult.hasErrors()).thenReturn(false);
		doThrow(new RuntimeException("Update failed")).when(userService).updateUserProfile(dto);
		try (MockedStatic<ProfileFormUtil> mockedProfileUtil = mockStatic(ProfileFormUtil.class)) {
			mockedProfileUtil
					.when(() -> ProfileFormUtil.handleUserProfileUpdate(
							eq(dto),
							eq(bindingResult),
							eq(redirectAttributes),
							eq(model),
							eq(userService),
							eq("dashboard/admin-account"),
							eq("/admin/profile-settings?success")))
					.thenCallRealMethod();
			String view = adminController.updateAccountSettings(dto, bindingResult, redirectAttributes, model);
			assertEquals("dashboard/admin-account", view);
			verify(bindingResult).hasErrors();
			verify(userService).updateUserProfile(dto);
			verify(model)
					.addAttribute("updateError", "An error occurred while updating your profile. Please try again.");
			verifyNoInteractions(riderService);
		}
	}

	@Test
	void approvalPendingRiders_populatesModelAndReturnsView() {
		RiderApprovalView riderDto =
				new RiderApprovalView(UUID.randomUUID(), "John Doe", "john@example.com", "1234567890");
		when(riderService.getPendingRiders()).thenReturn(Collections.singletonList(riderDto));
		String view = adminController.approvalPendingRiders(model);
		assertEquals("admin/pending-riders", view);
		verify(riderService).getPendingRiders();
		verify(model).addAttribute("riders", Collections.singletonList(riderDto));
		verifyNoInteractions(userService);
	}

	@Test
	void approveRider_successfulApproval_redirectsWithSuccessMessage() {
		UUID riderId = UUID.randomUUID();
		doNothing().when(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.VERIFIED);
		String view = adminController.approveRider(riderId, redirectAttributes);
		assertEquals("redirect:/admin/riders/pending", view);
		verify(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.VERIFIED);
		verify(redirectAttributes).addFlashAttribute("message", "Rider approved successfully");
		verifyNoInteractions(userService);
	}

	@Test
	void approveRider_failure_redirectsWithErrorMessage() {
		UUID riderId = UUID.randomUUID();
		doThrow(new RuntimeException("Approval failed"))
				.when(riderService)
				.updateRiderVerificationStatus(riderId, VerificationStatus.VERIFIED);
		String view = adminController.approveRider(riderId, redirectAttributes);
		assertEquals("redirect:/admin/riders/pending", view);
		verify(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.VERIFIED);
		verify(redirectAttributes).addFlashAttribute("error", "Failed to approve rider");
		verifyNoInteractions(userService);
	}

	@Test
	void rejectRider_successfulRejection_redirectsWithSuccessMessage() {
		UUID riderId = UUID.randomUUID();
		doNothing().when(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.REJECTED);
		String view = adminController.rejectRider(riderId, redirectAttributes);
		assertEquals("redirect:/admin/riders/pending", view);
		verify(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.REJECTED);
		verify(redirectAttributes).addFlashAttribute("message", "Rider rejected successfully");
		verifyNoInteractions(userService);
	}

	@Test
	void rejectRider_failure_redirectsWithErrorMessage() {
		UUID riderId = UUID.randomUUID();
		doThrow(new RuntimeException("Rejection failed"))
				.when(riderService)
				.updateRiderVerificationStatus(riderId, VerificationStatus.REJECTED);
		String view = adminController.rejectRider(riderId, redirectAttributes);
		assertEquals("redirect:/admin/riders/pending", view);
		verify(riderService).updateRiderVerificationStatus(riderId, VerificationStatus.REJECTED);
		verify(redirectAttributes).addFlashAttribute("error", "Failed to reject rider");
		verifyNoInteractions(userService);
	}

	@Test
	void viewRiderDetails_populatesModelAndReturnsView() {
		UUID riderId = UUID.randomUUID();
		RiderProfileView riderDto = new RiderProfileView();
		riderDto.setId(riderId);
		riderDto.setFullName("John Doe");
		when(riderService.getRiderDetails(riderId)).thenReturn(riderDto);
		String view = adminController.viewRiderDetails(riderId, model);
		assertEquals("admin/view-rider-details", view);
		verify(riderService).getRiderDetails(riderId);
		verify(model).addAttribute("rider", riderDto);
		verifyNoInteractions(userService);
	}
}
