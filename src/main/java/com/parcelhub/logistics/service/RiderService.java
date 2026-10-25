package com.parcelhub.logistics.service;

import com.parcelhub.logistics.dto.request.RiderOnboardingRequest;
import com.parcelhub.logistics.dto.request.RiderRegistrationRequest;
import com.parcelhub.logistics.dto.response.OpenParcelView;
import com.parcelhub.logistics.dto.response.RiderApprovalView;
import com.parcelhub.logistics.dto.response.RiderDashboardView;
import com.parcelhub.logistics.dto.response.RiderProfileView;
import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.Rider;
import com.parcelhub.logistics.enums.ParcelStatus;
import com.parcelhub.logistics.enums.Role;
import com.parcelhub.logistics.enums.VerificationStatus;
import com.parcelhub.logistics.exception.AlreadyExistsException;
import com.parcelhub.logistics.exception.ParcelAlreadyAssignedException;
import com.parcelhub.logistics.exception.ParcelNotFoundException;
import com.parcelhub.logistics.exception.UserNotFoundException;
import com.parcelhub.logistics.mapper.RiderRegistrationMapper;
import com.parcelhub.logistics.repository.ParcelRepository;
import com.parcelhub.logistics.repository.RiderRepository;
import com.parcelhub.logistics.repository.UserRepository;
import com.parcelhub.logistics.util.SessionUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Rider account lifecycle: registration, onboarding, admin verification and job pick-up. */
@RequiredArgsConstructor
@Service
@Slf4j
public class RiderService {

	/** Statuses in which a parcel counts as "currently being handled" by its rider. */
	private static final Set<ParcelStatus> ACTIVE_JOB_STATUSES =
			EnumSet.of(ParcelStatus.ASSIGNED, ParcelStatus.PICKED_UP, ParcelStatus.IN_TRANSIT);

	private final RiderRepository riderRepository;
	private final ParcelRepository parcelRepository;
	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;
	private final PasswordEncoder passwordEncoder;
	private final RiderRegistrationMapper registerRiderMapper;

	// ------------------------------------------------------------ lookups

	public boolean isEmailAlreadyInUse(String email) {
		return userRepository.findByEmail(email).isPresent();
	}

