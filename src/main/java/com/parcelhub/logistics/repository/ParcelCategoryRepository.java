package com.parcelhub.logistics.repository;

import com.parcelhub.logistics.entity.ParcelCategory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelCategoryRepository extends JpaRepository<ParcelCategory, UUID> {
	// You can add custom queries here if needed
}
