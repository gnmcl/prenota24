package com.prenota24.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_exception_slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityExceptionSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exception_id", nullable = false)
    private AvailabilityException availabilityException;

    @Column(name = "start_time", nullable = false)
    private LocalTime  startTime;

    @Column(name = "end_time",  nullable = false)
    private LocalTime  endTime;
}
