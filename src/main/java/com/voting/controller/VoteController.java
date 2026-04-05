package com.voting.controller;

import com.voting.model.User;
import com.voting.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /** Cast vote — multipart to accept face snapshot alongside vote data */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> castVote(
            @RequestParam Long pollId,
            @RequestParam Long optionId,
            @RequestParam boolean faceVerified,
            @RequestParam(required = false) MultipartFile faceSnap,
            @AuthenticationPrincipal User user) {

        voteService.castVote(pollId, optionId, user.getId(), faceVerified, faceSnap);
        return ResponseEntity.ok(Map.of("message",
            "Vote cast successfully! A confirmation email has been sent to you."));
    }

    @GetMapping("/check/{pollId}")
    public ResponseEntity<?> check(@PathVariable Long pollId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("hasVoted", voteService.hasVoted(pollId, user.getId())));
    }
}
