package com.parcelhub.logistics.service;

import com.parcelhub.logistics.dto.request.ProfileUpdateRequest;
import com.parcelhub.logistics.dto.request.AccountRegistrationRequest;
import com.parcelhub.logistics.entity.Account;
import com.parcelhub.logistics.enums.Role;
import com.parcelhub.logistics.exception.AlreadyExistsException;
import com.parcelhub.logistics.exception.UserNotFoundException;
import com.parcelhub.logistics.repository.UserRepository;
import com.parcelhub.logistics.util.SessionUtil;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public boolean isEmailAlreadyInUse(String email) {
		return userRepository.findByEmail(email).isPresent();
	}

	public String registerUser(AccountRegistrationRequest request) {
		if (isEmailAlreadyInUse(request.getEmail())) {
			throw new AlreadyExistsException("Provided email already registered");
		}

		Account account = new Account();
		account.setFullName(request.getFullName());
		account.setEmail(request.getEmail());
		account.setPassword(passwordEncoder.encode(request.getPassword()));
		account.setRoles(new HashSet<>(Set.of(Role.ROLE_USER)));

		userRepository.save(account);
		return "User registered successfully";
	}

	public Account getAuthenticatedUser() {
		return userRepository
				.findByEmail(SessionUtil.currentUserEmail())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	public ProfileUpdateRequest userProfileUpdateGet() {
		Account account = getAuthenticatedUser();
		ProfileUpdateRequest form = new ProfileUpdateRequest();
		form.setFullName(account.getFullName());
		form.setPassword(account.getPassword());
		form.setPhoneNumber(account.getPhoneNumber());
		form.setProfileImageUrl(account.getProfilePicture());
		return form;
	}

	public boolean updateUserProfile(ProfileUpdateRequest form) {
		Account account = getAuthenticatedUser();

		if (form.getFullName() != null) {
			account.setFullName(form.getFullName());
		}
		if (form.getPhoneNumber() != null) {
			account.setPhoneNumber(form.getPhoneNumber());
		}
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			account.setPassword(passwordEncoder.encode(form.getPassword()));
		}

		userRepository.save(account);
		return true;
	}
}
