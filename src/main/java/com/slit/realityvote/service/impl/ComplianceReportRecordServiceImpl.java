package com.slit.realityvote.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slit.realityvote.dto.ComplianceReportDto;
import com.slit.realityvote.dto.VotingActivityStats;
import com.slit.realityvote.entity.ComplianceReportRecord;
import com.slit.realityvote.entity.ReportStatus;
import com.slit.realityvote.repository.ComplianceReportRecordRepository;
import com.slit.realityvote.service.ComplianceReportRecordService;
import com.slit.realityvote.service.ComplianceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplianceReportRecordServiceImpl implements ComplianceReportRecordService {

    private final ComplianceReportRecordRepository repo;
    private final ComplianceService complianceService;
    private final ObjectMapper objectMapper; // NEW

    @Override
    @Transactional
    public ComplianceReportRecord generate(Long sessionId, String raisedByEmail) {
        ComplianceReportDto dto = complianceService.generateReport(sessionId);
        String recs = String.join("\n", dto.recommendations());

        String contestantJson;
        try {
            contestantJson = objectMapper.writeValueAsString(dto.activityStats().contestantStats());
        } catch (JsonProcessingException e) {
            contestantJson = "[]";
        }

        ComplianceReportRecord record = ComplianceReportRecord.builder()
                .sessionId(sessionId)
                .sessionDescription(dto.sessionDescription())
                .generatedAt(dto.reportGeneratedAt())
                .integrityStatus(dto.integrityReport().integrityStatus())
                .totalVotes(dto.activityStats().totalVotes())
                .totalFlagged(dto.integrityReport().auditedVoteCount())
                .totalRejected(dto.integrityReport().rejectedVoteCount())
                .recommendations(recs)
                .contestantStatsJson(contestantJson)
                .autoSnapshot(false)
                .status(ReportStatus.DRAFT)
                .raisedByEmail(raisedByEmail)
                .build();

        return repo.save(record);
    }

    @Override
    @Transactional
    public ComplianceReportRecord refreshAutoSnapshot(Long sessionId) {
        VotingActivityStats activity = complianceService.getActivity(sessionId);
        String contestantJson = serializeContestantStats(activity);

        Optional<ComplianceReportRecord> existing =
                repo.findFirstBySessionIdAndAutoSnapshotTrueOrderByGeneratedAtDesc(sessionId);

        if (existing.isPresent() && existing.get().getStatus() == ReportStatus.DRAFT) {
            ComplianceReportRecord record = existing.get();
            record.setSessionDescription(activity.sessionDescription());
            record.setGeneratedAt(LocalDateTime.now());
            record.setTotalVotes(activity.totalVotes());
            record.setTotalRejected(activity.totalRejected());
            record.setContestantStatsJson(contestantJson);
            return repo.save(record);
        }

        ComplianceReportRecord record = ComplianceReportRecord.builder()
                .sessionId(sessionId)
                .sessionDescription(activity.sessionDescription())
                .generatedAt(LocalDateTime.now())
                .integrityStatus("PENDING")
                .totalVotes(activity.totalVotes())
                .totalRejected(activity.totalRejected())
                .contestantStatsJson(contestantJson)
                .autoSnapshot(true)
                .status(ReportStatus.DRAFT)
                .raisedByEmail("system")
                .build();

        return repo.save(record);
    }

    @Override
    public Optional<ComplianceReportRecord> getLatestAutoSnapshot(Long sessionId) {
        return repo.findFirstBySessionIdAndAutoSnapshotTrueOrderByGeneratedAtDesc(sessionId);
    }

    private String serializeContestantStats(VotingActivityStats activity) {
        try {
            return objectMapper.writeValueAsString(activity.contestantStats());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    @Transactional
    public ComplianceReportRecord file(Long id) {
        ComplianceReportRecord record = get(id);
        if (record.getStatus() == ReportStatus.FILED) {
            throw new IllegalStateException("Report #" + id + " is already filed.");
        }
        record.setStatus(ReportStatus.FILED);
        record.setFiledAt(LocalDateTime.now());
        return repo.save(record);
    }

    @Override public List<ComplianceReportRecord> getAll()                    { return repo.findAllByOrderByCreatedDateDesc(); }
    @Override public Optional<ComplianceReportRecord> getById(Long id)        { return repo.findById(id); }
    @Override public List<ComplianceReportRecord> getBySession(Long sessionId){ return repo.findBySessionId(sessionId); }

    private ComplianceReportRecord get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComplianceReportRecord not found: " + id));
    }
}
