package com.voting.service;

import com.voting.model.AuditLog;
import com.voting.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void log(String action, Long userId, String details) {
        repo.save(AuditLog.builder().action(action).userId(userId).details(details).build());
    }

    public List<AuditLog> getAll() { return repo.findAllByOrderByLoggedAtDesc(); }
}
