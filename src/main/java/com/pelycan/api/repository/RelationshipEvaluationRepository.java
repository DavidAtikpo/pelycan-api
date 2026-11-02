package com.pelycan.api.repository;

import com.pelycan.api.domain.RelationshipEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipEvaluationRepository extends JpaRepository<RelationshipEvaluation, UUID> {

    List<RelationshipEvaluation> findByUserIdOrderByEvaluationDateDesc(UUID userId);

    Optional<RelationshipEvaluation> findByIdAndUserId(UUID id, UUID userId);

    List<RelationshipEvaluation> findByUserIdAndRelationNameIgnoreCase(UUID userId, String relationName);

    void deleteByUserIdAndRelationNameIgnoreCase(UUID userId, String relationName);
}
