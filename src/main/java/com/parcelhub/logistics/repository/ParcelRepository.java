package com.parcelhub.logistics.repository;

import com.parcelhub.logistics.entity.Parcel;
import com.parcelhub.logistics.entity.Rider;
import com.parcelhub.logistics.enums.ParcelStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {
	@Query("SELECT p FROM Parcel p WHERE p.sender.id = :senderId AND p.status = :status")
	List<Parcel> findBySenderAndStatus(UUID senderId, ParcelStatus status);

	boolean existsByTrackingId(String trackingId);

	@Query("SELECT p FROM Parcel p WHERE p.sender.id = :senderId")
	List<Parcel> getAllBySender(UUID senderId);

	List<Parcel> findByStatusAndRiderIsNull(ParcelStatus status);

	List<Parcel> findByStatusInAndRider(List<ParcelStatus> statuses, Rider rider);

	List<Parcel> findByRider(Rider rider);
}
