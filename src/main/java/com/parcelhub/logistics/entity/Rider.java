package com.parcelhub.logistics.entity;

import com.parcelhub.logistics.enums.Gender;
import com.parcelhub.logistics.enums.VehicleType;
import com.parcelhub.logistics.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "riders")
@PrimaryKeyJoinColumn(name = "id")
public class Rider extends Account {

	@Column(nullable = true)
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private Gender gender;

	@Column(nullable = true)
	private String address;

	@Column(nullable = true)
	private String upazila;

	@Column(nullable = true)
	private String district;

	@Column(nullable = true)
	private String postalCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private VehicleType vehicleType;

	@Column(nullable = true)
	private String vehicleModel;

	@Column(nullable = true)
	private String vehicleRegistrationNumber;

	@Column(nullable = true)
	private String vehicleInsuranceProvider;

	@Column(nullable = true)
	private LocalDate insuranceExpiryDate;

	@Column(nullable = true)
	private String vehicleRegistrationDocumentPath;

	@Column(nullable = true)
	private String insuranceDocumentPath;

	@Column(nullable = true, unique = true)
	private String nationalIdNumber;

	@Column(nullable = true)
	private String nationalIdFrontPath;

	@Column(nullable = true)
	private String nationalIdBackPath;

	@Column(nullable = true)
	private String driversLicenseNumber;

	@Column(nullable = true)
	private LocalDate licenseExpiryDate;

	@Column(nullable = true)
	private String driversLicenseFrontPath;

	@Column(nullable = true)
	private String driversLicenseBackPath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private VerificationStatus verificationStatus = VerificationStatus.PENDING;

	@Column(nullable = true)
	private Boolean isAvailable = false;

	@Column(nullable = true)
	private Boolean isAssigned = false;

	@Column(nullable = true)
	private double riderAvgRating = 0.0;

	@Column(nullable = true)
	private int totalRating = 0;

	@Column(nullable = true)
	private double riderBalance = 0.0;
}
