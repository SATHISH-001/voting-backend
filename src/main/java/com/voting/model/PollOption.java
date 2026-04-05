package com.voting.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_options")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PollOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnoreProperties({"options","createdBy","hibernateLazyInitializer"})
    private Poll poll;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
