package com.parcelhub.logistics.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
	@NotEmpty(message = "Name must not be empty.")
	private String fullName;

	private String password;

	private String phoneNumber;

	private String profileImageUrl;
}
