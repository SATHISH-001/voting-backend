package com.voting.service;

import com.voting.model.*;
import com.voting.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepo;
    private final PollRepository pollRepo;
    private final UserRepository userRepo;
    private final CryptoService crypto;
    private final AuditService audit;
    private final EmailService email;
    private final FileStorageService storage;

    @Transactional
    public void castVote(Long pollId, Long optionId, Long userId,
                         boolean faceVerified, MultipartFile faceSnap) {

        // 1. Duplicate vote guard
        if (voteRepo.existsByPollIdAndUserId(pollId, userId))
            throw new RuntimeException("You have already voted in this poll.");

        // 2. Face must be verified at client
        if (!faceVerified)
            throw new RuntimeException("Face verification is required.");

        // 3. Load voter — check voterIdVerified
        User voter = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Voter not found"));
        if (!voter.canVote())
            throw new RuntimeException("Your Voter ID has not been verified by the administrator yet. You cannot vote until then.");

        // 4. Validate poll is active
        Poll poll = pollRepo.findById(pollId)
            .orElseThrow(() -> new RuntimeException("Poll not found"));
        if (poll.getStatus() != Poll.Status.ACTIVE)
            throw new RuntimeException("This poll is not currently active.");

        // 5. Validate option
        PollOption chosen = poll.getOptions().stream()
            .filter(o -> o.getId().equals(optionId)).findFirst()
            .orElseThrow(() -> new RuntimeException("Invalid option."));

        // 6. Save face snapshot for admin viewing
        String snapPath = null;
        if (faceSnap != null && !faceSnap.isEmpty()) {
            snapPath = storage.save(faceSnap, "snapshots");
        }

        // 7. Encrypt & hash
        String enc  = crypto.encrypt(String.valueOf(optionId));
        String hash = crypto.sha256(userId + "|" + pollId + "|" + optionId);

        // 8. Persist vote
        voteRepo.save(Vote.builder()
            .poll(poll).userId(userId)
            .encryptedChoice(enc).voteHash(hash)
            .faceVerified(true).faceSnapPath(snapPath)
            .build());

        audit.log("VOTE_CAST", userId, "Poll:" + pollId + " Option:" + optionId);

        // 9. Auto-send confirmation email
        try {
            email.sendVoteConfirmation(voter.getEmail(), voter.getName(),
                poll.getTitle(), chosen.getLabel());
        } catch (Exception ex) {
            audit.log("EMAIL_FAIL", userId, "Confirmation email failed: " + ex.getMessage());
        }
    }

    public boolean hasVoted(Long pollId, Long userId) {
        return voteRepo.existsByPollIdAndUserId(pollId, userId);
    }

    public long totalVotes(Long pollId) { return voteRepo.countByPollId(pollId); }

    public List<ResultItem> getResults(Long pollId) {
        Poll poll = pollRepo.findById(pollId).orElseThrow();
        Map<Long, String> labels = poll.getOptions().stream()
            .collect(Collectors.toMap(PollOption::getId, PollOption::getLabel));
        Map<Long, Long> counts = new LinkedHashMap<>();
        poll.getOptions().forEach(o -> counts.put(o.getId(), 0L));
        voteRepo.findByPollId(pollId).forEach(v -> {
            try {
                Long oid = Long.parseLong(crypto.decrypt(v.getEncryptedChoice()));
                counts.merge(oid, 1L, Long::sum);
            } catch (Exception ignored) {}
        });
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts.entrySet().stream().map(e -> {
            ResultItem r = new ResultItem();
            r.setOptionId(e.getKey());
            r.setOptionLabel(labels.getOrDefault(e.getKey(), "Unknown"));
            r.setCount(e.getValue());
            r.setPercentage(total > 0 ? Math.round(e.getValue() * 1000.0 / total) / 10.0 : 0.0);
            return r;
        }).collect(Collectors.toList());
    }

    /** Returns votes cast by a user with their face-snap URL for admin review */
    public List<VoteDetail> getVotesByPoll(Long pollId) {
        return voteRepo.findByPollId(pollId).stream().map(v -> {
            User u = userRepo.findById(v.getUserId()).orElse(null);
            VoteDetail d = new VoteDetail();
            d.setVoteId(v.getId());
            d.setUserId(v.getUserId());
            d.setVoterName(u != null ? u.getName()   : "Unknown");
            d.setVoterEmail(u != null ? u.getEmail() : "Unknown");
            d.setVoterId(u != null ? u.getVoterId()  : null);
            d.setCastAt(v.getCastAt() != null ? v.getCastAt().toString() : null);
            d.setFaceVerified(v.getFaceVerified());
            d.setFaceSnapUrl(v.getFaceSnapPath() != null
                ? "/" + v.getFaceSnapPath().replace("\\", "/") : null);
            return d;
        }).collect(Collectors.toList());
    }

    @Data public static class ResultItem {
        private Long optionId; private String optionLabel;
        private Long count; private Double percentage;
    }

    @Data public static class VoteDetail {
        private Long voteId, userId;
        private String voterName, voterEmail, voterId;
        private String castAt, faceSnapUrl;
        private Boolean faceVerified;
    }
}
