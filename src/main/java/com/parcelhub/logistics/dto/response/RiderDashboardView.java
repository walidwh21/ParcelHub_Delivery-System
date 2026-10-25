package com.parcelhub.logistics.dto.response;

import com.parcelhub.logistics.enums.VerificationStatus;
import lombok.Data;

@Data
public class RiderDashboardView {

	private String fullName;

	private VerificationStatus verificationStatus;

	private Boolean isAvailable;

	private double riderAvgRating;

	private double riderBalance;
}
