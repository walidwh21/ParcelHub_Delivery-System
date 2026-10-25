package com.parcelhub.logistics.dto.request;

import com.parcelhub.logistics.enums.Gender;
import com.parcelhub.logistics.enums.VehicleType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RiderOnboardingRequest {

	// Personal Info
	@NotNull(message = "Date of birth is required")
	@Past(message = "Date of birth must be in the past")
	private LocalDate dateOfBirth;

	@NotNull(message = "Gender is required")
	private Gender gender;

	@NotBlank(message = "Address is required")
	@Size(max = 255, message = "Address cannot exceed 255 characters")
	private String address;

	@NotBlank(message = "Upazila is required")
	@Size(max = 100, message = "Upazila name cannot exceed 100 characters")
	private String upazila;

	@NotBlank(message = "District is required")
	@Size(max = 100, message = "District name cannot exceed 100 characters")
	private String district;

	@NotBlank(message = "Postal code is required")
	private String postalCode;

	// Vehicle Info
	@NotNull(message = "Vehicle type is required")
	private VehicleType vehicleType;

	@NotBlank(message = "Vehicle model is required")
	@Size(max = 100, message = "Vehicle model cannot exceed 100 characters")
	private String vehicleModel;

	@NotBlank(message = "Vehicle registration number is required")
	// @Pattern(regexp = "^[A-Z0-9-]+$", message = "Invalid vehicle registration number format")
	private String vehicleRegistrationNumber;

	@NotBlank(message = "Vehicle insurance provider is required")
	@Size(max = 100, message = "Insurance provider name cannot exceed 100 characters")
	private String vehicleInsuranceProvider;

	@NotNull(message = "Insurance expiry date is required")
	@Future(message = "Insurance expiry date must be in the future")
	private LocalDate insuranceExpiryDate;

	@NotNull(message = "License expiry date is required")
	@Future(message = "License expiry date must be in the future")
	private LocalDate licenseExpiryDate;

	@NotBlank(message = "National ID number is required")
	private String nationalIdNumber;

	@NotBlank(message = "Driver's license number is required")
	// @Pattern(regexp = "^[A-Z0-9-]+$", message = "Invalid driver's license number format")
	private String driversLicenseNumber;

	// File Uploads
	@NotNull(message = "Vehicle registration document is required")
	private MultipartFile vehicleRegistrationDocument;

	@NotNull(message = "Insurance document is required")
	private MultipartFile insuranceDocument;

	@NotNull(message = "National ID front is required")
	private MultipartFile nationalIdFront;

	@NotNull(message = "National ID back is required")
	private MultipartFile nationalIdBack;

	@NotNull(message = "Driver's license front is required")
	private MultipartFile driversLicenseFront;

	@NotNull(message = "Driver's license back is required")
	private MultipartFile driversLicenseBack;
}
