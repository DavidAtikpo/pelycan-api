package com.pelycan.api.repository;

import com.pelycan.api.domain.GroupeParoleSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupeParoleSessionRepository extends JpaRepository<GroupeParoleSession, UUID> {

    List<GroupeParoleSession> findAllByOrderBySessionDateDescCreatedAtDesc();
}
