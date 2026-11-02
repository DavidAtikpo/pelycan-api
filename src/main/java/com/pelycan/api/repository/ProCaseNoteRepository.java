package com.pelycan.api.repository;

import com.pelycan.api.domain.ProCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProCaseNoteRepository extends JpaRepository<ProCaseNote, UUID> {

    List<ProCaseNote> findByCaseIdOrderByCreatedAtDesc(String caseId);
}
