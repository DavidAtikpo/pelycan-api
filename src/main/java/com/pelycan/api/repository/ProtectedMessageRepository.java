package com.pelycan.api.repository;

import com.pelycan.api.domain.ProtectedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProtectedMessageRepository extends JpaRepository<ProtectedMessage, UUID> {

    @Query("""
            SELECT m FROM ProtectedMessage m
            WHERE m.deletedBySender = false
              AND ((m.senderId = :userId AND m.recipientId = :otherUserId)
                OR (m.senderId = :otherUserId AND m.recipientId = :userId))
            ORDER BY m.createdAt ASC
            """)
    List<ProtectedMessage> findConversation(
            @Param("userId") UUID userId,
            @Param("otherUserId") UUID otherUserId);

    @Query("""
            SELECT m FROM ProtectedMessage m
            WHERE m.deletedBySender = false
              AND (m.senderId = :userId OR m.recipientId = :userId)
            ORDER BY m.createdAt DESC
            """)
    List<ProtectedMessage> findAllForUser(@Param("userId") UUID userId);
}
