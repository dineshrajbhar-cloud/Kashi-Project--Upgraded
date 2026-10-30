package com.kashi.portal.donation.repository;

import com.kashi.portal.donation.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    Optional<Donor> findByEmail(String email);

    boolean existsByEmail(String email);
}