	public Rider findByRiderId(UUID id) {
		return riderRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	public Rider getAuthenticatedRider() {
		return riderRepository
				.findByEmail(SessionUtil.currentUserEmail())
				.orElseThrow(() -> new UserNotFoundException("User not Authenticated"));
	}

	public RiderDashboardView riderDashboardResponse() {
		Rider rider = getAuthenticatedRider();

		RiderDashboardView view = new RiderDashboardView();
		view.setFullName(rider.getFullName());
		view.setVerificationStatus(rider.getVerificationStatus());
		view.setIsAvailable(rider.getIsAvailable());
		view.setRiderAvgRating(rider.getRiderAvgRating());
		view.setRiderBalance(rider.getRiderBalance());
		return view;
	}

	// ------------------------------------------------- registration & onboarding

	public Rider registerRider(RiderRegistrationRequest request) {
		if (isEmailAlreadyInUse(request.getEmail())) {
			throw new AlreadyExistsException("Provided email already registered");
		}

		request.setPassword(passwordEncoder.encode(request.getPassword()));

		Rider rider = registerRiderMapper.toEntity(request);
		if (rider.getRoles() == null || rider.getRoles().isEmpty()) {
			rider.setRoles(new HashSet<>(Set.of(Role.ROLE_RIDER)));
		}
		return riderRepository.save(rider);
	}

	public Rider onboardRider(UUID riderId, RiderOnboardingRequest request) {
		Rider rider = findByRiderId(riderId);

		if (riderRepository
				.findByNationalIdNumber(request.getNationalIdNumber())
				.isPresent()) {
			throw new AlreadyExistsException("Provided National ID Number already registered");
		}

		applyPersonalDetails(rider, request);
		applyVehicleDetails(rider, request);
		applyDocuments(rider, request);
		rider.setVerificationStatus(VerificationStatus.PENDING);

		return riderRepository.save(rider);
	}

	private void applyPersonalDetails(Rider rider, RiderOnboardingRequest request) {
		rider.setDateOfBirth(request.getDateOfBirth());
		rider.setGender(request.getGender());
		rider.setAddress(request.getAddress());
		rider.setUpazila(request.getUpazila());
		rider.setDistrict(request.getDistrict());
		rider.setPostalCode(request.getPostalCode());
		rider.setNationalIdNumber(request.getNationalIdNumber());
	}

	private void applyVehicleDetails(Rider rider, RiderOnboardingRequest request) {
		rider.setVehicleType(request.getVehicleType());
		rider.setVehicleModel(request.getVehicleModel());
		rider.setVehicleRegistrationNumber(request.getVehicleRegistrationNumber());
		rider.setVehicleInsuranceProvider(request.getVehicleInsuranceProvider());
		rider.setInsuranceExpiryDate(request.getInsuranceExpiryDate());
		rider.setLicenseExpiryDate(request.getLicenseExpiryDate());
		rider.setDriversLicenseNumber(request.getDriversLicenseNumber());
	}

	private void applyDocuments(Rider rider, RiderOnboardingRequest request) {
		rider.setNationalIdFrontPath(store(request.getNationalIdFront()));
		rider.setNationalIdBackPath(store(request.getNationalIdBack()));
		rider.setDriversLicenseFrontPath(store(request.getDriversLicenseFront()));
		rider.setDriversLicenseBackPath(store(request.getDriversLicenseBack()));
		rider.setVehicleRegistrationDocumentPath(store(request.getVehicleRegistrationDocument()));
		rider.setInsuranceDocumentPath(store(request.getInsuranceDocument()));
	}

	private String store(MultipartFile file) {
		return fileStorageService.storeFile(file);
	}

	// ------------------------------------------------------- admin verification

	public List<RiderApprovalView> getPendingRiders() {
		return riderRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
				.map(r -> new RiderApprovalView(r.getId(), r.getFullName(), r.getEmail(), r.getPhoneNumber()))
				.toList();
	}

	public void updateRiderVerificationStatus(UUID riderId, VerificationStatus verificationStatus) {
		Rider rider = findByRiderId(riderId);
		rider.setVerificationStatus(verificationStatus);
		riderRepository.save(rider);
	}

	public void updateRiderStatus(Boolean status) {
		Rider rider = getAuthenticatedRider();
		rider.setIsAvailable(status);
		riderRepository.save(rider);
	}

	// --------------------------------------------------------------- job board

	public List<OpenParcelView> currentParcelsForRider() {
		List<Parcel> openParcels = parcelRepository.findByStatusAndRiderIsNull(ParcelStatus.BOOKED);

		if (openParcels.isEmpty()) {
			log.warn("No available parcels found with status: BOOKED");
		} else {
			log.info("Found {} available parcels with status: BOOKED", openParcels.size());
		}

		return openParcels.stream().map(this::toOpenParcelView).toList();
	}

	private OpenParcelView toOpenParcelView(Parcel parcel) {
		OpenParcelView view = new OpenParcelView();
		view.setId(parcel.getId());
		view.setTrackingId(parcel.getTrackingId());
		view.setPickupDistrict(parcel.getPickupDistrict());
		view.setPickupUpazila(parcel.getPickupUpazila());
		view.setPickupVillage(parcel.getPickupVillage());
		view.setReceiverDistrict(parcel.getReceiverDistrict());
		view.setReceiverUpazila(parcel.getReceiverUpazila());
		view.setReceiverVillage(parcel.getReceiverVillage());
		view.setPrice(parcel.getPrice());
		return view;
	}

	@Transactional
	public void acceptParcelDelivery(UUID parcelId) {
		Rider rider = getAuthenticatedRider();
		Parcel parcel = parcelRepository
				.findById(parcelId)
				.orElseThrow(() -> new ParcelNotFoundException("Parcel not found with ID: " + parcelId));

		if (parcel.getRider() != null) {
			throw new AlreadyExistsException("Parcel already assigned to another rider");
		}
		if (rider.getIsAssigned()) {
			throw new ParcelAlreadyAssignedException("You have already assigned to a parcel");
		}

		rider.setIsAssigned(true);
		parcel.setRider(rider);
		parcel.setStatus(ParcelStatus.ASSIGNED);
		parcel.setAssignedAt(LocalDateTime.now());
		parcelRepository.save(parcel);
	}

	public List<Parcel> getAssignedParcelByRider(Rider rider) {
		List<Parcel> parcels = parcelRepository.findByStatusInAndRider(List.copyOf(ACTIVE_JOB_STATUSES), rider);
		log.info("Rider {} currently handles {} parcel(s)", rider.getEmail(), parcels.size());
		return parcels;
	}

	// --------------------------------------------------------------- profile view

	public RiderProfileView getRiderDetails(UUID riderId) {
		return mapToDto(
				riderRepository.findById(riderId).orElseThrow(() -> new UserNotFoundException("Rider not found")));
	}

	public RiderProfileView mapToDto(Rider rider) {
		RiderProfileView view = new RiderProfileView();
		fillIdentity(view, rider);
		fillVehicleAndDocuments(view, rider);
		fillStatistics(view, rider);
		return view;
	}

	private void fillIdentity(RiderProfileView view, Rider rider) {
		view.setId(rider.getId());
		view.setFullName(rider.getFullName());
		view.setEmail(rider.getEmail());
		view.setPhoneNumber(rider.getPhoneNumber());
		view.setProfilePicture(rider.getProfilePicture());
		view.setDateOfBirth(rider.getDateOfBirth());
		view.setGender(rider.getGender());
		view.setAddress(rider.getAddress());
		view.setUpazila(rider.getUpazila());
		view.setDistrict(rider.getDistrict());
		view.setPostalCode(rider.getPostalCode());
	}

	private void fillVehicleAndDocuments(RiderProfileView view, Rider rider) {
		view.setVehicleType(rider.getVehicleType());
		view.setVehicleModel(rider.getVehicleModel());
		view.setVehicleRegistrationNumber(rider.getVehicleRegistrationNumber());
		view.setVehicleInsuranceProvider(rider.getVehicleInsuranceProvider());
		view.setInsuranceExpiryDate(rider.getInsuranceExpiryDate());
		view.setVehicleRegistrationDocumentPath(rider.getVehicleRegistrationDocumentPath());
		view.setInsuranceDocumentPath(rider.getInsuranceDocumentPath());
		view.setNationalIdNumber(rider.getNationalIdNumber());
		view.setNationalIdFrontPath(rider.getNationalIdFrontPath());
		view.setNationalIdBackPath(rider.getNationalIdBackPath());
		view.setDriversLicenseNumber(rider.getDriversLicenseNumber());
		view.setLicenseExpiryDate(rider.getLicenseExpiryDate());
		view.setDriversLicenseFrontPath(rider.getDriversLicenseFrontPath());
		view.setDriversLicenseBackPath(rider.getDriversLicenseBackPath());
	}

	private void fillStatistics(RiderProfileView view, Rider rider) {
		view.setVerificationStatus(rider.getVerificationStatus());
		view.setIsAvailable(rider.getIsAvailable());
		view.setIsAssigned(rider.getIsAssigned());
		view.setRiderAvgRating(rider.getRiderAvgRating());
		view.setTotalRating(rider.getTotalRating());
		view.setRiderBalance(rider.getRiderBalance());
	}
}
