package com.voting.controller;

import com.voting.model.*;
import com.voting.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserAdminService userAdmin;
    private final PollService pollService;
    private final VoteService voteService;
    private final AuditService auditService;

    // ── USERS ──────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<User>> allUsers() { return ResponseEntity.ok(userAdmin.getAll()); }

    @GetMapping("/users/unverified")
    public ResponseEntity<List<User>> unverified() { return ResponseEntity.ok(userAdmin.getUnverified()); }

    @GetMapping("/users/pending-voter-id")
    public ResponseEntity<List<User>> pendingVoterId() { return ResponseEntity.ok(userAdmin.getPendingVoterId()); }

    @PostMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> addUser(
            @RequestParam String name, @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String voterId,
            @RequestParam(required = false) MultipartFile faceImage,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userAdmin.addUser(
            name, email, password, phone, address, voterId, faceImage, admin.getId()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UpdateReq req,
                                           @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userAdmin.updateUser(
            id, req.getName(), req.getPhone(), req.getAddress(), req.getVoterId(), admin.getId()));
    }

    @PutMapping("/users/{id}/verify-account")
    public ResponseEntity<User> verifyAccount(@PathVariable Long id,
                                               @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userAdmin.verifyAccount(id, admin.getId()));
    }

    /** Verify voter ID → user can now vote */
    @PutMapping("/users/{id}/verify-voter-id")
    public ResponseEntity<User> verifyVoterId(@PathVariable Long id,
                                               @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(userAdmin.verifyVoterId(id, admin.getId()));
    }

    /** Reject voter ID with reason → user sees message on profile and must re-submit */
    @PutMapping("/users/{id}/reject-voter-id")
    public ResponseEntity<User> rejectVoterId(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal User admin) {
        String reason = body.getOrDefault("reason", "Invalid voter ID. Please re-enter.");
        return ResponseEntity.ok(userAdmin.rejectVoterId(id, reason, admin.getId()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                         @AuthenticationPrincipal User admin) {
        userAdmin.deleteUser(id, admin.getId());
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // ── EMAIL ───────────────────────────────────────────────────
    @PostMapping("/users/{id}/remind")
    public ResponseEntity<?> remind(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        userAdmin.sendReminder(id, admin.getId());
        return ResponseEntity.ok(Map.of("message", "Reminder sent"));
    }

    @PostMapping("/users/remind-all-unverified")
    public ResponseEntity<?> remindAll(@AuthenticationPrincipal User admin) {
        int n = userAdmin.remindAllUnverified(admin.getId());
        return ResponseEntity.ok(Map.of("message", n + " reminders sent"));
    }

    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailReq req,
                                        @AuthenticationPrincipal User admin) {
        userAdmin.sendCustomEmail(req.getToEmail(), req.getSubject(), req.getBody(), admin.getId());
        return ResponseEntity.ok(Map.of("message", "Email sent to " + req.getToEmail()));
    }

    // ── POLLS ───────────────────────────────────────────────────
    @GetMapping("/polls")
    public ResponseEntity<List<Poll>> allPolls() { return ResponseEntity.ok(pollService.getAllPolls()); }

    @GetMapping("/polls/{id}/vote-count")
    public ResponseEntity<?> voteCount(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "pollId", id,
            "totalVotes", voteService.totalVotes(id),
            "results", voteService.getResults(id)
        ));
    }

    /** Returns voters who voted in a poll + their face snapshot URL for admin review */
    @GetMapping("/polls/{id}/voters")
    public ResponseEntity<?> pollVoters(@PathVariable Long id) {
        return ResponseEntity.ok(voteService.getVotesByPoll(id));
    }

    // ── AUDIT ───────────────────────────────────────────────────
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> audit() { return ResponseEntity.ok(auditService.getAll()); }

    // ── DTOs ────────────────────────────────────────────────────
    @Data static class UpdateReq { private String name, phone, address, voterId; }
    @Data static class EmailReq  { private String toEmail, subject, body; }
}
