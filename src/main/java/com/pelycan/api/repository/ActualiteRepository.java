package com.pelycan.api.repository;

import com.pelycan.api.domain.Actualite;
import com.pelycan.api.domain.ActualiteCategory;
import com.pelycan.api.domain.ActualiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ActualiteRepository extends JpaRepository<Actualite, UUID> {

    @Query("""
            SELECT a FROM Actualite a
            LEFT JOIN FETCH a.author
            ORDER BY a.updatedAt DESC
            """)
    List<Actualite> findAllForAdmin();

    List<Actualite> findByStatusOrderByPublishedAtDescCreatedAtDesc(ActualiteStatus status);

    List<Actualite> findByStatusAndCategoryOrderByPublishedAtDescCreatedAtDesc(
            ActualiteStatus status,
            ActualiteCategory category);
}
