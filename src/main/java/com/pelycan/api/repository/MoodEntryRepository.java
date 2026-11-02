package com.pelycan.api.repository;

import com.pelycan.api.domain.MoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MoodEntryRepository extends JpaRepository<MoodEntry, UUID> {
}
