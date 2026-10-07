package com.slit.realityvote.repository;

import com.slit.realityvote.entity.ComplianceMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceMessageRepository extends JpaRepository<ComplianceMessage, Long> {

    List<ComplianceMessage> findByRecipient_IdOrderBySentAtDesc(Long recipientId);

    List<ComplianceMessage> findBySentBy_IdOrderBySentAtDesc(Long sentById);
}
