package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Duplicate-vote guard used by VoteServiceImpl before saving
    boolean existsByVotingSession_IdAndContestant_IdAndVoter_Id(Long sessionId, Long contestantId, Long voterId);

    long countByVotingSession_IdAndContestant_Id(Long sessionId, Long contestantId);

    long countByVotingSession_Id(Long sessionId);

    /**
     * Live tally: contestant id + vote count, ordered highest first.
     * Backs both the "Live Vote Counter" requirement and (later) the
     * Reports module's ranking report.
     */
    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " +
           "FROM Vote v WHERE v.votingSession.id = :sessionId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyBySession(@Param("sessionId") Long sessionId);

    interface ContestantTally {
        Long getContestantId();
        Long getVoteCount();
    }

    // Did this viewer vote for this specific contestant in this session?
    List<Vote> findByVotingSession_IdAndVoter_Id(Long sessionId, Long voterId);

    // Full voting history for a viewer's "My Voting History" page
    List<Vote> findByVoter_IdOrderByVotedAtDesc(Long voterId);

    // ── Compliance monitoring: vote time-series ──────────────────────────────

    /**
     * Returns vote counts grouped into fixed-minute buckets for each contestant
     * within a specific voting session.
     * bucket = FLOOR(TIMESTAMPDIFF(MINUTE, sessionStart, voted_at) / intervalMin)
     * Used to build the "Votes by Contestant" line chart.
     * Native query — works on both MySQL and H2.
     */
    @Query(value = "SELECT " +
           "FLOOR(TIMESTAMPDIFF(MINUTE, :sessionStart, v.voted_at) / :intervalMin) AS bucket, " +
           "v.contestant_id AS contestantId, " +
           "COUNT(*) AS voteCount " +
           "FROM votes v " +
           "WHERE v.voting_session_id = :sessionId " +
           "AND v.voted_at >= :sessionStart " +
           "GROUP BY bucket, v.contestant_id " +
           "ORDER BY bucket, v.contestant_id",
           nativeQuery = true)
    List<VoteBucket> getVoteTrend(@Param("sessionId") Long sessionId,
                                   @Param("sessionStart") java.time.LocalDateTime sessionStart,
                                   @Param("intervalMin") int intervalMin);

    interface VoteBucket {
        Integer getBucket();
        Long getContestantId();
        Long getVoteCount();
    }

    /**
     * Distinct voter count per bucket — used to build the "Active Users" line chart.
     * Native query — works on both MySQL and H2.
     */
    @Query(value = "SELECT " +
           "FLOOR(TIMESTAMPDIFF(MINUTE, :sessionStart, v.voted_at) / :intervalMin) AS bucket, " +
           "COUNT(DISTINCT v.voter_id) AS userCount " +
           "FROM votes v " +
           "WHERE v.voting_session_id = :sessionId " +
           "AND v.voted_at >= :sessionStart " +
           "GROUP BY bucket " +
           "ORDER BY bucket",
           nativeQuery = true)
    List<UserBucket> getUserCountTrend(@Param("sessionId") Long sessionId,
                                        @Param("sessionStart") java.time.LocalDateTime sessionStart,
                                        @Param("intervalMin") int intervalMin);

    interface UserBucket {
        Integer getBucket();
        Long getUserCount();
    }

    /** Distinct voter emails for all voters in a session (used by monitoring feed). */
    @Query("SELECT DISTINCT v.voter.email FROM Vote v WHERE v.votingSession.id = :sessionId")
    java.util.Set<String> findVoterEmailsBySession(@Param("sessionId") Long sessionId);

    /** All votes cast by a specific voter across all sessions. */
    long countByVoter_Id(Long voterId);

    /** All votes cast by a specific voter within one session. */
    long countByVotingSession_IdAndVoter_Id(Long sessionId, Long voterId);


    // ---- Reports & Analytics ----

    @Query("SELECT COUNT(DISTINCT v.voter.id) FROM Vote v")
    long countDistinctVoters();

    /**
     * Total votes per contestant across ALL of that contestant's voting
     * sessions within one show - i.e. their overall standing, not just
     * one session's tally. Backs the Contestant Rankings / Winner report.
     */
    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " +
           "FROM Vote v WHERE v.contestant.show.id = :showId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyByShow(@Param("showId") Long showId);
}
