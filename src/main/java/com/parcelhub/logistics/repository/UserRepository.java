package com.parcelhub.logistics.repository;

import com.parcelhub.logistics.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Account, UUID> {
	Optional<Account> findByEmail(String email);
}
