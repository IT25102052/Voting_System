package com.slit.realityvote.repository;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:flaggedOnly = false OR a.flagged = true) " +
           "AND (:keyword IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdDate DESC")
    Page<AuditLog> search(@Param("eventType") AuditEventType eventType,
                           @Param("flaggedOnly") boolean flaggedOnly,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    // Used by the fraud-detection rule: how many matching events has this
    // actor triggered since `since`? (e.g. repeated VOTE_REJECTED or
    // LOGIN_FAILURE in the last few minutes)
    long countByActorEmailAndEventTypeAndCreatedDateAfter(String actorEmail, AuditEventType eventType, LocalDateTime since);

    long countByFlaggedTrue();
}
