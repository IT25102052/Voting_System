package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.ScoreService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final JudgeRepository judgeRepository;
    private final JudgeAssignmentRepository assignmentRepository;
    private final EpisodeRepository episodeRepository;
    private final SeasonRepository seasonRepository;
    private final VotingSessionRepository votingSessionRepository;
    private final ContestantRepository contestantRepository;

    private Judge requireJudge(String judgeEmail) {
        return judgeRepository.findByEmailIgnoreCaseAndDeletedFalse(judgeEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No judge profile found for " + judgeEmail + ". Ask an administrator to create one."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Episode> getAssignedEpisodes(String judgeEmail) {
        Judge judge = requireJudge(judgeEmail);
        List<JudgeAssignment> assignments = assignmentRepository.findByJudge_IdOrderByAssignedDateDesc(judge.getId());

        Set<Episode> episodes = new LinkedHashSet<>();
        for (JudgeAssignment a : assignments) {
            if (a.getEpisode() != null) {
                episodes.add(a.getEpisode());
            } else if (a.getSeason() != null) {
                episodes.addAll(episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(a.getSeason().getId()));
            } else if (a.getShow() != null) {
                List<Season> seasons = seasonRepository.findByShowIdOrderBySeasonNumberAsc(a.getShow().getId());
                for (Season s : seasons) {
                    episodes.addAll(episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(s.getId()));
                }
            }
        }
        return new ArrayList<>(episodes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contestant> getScorableContestants(String judgeEmail, Long episodeId) {
        List<Episode> assigned = getAssignedEpisodes(judgeEmail);
        Episode episode = assigned.stream()
                .filter(e -> e.getId().equals(episodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to judge this episode."));

        List<VotingSession> sessions = votingSessionRepository.findByEpisodeIdOrderByStartTimeDesc(episodeId);
        Set<Contestant> contestants = new LinkedHashSet<>();
        sessions.forEach(s -> contestants.addAll(s.getContestants()));

        if (contestants.isEmpty()) {
            Long showId = episode.getSeason().getShow().getId();
            contestants.addAll(contestantRepository.search(null, showId, null, Pageable.unpaged()).getContent());
        }
        return new ArrayList<>(contestants);
    }

    @Override
    @Transactional
    public Score submitScore(String judgeEmail, Long contestantId, Long episodeId, Integer scoreValue, String remarks) {
        Judge judge = requireJudge(judgeEmail);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new EntityNotFoundException("Episode not found with id: " + episodeId));

        Contestant contestant = getScorableContestants(judgeEmail, episodeId).stream()
                .filter(c -> c.getId().equals(contestantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This contestant is not scorable by you for this episode."));

        var existing = scoreRepository.findByJudge_IdAndContestant_IdAndEpisode_Id(
                judge.getId(), contestantId, episodeId);

        if (existing.isPresent()) {
            if (!episode.isJudgingOpen()) {
                throw new IllegalStateException("The judging window for this episode is closed; scores can no longer be revised.");
            }
            Score score = existing.get();
            score.setScoreValue(scoreValue);
            score.setRemarks(remarks);
            return scoreRepository.save(score);
        } else {
            if (!episode.isJudgingOpen()) {
                throw new IllegalStateException("The judging window for this episode is not open yet.");
            }
            Score score = Score.builder()
                    .judge(judge)
                    .contestant(contestant)
                    .episode(episode)
                    .scoreValue(scoreValue)
                    .remarks(remarks)
                    .build();
            return scoreRepository.save(score);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Score> getScoringHistory(String judgeEmail) {
        Judge judge = requireJudge(judgeEmail);
        return scoreRepository.findByJudge_IdOrderBySubmittedDateDesc(judge.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Score getExistingScore(String judgeEmail, Long contestantId, Long episodeId) {
        Judge judge = requireJudge(judgeEmail);
        return scoreRepository.findByJudge_IdAndContestant_IdAndEpisode_Id(
                judge.getId(), contestantId, episodeId).orElse(null);
    }
}
