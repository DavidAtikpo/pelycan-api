package com.pelycan.api.repository;

import com.pelycan.api.domain.UserMedicalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserMedicalInfoRepository extends JpaRepository<UserMedicalInfo, UUID> {}
