package com.parcelhub.logistics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.util.SessionUtil;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.ui.Model;

class PublicControllerTest {

	private PublicController publicController;
	private Model model;
	private MockedStatic<SessionUtil> authUtilMock;

	@BeforeEach
	void setUp() {
		publicController = new PublicController();
		model = mock(Model.class);
		authUtilMock = mockStatic(SessionUtil.class);
	}

	@AfterEach
	void tearDown() {
		authUtilMock.close();
	}

	@ParameterizedTest
	@CsvSource({
		"ROLE_ADMIN, redirect:/admin/dashboard",
		"ROLE_USER, redirect:/user/dashboard",
		"ROLE_RIDER, redirect:/rider/dashboard"
	})
	void home_authenticatedUser_redirectsBasedOnRole(String role, String expectedRedirect) {
		when(SessionUtil.hasActiveSession()).thenReturn(true);
		when(SessionUtil.currentUserRoles()).thenReturn(List.of(role));

		String viewName = publicController.home(model);
		assertEquals(expectedRedirect, viewName);
	}

	@Test
	void home_notAuthenticated_returnsIndexPage() {
		authUtilMock.when(SessionUtil::hasActiveSession).thenReturn(false);

		String viewName = publicController.home(model);

		verify(model).addAttribute("title", "Dashboard - Home");
		assertEquals("index", viewName);
	}

	@Test
	void home_authenticatedWithNoRoles_returnsIndexPage() {
		authUtilMock.when(SessionUtil::hasActiveSession).thenReturn(true);
		authUtilMock.when(SessionUtil::currentUserRoles).thenReturn(List.of());

		String viewName = publicController.home(model);

		verify(model).addAttribute("title", "Dashboard - Home");
		assertEquals("index", viewName);
	}
}
