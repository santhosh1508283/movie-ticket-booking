package com.dmg.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "seat_hold_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hold_show_seat",
                        columnNames = {"hold_id", "show_seat_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHoldItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "hold_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hold_item_hold")
    )
    private SeatHold seatHold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "show_seat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hold_item_show_seat")
    )
    private ShowSeat showSeat;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}