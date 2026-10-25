package com.parcelhub.logistics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.dto.request.AccountRegistrationRequest;
import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.enums.Role;
import com.parcelhub.logistics.exception.AlreadyExistsException;
import com.parcelhub.logistics.exception.UserNotFoundException;
import com.parcelhub.logistics.repository.UserRepository;
import com.parcelhub.logistics.util.SessionUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	private Account user;
	private String email = "test@example.com";
	private MockedStatic<SessionUtil> authUtilMockedStatic;

	@BeforeEach
	void setUp() {
		user = new Account();
		user.setId(UUID.randomUUID());
		user.setFullName("Test User");
		user.setEmail(email);
		user.setPassword("encodedPassword");
		user.setPhoneNumber("1234567890");
		user.setProfilePicture("http://example.com/profile.jpg");
		user.setRoles(new HashSet<>(Collections.singletonList(Role.ROLE_USER)));

		// Initialize static mock for SessionUtil
		authUtilMockedStatic = mockStatic(SessionUtil.class);
	}

	@AfterEach
	void tearDown() {
		// Close the static mock after each test
		authUtilMockedStatic.close();
	}

	@Test
	void testIsEmailAlreadyInUse_EmailExists() {
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		assertTrue(userService.isEmailAlreadyInUse(email));
		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	void testIsEmailAlreadyInUse_EmailDoesNotExist() {
		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		assertFalse(userService.isEmailAlreadyInUse(email));
		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	void testRegisterUser_Success() {
		AccountRegistrationRequest dto = new AccountRegistrationRequest();
		dto.setFullName("New User");
		dto.setEmail(email);
		dto.setPassword("password123");

		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(userRepository.save(any(Account.class))).thenReturn(user);

		String result = userService.registerUser(dto);

		assertEquals("User registered successfully", result);
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, times(1)).encode("password123");
		verify(userRepository, times(1)).save(any(Account.class));
	}

	@Test
	void testRegisterUser_EmailAlreadyInUse() {
		AccountRegistrationRequest dto = new AccountRegistrationRequest();
		dto.setEmail(email);

		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		assertThrows(AlreadyExistsException.class, () -> userService.registerUser(dto));
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any(Account.class));
	}

	@Test
	void testGetAuthenticatedUser_Success() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		Account result = userService.getAuthenticatedUser();

		assertEquals(user, result);
		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	void testGetAuthenticatedUser_UserNotFound() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> userService.getAuthenticatedUser());
		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	void testUserProfileUpdateGet_Success() {
		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		ProfileUpdateRequest result = userService.userProfileUpdateGet();

		assertEquals(user.getFullName(), result.getFullName());
		assertEquals(user.getPassword(), result.getPassword());
		assertEquals(user.getPhoneNumber(), result.getPhoneNumber());
		assertEquals(user.getProfilePicture(), result.getProfileImageUrl());
		verify(userRepository, times(1)).findByEmail(email);
	}

	@Test
	void testUpdateUserProfile_FullUpdate() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		dto.setFullName("Updated Name");
		dto.setPassword("newPassword");
		dto.setPhoneNumber("0987654321");
		dto.setProfileImageUrl("http://example.com/new.jpg");

		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
		when(userRepository.save(any(Account.class))).thenReturn(user);

		boolean result = userService.updateUserProfile(dto);

		assertTrue(result);
		assertEquals("Updated Name", user.getFullName());
		assertEquals("encodedNewPassword", user.getPassword());
		assertEquals("0987654321", user.getPhoneNumber());
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, times(1)).encode("newPassword");
		verify(userRepository, times(1)).save(user);
	}

	@Test
	void testUpdateUserProfile_PartialUpdate_NoPassword() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		dto.setFullName("Updated Name");
		dto.setPhoneNumber("0987654321");

		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		when(userRepository.save(any(Account.class))).thenReturn(user);

		boolean result = userService.updateUserProfile(dto);

		assertTrue(result);
		assertEquals("Updated Name", user.getFullName());
		assertEquals("encodedPassword", user.getPassword()); // Original password remains
		assertEquals("0987654321", user.getPhoneNumber());
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, times(1)).save(user);
	}

	@Test
	void testUpdateUserProfile_EmptyPassword() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		dto.setFullName("Updated Name");
		dto.setPassword(""); // Empty password
		dto.setPhoneNumber("0987654321");

		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		when(userRepository.save(any(Account.class))).thenReturn(user);

		boolean result = userService.updateUserProfile(dto);

		assertTrue(result);
		assertEquals("Updated Name", user.getFullName());
		assertEquals("encodedPassword", user.getPassword()); // Password unchanged
		assertEquals("0987654321", user.getPhoneNumber());
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, times(1)).save(user);
	}

	@Test
	void testUpdateUserProfile_NullFields() {
		ProfileUpdateRequest dto = new ProfileUpdateRequest();
		dto.setFullName(null); // Null field
		dto.setPassword(null); // Null password
		dto.setPhoneNumber(null); // Null field

		authUtilMockedStatic.when(SessionUtil::currentUserEmail).thenReturn(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		when(userRepository.save(any(Account.class))).thenReturn(user);

		boolean result = userService.updateUserProfile(dto);

		assertTrue(result);
		assertEquals("Test User", user.getFullName()); // Unchanged
		assertEquals("encodedPassword", user.getPassword()); // Unchanged
		assertEquals("1234567890", user.getPhoneNumber()); // Unchanged
		verify(userRepository, times(1)).findByEmail(email);
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, times(1)).save(user);
	}
}
