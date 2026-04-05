package com.voting.controller;

import com.voting.model.Poll;
import com.voting.model.User;
import com.voting.service.PollService;
import com.voting.service.VoteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final VoteService voteService;

    private static final DateTimeFormatter DT = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm")
        .optionalStart().appendPattern(":ss").optionalEnd()
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .toFormatter();

    private LocalDateTime parse(String s) {
        return (s == null || s.isBlank()) ? null : LocalDateTime.parse(s, DT);
    }

    @GetMapping
    public ResponseEntity<List<Poll>> activePolls() {
        return ResponseEntity.ok(pollService.getActivePolls());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPoll(@PathVariable Long id, @AuthenticationPrincipal User u) {
        Poll poll = pollService.getById(id);
        Map<String, Object> r = new HashMap<>();
        r.put("poll", poll);
        r.put("hasVoted", voteService.hasVoted(id, u.getId()));
        return ResponseEntity.ok(r);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Poll> create(@RequestBody Req req, @AuthenticationPrincipal User u) {
        return ResponseEntity.ok(pollService.create(req.getTitle(), req.getDescription(),
            parse(req.getStartTime()), parse(req.getEndTime()), req.getOptions(), u.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Poll> update(@PathVariable Long id, @RequestBody Req req,
                                       @AuthenticationPrincipal User u) {
        return ResponseEntity.ok(pollService.update(id, req.getTitle(), req.getDescription(),
            parse(req.getStartTime()), parse(req.getEndTime()), req.getOptions(), u.getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Poll> status(@PathVariable Long id, @RequestBody Map<String,String> body,
                                       @AuthenticationPrincipal User u) {
        return ResponseEntity.ok(pollService.updateStatus(id, body.get("status"), u.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        pollService.delete(id, u.getId());
        return ResponseEntity.ok(Map.of("message", "Poll deleted"));
    }

    @Data static class Req {
        private String title, description, startTime, endTime;
        private List<String> options;
    }
}
