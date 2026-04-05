package com.voting.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    /** Voter's government ID number */
    @Column(name = "voter_id", unique = true, length = 50)
    private String voterId;

    /** TRUE only after admin explicitly verifies this voter's ID */
    @Column(name = "voter_id_verified", nullable = false)
    @Builder.Default
    private Boolean voterIdVerified = false;

    /** TRUE when admin rejects voter ID and requests re-submission */
    @Column(name = "voter_id_rejected", nullable = false)
    @Builder.Default
    private Boolean voterIdRejected = false;

    /** Rejection message shown to voter on their profile page */
    @Column(name = "voter_id_reject_msg", length = 500)
    private String voterIdRejectMsg;

    /** Path to the face snapshot saved during vote submission */
    @JsonIgnore
    @Column(name = "face_image_path", length = 500)
    private String faceImagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.VOTER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @JsonIgnore @Column(name = "otp_code")
    private String otpCode;

    @JsonIgnore @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonIgnore @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role { VOTER, ADMIN }

    public boolean hasFace() {
        return faceImagePath != null && !faceImagePath.isBlank();
    }

    /** Voter can cast a vote only when both email and voter-ID are verified */
    public boolean canVote() {
        return Boolean.TRUE.equals(verified) && Boolean.TRUE.equals(voterIdVerified);
    }
}
