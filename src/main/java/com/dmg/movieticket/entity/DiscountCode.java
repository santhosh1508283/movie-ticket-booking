package com.dmg.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "discount_codes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_discount_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_discount_code_active_dates",
                        columnList = "active,valid_from,valid_until"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal discountValue;

    @Column(
            name = "minimum_order_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal minimumOrderAmount;

    @Column(
            name = "maximum_discount_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal maximumDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Boolean active;
}