package com.parcelhub.logistics.dto.response;

import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class RiderApprovalView {

	private UUID id;

	private String fullName;

	private String email;

	private String phoneNumber;

	public RiderApprovalView(UUID id, String fullName, String email, String phoneNumber) {
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.phoneNumber = phoneNumber;
	}
}
