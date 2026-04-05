package com.voting.repository;
import com.voting.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByPollIdAndUserId(Long pollId, Long userId);
    List<Vote> findByPollId(Long pollId);
    Optional<Vote> findByPollIdAndUserId(Long pollId, Long userId);
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId")
    long countByPollId(@Param("pollId") Long pollId);
    List<Vote> findByUserId(Long userId);
}
