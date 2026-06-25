package com.astrochakra.accounting.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "removals")
@Getter
@Setter
public class Removal {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "date")
    private String date;

    @Column(name = "time")
    private String time;

    @Column(name = "type")
    private String type;

    @Column(name = "label")
    private String label;

    @Column(name = "detail")
    private String detail;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private Instant createdAt;
}
