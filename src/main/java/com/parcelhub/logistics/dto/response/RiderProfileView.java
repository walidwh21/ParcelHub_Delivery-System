package com.parcelhub.logistics.dto.response;

import com.parcelhub.logistics.enums.Gender;
import com.parcelhub.logistics.enums.VehicleType;
import com.parcelhub.logistics.enums.VerificationStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RiderProfileView {
	private UUID id;
	private String fullName;
	private String email;
	private String phoneNumber;
	private String profilePicture;
	private LocalDate dateOfBirth;
	private Gender gender;
	private String address;
	private String upazila;
	private String district;
	private String postalCode;
	private VehicleType vehicleType;
	private String vehicleModel;
	private String vehicleRegistrationNumber;
	private String vehicleInsuranceProvider;
	private LocalDate insuranceExpiryDate;
	private String vehicleRegistrationDocumentPath;
	private String insuranceDocumentPath;
	private String nationalIdNumber;
	private String nationalIdFrontPath;
	private String nationalIdBackPath;
	private String driversLicenseNumber;
	private LocalDate licenseExpiryDate;
	private String driversLicenseFrontPath;
	private String driversLicenseBackPath;
	private VerificationStatus verificationStatus;
	private Boolean isAvailable;
	private Boolean isAssigned;
	private double riderAvgRating;
	private int totalRating;
	private double riderBalance;
}
