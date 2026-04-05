package com.voting.controller;

import com.voting.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {
    private final VoteService voteService;

    @GetMapping("/{pollId}")
    public ResponseEntity<?> results(@PathVariable Long pollId) {
        return ResponseEntity.ok(Map.of(
            "results",    voteService.getResults(pollId),
            "totalVotes", voteService.totalVotes(pollId)
        ));
    }
}
