package com.parcelhub.logistics.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRegistrationRequest {

	@NotEmpty(message = "Name can not be empty")
	private String fullName;

	@NotEmpty(message = "Email can not be empty")
	@Email(message = "Invalid Email format")
	private String email;

	@NotEmpty(message = "Password can not be empty")
	@Size(min = 6, message = "Password must be at least 6 characters long")
	@Pattern(
			regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
			message = "Password must contain at least one digit, lowercase, uppercase and special character")
	private String password;
}
