package com.pelycan.api.service;

import com.pelycan.api.domain.RelationshipEvaluation;
import com.pelycan.api.repository.RelationshipEvaluationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RelationshipEvaluationService {

    private final RelationshipEvaluationRepository repository;

    public RelationshipEvaluationService(RelationshipEvaluationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(UUID userId) {
        return repository.findByUserIdOrderByEvaluationDateDesc(userId).stream()
                .map(this::toJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForUser(UUID userId, UUID evaluationId) {
        return toJson(findOwned(evaluationId, userId));
    }

    @Transactional
    public Map<String, Object> create(UUID userId, Map<String, Object> body) {
        RelationshipEvaluation evaluation = new RelationshipEvaluation();
        evaluation.setId(parseId(body.get("evaluation_id"), body.get("id")));
        evaluation.setUserId(userId);
        applyBody(evaluation, body);
        repository.save(evaluation);
        return toJson(evaluation);
    }

    @Transactional
    public Map<String, Object> update(UUID userId, UUID evaluationId, Map<String, Object> body) {
        RelationshipEvaluation evaluation = findOwned(evaluationId, userId);
        applyBody(evaluation, body);
        repository.save(evaluation);
        return toJson(evaluation);
    }

    @Transactional
    public void deleteForUser(UUID userId, UUID evaluationId) {
        RelationshipEvaluation evaluation = findOwned(evaluationId, userId);
        repository.delete(evaluation);
    }

    @Transactional
    public void deleteByRelationName(UUID userId, String relationName) {
        if (relationName == null || relationName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de relation requis");
        }
        repository.deleteByUserIdAndRelationNameIgnoreCase(userId, relationName.trim());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statsForUser(UUID userId) {
        List<RelationshipEvaluation> evaluations =
                repository.findByUserIdOrderByEvaluationDateDesc(userId);

        Map<String, Long> byToxicity = new LinkedHashMap<>();
        Map<String, Long> byRelation = new LinkedHashMap<>();

        for (RelationshipEvaluation evaluation : evaluations) {
            byToxicity.merge(evaluation.getToxicityLevel(), 1L, Long::sum);
            byRelation.merge(evaluation.getRelationName(), 1L, Long::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", evaluations.size());
        summary.put("by_toxicity_level", byToxicity);
        summary.put("by_relation_name", byRelation);
        return summary;
    }

    private RelationshipEvaluation findOwned(UUID evaluationId, UUID userId) {
        return repository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Évaluation non trouvée"));
    }

    private void applyBody(RelationshipEvaluation evaluation, Map<String, Object> body) {
        String relationName = string(body.get("relation_name"));
        if (relationName != null && !relationName.isBlank()) {
            evaluation.setRelationName(relationName);
        } else if (evaluation.getRelationName() == null || evaluation.getRelationName().isBlank()) {
            evaluation.setRelationName("Ma relation");
        }

        String relationType = string(body.get("relation_type"));
        if (relationType != null && !relationType.isBlank()) {
            evaluation.setRelationType(relationType);
        } else if (evaluation.getRelationType() == null || evaluation.getRelationType().isBlank()) {
            evaluation.setRelationType("romantic");
        }

        Instant evaluationDate = parseInstant(body.get("evaluation_date"));
        if (evaluationDate != null) {
            evaluation.setEvaluationDate(evaluationDate);
        } else if (evaluation.getEvaluationDate() == null) {
            evaluation.setEvaluationDate(Instant.now());
        }

        Map<String, Object> categoryScores = parseJsonMap(body.get("category_scores"));
        if (!categoryScores.isEmpty()) {
            evaluation.setCategoryScores(categoryScores);
        }

        Integer totalScore = toInteger(body.get("total_score"));
        if (totalScore != null) {
            evaluation.setTotalScore(totalScore);
        }

        Integer maxScore = toInteger(body.get("max_score"));
        if (maxScore != null) {
            evaluation.setMaxScore(maxScore);
        }

        String toxicityLevel = string(body.get("toxicity_level"));
        if (toxicityLevel != null && !toxicityLevel.isBlank()) {
            evaluation.setToxicityLevel(toxicityLevel);
        } else if (evaluation.getToxicityLevel() == null || evaluation.getToxicityLevel().isBlank()) {
            evaluation.setToxicityLevel("healthy");
        }

        Map<String, Object> answers = parseJsonMap(body.get("answers"));
        if (!answers.isEmpty()) {
            evaluation.setAnswers(answers);
        }

        if (body.containsKey("notes")) {
            evaluation.setNotes(string(body.get("notes")));
        }
    }

    private Map<String, Object> toJson(RelationshipEvaluation evaluation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("evaluation_id", evaluation.getId().toString());
        json.put("id", evaluation.getId().toString());
        json.put("relation_name", evaluation.getRelationName());
        json.put("relation_type", evaluation.getRelationType());
        json.put("evaluation_date", evaluation.getEvaluationDate().toString());
        json.put("category_scores", evaluation.getCategoryScores() != null
                ? evaluation.getCategoryScores()
                : Map.of());
        json.put("total_score", evaluation.getTotalScore());
        json.put("max_score", evaluation.getMaxScore());
        json.put("toxicity_level", evaluation.getToxicityLevel());
        json.put("answers", evaluation.getAnswers() != null
                ? evaluation.getAnswers()
                : Map.of());
        json.put("notes", evaluation.getNotes());
        json.put("created_at", evaluation.getCreatedAt().toString());
        json.put("updated_at", evaluation.getUpdatedAt().toString());
        return json;
    }

    private static UUID parseId(Object primary, Object fallback) {
        String value = string(primary);
        if (value == null || value.isBlank()) {
            value = string(fallback);
        }
        if (value == null || value.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    private static String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> result.put(String.valueOf(key), entryValue));
            return result;
        }
        return new HashMap<>();
    }
}
