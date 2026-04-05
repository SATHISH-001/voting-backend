package com.voting.service;

import com.voting.model.User;
import com.voting.repository.UserRepository;
import com.voting.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditService audit;
    private final FileStorageService storage;

    public void register(String name, String email, String password,
                         String phone, String address, String voterId,
                         MultipartFile faceImage) {
        if (userRepo.existsByEmail(email))
            throw new RuntimeException("Email already registered");
        if (voterId != null && !voterId.isBlank() && userRepo.existsByVoterId(voterId))
            throw new RuntimeException("Voter ID already registered");

        String facePath = (faceImage != null && !faceImage.isEmpty())
            ? storage.save(faceImage, "faces") : null;

        String otp = otp6();
        userRepo.save(User.builder()
            .name(name).email(email).password(encoder.encode(password))
            .phone(phone).address(address).voterId(voterId)
            .faceImagePath(facePath)
            .verified(false).voterIdVerified(false).voterIdRejected(false)
            .otpCode(otp).otpExpiry(LocalDateTime.now().plusMinutes(10))
            .build());

        emailService.sendOtp(email, otp);
        audit.log("REGISTER", null, "New voter: " + email);
    }

    public void verifyOtp(String email, String otp) {
        User u = find(email);
        if (!otp.equals(u.getOtpCode())) throw new RuntimeException("Invalid OTP");
        if (LocalDateTime.now().isAfter(u.getOtpExpiry())) throw new RuntimeException("OTP expired");
        u.setVerified(true); u.setOtpCode(null); u.setOtpExpiry(null);
        userRepo.save(u);
        audit.log("VERIFY_EMAIL", u.getId(), "Verified: " + email);
    }

    public void resendOtp(String email) {
        User u = find(email);
        if (u.getVerified()) throw new RuntimeException("Already verified");
        String otp = otp6();
        u.setOtpCode(otp); u.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepo.save(u);
        emailService.sendOtp(email, otp);
    }

    public String login(String email, String password) {
        User u = find(email);
        if (!u.getVerified()) throw new RuntimeException("Please verify your email first");
        if (!encoder.matches(password, u.getPassword())) throw new RuntimeException("Invalid credentials");
        audit.log("LOGIN", u.getId(), "Login: " + email);
        return jwtUtil.generateToken(email, u.getRole().name(), u.getId());
    }

    /** Voter updates their own voter ID from profile page */
    public User updateVoterId(Long userId, String newVoterId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (newVoterId == null || newVoterId.isBlank()) throw new RuntimeException("Voter ID cannot be empty");
        // Check uniqueness ignoring self
        userRepo.findByVoterId(newVoterId).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) throw new RuntimeException("Voter ID already in use");
        });
        u.setVoterId(newVoterId);
        u.setVoterIdVerified(false);   // reset — needs re-verification
        u.setVoterIdRejected(false);
        u.setVoterIdRejectMsg(null);
        User saved = userRepo.save(u);
        audit.log("VOTER_ID_UPDATED", userId, "New voter ID submitted: " + newVoterId);
        return saved;
    }

    /** Return voter's current profile info */
    public User getProfile(Long userId) {
        return userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private User find(String email) {
        return userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String otp6() { return String.format("%06d", new Random().nextInt(999999)); }
}
