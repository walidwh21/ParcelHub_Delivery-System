package com.parcelhub.logistics.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

class SessionUtilTest {

	@Test
	void testGetAuthenticatedUsername_WithUserDetails() {
		// Arrange: Create mock objects
		SecurityContext mockContext = mock(SecurityContext.class);
		Authentication mockAuth = mock(Authentication.class);
		UserDetails mockUserDetails = mock(UserDetails.class);

		when(mockUserDetails.getUsername()).thenReturn("testuser");
		when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
		when(mockContext.getAuthentication()).thenReturn(mockAuth);

		// Set SecurityContext in SecurityContextHolder
		SecurityContextHolder.setContext(mockContext);

		// Act
		String username = SessionUtil.currentUserEmail();

		// Assert
		assertEquals("testuser", username);
	}

	@Test
	void testGetAuthenticatedUsername_WithNonUserDetails() {
		// Arrange: Create mock objects
		SecurityContext mockContext = mock(SecurityContext.class);
		Authentication mockAuth = mock(Authentication.class);

		when(mockAuth.getPrincipal()).thenReturn("nonUser");
		when(mockContext.getAuthentication()).thenReturn(mockAuth);

		// Set SecurityContext in SecurityContextHolder
		SecurityContextHolder.setContext(mockContext);

		// Act
		String username = SessionUtil.currentUserEmail();

		// Assert
		assertEquals("nonUser", username);
	}

	@Test
	void testGetAuthenticatedUsername_WithNullAuthentication() {
		// Arrange: Create mock objects
		SecurityContext mockContext = mock(SecurityContext.class);
		when(mockContext.getAuthentication()).thenReturn(null);
		SecurityContextHolder.setContext(mockContext);

		// Act
		String username = SessionUtil.currentUserEmail();

		// Assert
		assertNull(username);
	}

	@Test
	void testIsAuthenticated_WithAuthenticatedUser() {
		// Arrange
		SecurityContext mockContext = mock(SecurityContext.class);
		Authentication mockAuth = mock(Authentication.class);
		UserDetails mockUserDetails = mock(UserDetails.class);

		when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
		when(mockContext.getAuthentication()).thenReturn(mockAuth);
		SecurityContextHolder.setContext(mockContext);

		// Act
		boolean hasActiveSession = SessionUtil.hasActiveSession();

		// Assert
		assertTrue(hasActiveSession);
	}

	@Test
	void testIsAuthenticated_WithNullAuthentication() {
		// Arrange
		SecurityContext mockContext = mock(SecurityContext.class);
		when(mockContext.getAuthentication()).thenReturn(null);
		SecurityContextHolder.setContext(mockContext);

		// Act
		boolean hasActiveSession = SessionUtil.hasActiveSession();

		// Assert
		assertFalse(hasActiveSession);
	}

	@Test
	void testGetAuthenticatedUserRoles_WithNoRoles() {
		// Arrange
		SecurityContext mockContext = mock(SecurityContext.class);
		Authentication mockAuth = mock(Authentication.class);

		when(mockAuth.getAuthorities()).thenReturn(List.of());
		when(mockContext.getAuthentication()).thenReturn(mockAuth);
		SecurityContextHolder.setContext(mockContext);

		// Act
		List<String> roles = SessionUtil.currentUserRoles();

		// Assert
		assertTrue(roles.isEmpty());
	}

	@Test
	void testGetAuthenticatedUserRoles_WithNullAuthentication() {
		// Arrange
		SecurityContext mockContext = mock(SecurityContext.class);
		when(mockContext.getAuthentication()).thenReturn(null);
		SecurityContextHolder.setContext(mockContext);

		// Act
		List<String> roles = SessionUtil.currentUserRoles();

		// Assert
		assertTrue(roles.isEmpty());
	}
}
