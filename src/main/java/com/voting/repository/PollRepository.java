package com.voting.repository;
import com.voting.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByStatus(Poll.Status status);
    List<Poll> findAllByOrderByCreatedAtDesc();
}
