package com.voting.service;

import com.voting.model.User;
import com.voting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final AuditService audit;
    private final FileStorageService storage;

    public List<User> getAll()         { return userRepo.findAll(); }
    public List<User> getUnverified()  { return userRepo.findByVerifiedFalse(); }
    public List<User> getPendingVoterId() { return userRepo.findByVoterIdVerifiedFalseAndVoterIdNotNull(); }

    /** Admin adds a voter directly (pre-verified) */
    @Transactional
    public User addUser(String name, String email2, String password,
                        String phone, String address, String voterId,
                        MultipartFile faceImage, Long adminId) {
        if (userRepo.existsByEmail(email2))
            throw new RuntimeException("Email already registered: " + email2);
        if (voterId != null && !voterId.isBlank() && userRepo.existsByVoterId(voterId))
            throw new RuntimeException("Voter ID already exists: " + voterId);

        String facePath = (faceImage != null && !faceImage.isEmpty())
            ? storage.save(faceImage, "faces") : null;

        User u = User.builder()
            .name(name).email(email2).password(encoder.encode(password))
            .phone(phone).address(address).voterId(voterId)
            .faceImagePath(facePath)
            .verified(true).voterIdVerified(voterId != null && !voterId.isBlank())
            .voterIdRejected(false).role(User.Role.VOTER)
            .build();
        User saved = userRepo.save(u);
        audit.log("ADMIN_ADD_USER", adminId, "Added voter: " + email2);
        return saved;
    }

    /** Admin edits voter profile details */
    @Transactional
    public User updateUser(Long id, String name, String phone,
                           String address, String voterId, Long adminId) {
        User u = byId(id);
        if (name    != null && !name.isBlank())    u.setName(name);
        if (phone   != null)                        u.setPhone(phone);
        if (address != null)                        u.setAddress(address);
        if (voterId != null && !voterId.isBlank()) {
            userRepo.findByVoterId(voterId).ifPresent(ex -> {
                if (!ex.getId().equals(id))
                    throw new RuntimeException("Voter ID already in use");
            });
            u.setVoterId(voterId);
            u.setVoterIdVerified(false); // requires re-verification
            u.setVoterIdRejected(false);
            u.setVoterIdRejectMsg(null);
        }
        User saved = userRepo.save(u);
        audit.log("ADMIN_UPDATE_USER", adminId, "Updated user: " + id);
        return saved;
    }

    /** Admin verifies voter's email account */
    @Transactional
    public User verifyAccount(Long id, Long adminId) {
        User u = byId(id);
        u.setVerified(true); u.setOtpCode(null); u.setOtpExpiry(null);
        User saved = userRepo.save(u);
        audit.log("ADMIN_VERIFY_ACCOUNT", adminId, "Account verified: " + id);
        return saved;
    }

    /** Admin verifies the voter's ID — voter can now cast votes */
    @Transactional
    public User verifyVoterId(Long id, Long adminId) {
        User u = byId(id);
        if (u.getVoterId() == null || u.getVoterId().isBlank())
            throw new RuntimeException("Voter has no Voter ID to verify");
        u.setVoterIdVerified(true);
        u.setVoterIdRejected(false);
        u.setVoterIdRejectMsg(null);
        User saved = userRepo.save(u);
        audit.log("ADMIN_VERIFY_VOTER_ID", adminId, "Voter ID verified for user: " + id);
        // Notify voter
        try { email.sendVoterIdVerified(u.getEmail(), u.getName()); }
        catch (Exception ignored) {}
        return saved;
    }

    /** Admin rejects voter ID and asks user to re-enter */
    @Transactional
    public User rejectVoterId(Long id, String reason, Long adminId) {
        User u = byId(id);
        u.setVoterIdVerified(false);
        u.setVoterIdRejected(true);
        u.setVoterIdRejectMsg(reason);
        User saved = userRepo.save(u);
        audit.log("ADMIN_REJECT_VOTER_ID", adminId,
            "Voter ID rejected for user " + id + ": " + reason);
        // Notify voter by email
        try { email.sendVoterIdRejected(u.getEmail(), u.getName(), reason); }
        catch (Exception ignored) {}
        return saved;
    }

    @Transactional
    public void deleteUser(Long id, Long adminId) {
        User u = byId(id);
        if (u.getRole() == User.Role.ADMIN)
            throw new RuntimeException("Cannot delete admin accounts");
        userRepo.delete(u);
        audit.log("ADMIN_DELETE_USER", adminId, "Deleted user: " + id);
    }

    public void sendReminder(Long id, Long adminId) {
        User u = byId(id);
        email.sendVerificationReminder(u.getEmail(), u.getName());
        audit.log("ADMIN_REMIND", adminId, "Reminder sent to: " + u.getEmail());
    }

    public int remindAllUnverified(Long adminId) {
        List<User> list = userRepo.findByVerifiedFalse();
        list.forEach(u -> {
            try { email.sendVerificationReminder(u.getEmail(), u.getName()); }
            catch (Exception ignored) {}
        });
        audit.log("ADMIN_BULK_REMIND", adminId, "Reminded " + list.size() + " users");
        return list.size();
    }

    public void sendCustomEmail(String to, String subject, String body, Long adminId) {
        email.sendCustomEmail(to, subject, body);
        audit.log("ADMIN_CUSTOM_EMAIL", adminId, "Email sent to: " + to);
    }

    private User byId(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
