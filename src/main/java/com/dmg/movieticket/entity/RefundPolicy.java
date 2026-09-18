package com.dmg.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "refund_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_policy_hours",
                        columnNames = "hours_before_show"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "hours_before_show",
            nullable = false
    )
    private Integer hoursBeforeShow;

    @Column(
            name = "refund_percentage",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal refundPercentage;

    @Column(nullable = false)
    private Boolean active;
}