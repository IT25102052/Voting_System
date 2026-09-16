package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByVotingSession_IdAndContestant_IdAndVoter_Id(Long sessionId, Long contestantId, Long voterId);
    long countByVotingSession_IdAndContestant_Id(Long sessionId, Long contestantId);
    long countByVotingSession_Id(Long sessionId);

    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " +"FROM Vote v WHERE v.votingSession.id = :sessionId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyBySession(@Param("sessionId") Long sessionId);

    interface ContestantTally {
        Long getContestantId();
        Long getVoteCount();
    }

    List<Vote> findByVotingSession_IdAndVoter_Id(Long sessionId, Long voterId);
    List<Vote> findByVoter_IdOrderByVotedAtDesc(Long voterId);

    @Query("SELECT COUNT(DISTINCT v.voter.id) FROM Vote v")
    long countDistinctVoters();

    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " + "FROM Vote v WHERE v.contestant.show.id = :showId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyByShow(@Param("showId") Long showId);
}
