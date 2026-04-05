package com.voting.repository;
import com.voting.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVoterId(String voterId);
    boolean existsByEmail(String email);
    boolean existsByVoterId(String voterId);
    List<User> findByVerifiedFalse();
    List<User> findByVoterIdVerifiedFalseAndVoterIdNotNull();
}
