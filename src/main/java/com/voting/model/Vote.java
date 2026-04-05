package com.voting.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id","user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Vote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnoreProperties({"options","createdBy","hibernateLazyInitializer"})
    private Poll poll;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "encrypted_choice", nullable = false, columnDefinition = "TEXT")
    private String encryptedChoice;

    @Column(name = "vote_hash", nullable = false, length = 64)
    private String voteHash;

    @Column(name = "face_verified", nullable = false)
    @Builder.Default
    private Boolean faceVerified = false;

    /** Path to face snapshot captured at vote time — admin can view this */
    @Column(name = "face_snap_path", length = 500)
    private String faceSnapPath;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreationTimestamp
    @Column(name = "cast_at")
    private LocalDateTime castAt;
}
