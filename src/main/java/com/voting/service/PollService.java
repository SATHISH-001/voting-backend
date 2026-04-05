package com.voting.service;

import com.voting.model.Poll;
import com.voting.model.PollOption;
import com.voting.model.User;
import com.voting.repository.PollRepository;
import com.voting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepo;
    private final UserRepository userRepo;
    private final AuditService audit;

    public List<Poll> getActivePolls() { return pollRepo.findByStatus(Poll.Status.ACTIVE); }
    public List<Poll> getAllPolls()     { return pollRepo.findAllByOrderByCreatedAtDesc(); }

    public Poll getById(Long id) {
        return pollRepo.findById(id).orElseThrow(() -> new RuntimeException("Poll not found: " + id));
    }

    @Transactional
    public Poll create(String title, String description, LocalDateTime start, LocalDateTime end,
                       List<String> labels, Long adminId) {
        User admin = userRepo.findById(adminId).orElseThrow();
        Poll poll = Poll.builder().title(title).description(description)
            .startTime(start).endTime(end).status(Poll.Status.DRAFT).createdBy(admin).build();
        List<PollOption> opts = IntStream.range(0, labels.size())
            .mapToObj(i -> PollOption.builder().poll(poll).label(labels.get(i)).displayOrder(i).build())
            .collect(Collectors.toList());
        poll.setOptions(opts);
        Poll saved = pollRepo.save(poll);
        audit.log("POLL_CREATED", adminId, "Poll: " + title);
        return saved;
    }

    @Transactional
    public Poll updateStatus(Long id, String status, Long adminId) {
        Poll p = getById(id);
        p.setStatus(Poll.Status.valueOf(status.toUpperCase()));
        Poll saved = pollRepo.save(p);
        audit.log("POLL_STATUS", adminId, "Poll " + id + " -> " + status);
        return saved;
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        pollRepo.delete(getById(id));
        audit.log("POLL_DELETED", adminId, "Poll deleted: " + id);
    }

    @Transactional
    public Poll update(Long id, String title, String desc, LocalDateTime start, LocalDateTime end,
                       List<String> labels, Long adminId) {
        Poll p = getById(id);
        if (title  != null) p.setTitle(title);
        if (desc   != null) p.setDescription(desc);
        if (start  != null) p.setStartTime(start);
        if (end    != null) p.setEndTime(end);
        if (labels != null && !labels.isEmpty()) {
            p.getOptions().clear();
            List<PollOption> opts = IntStream.range(0, labels.size())
                .mapToObj(i -> PollOption.builder().poll(p).label(labels.get(i)).displayOrder(i).build())
                .collect(Collectors.toList());
            p.getOptions().addAll(opts);
        }
        audit.log("POLL_UPDATED", adminId, "Poll updated: " + id);
        return pollRepo.save(p);
    }
}
