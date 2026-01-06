package com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the CARPARK table.
 * This table stores information about car parks including their details,
 * location information, and management status.
 */
@Entity
@Table(name = "CARPARK")
@Setter
@Getter
@NoArgsConstructor
public class Icarpark {

    @Id
    @Column(name = "H_CAR_PARK_ID")
    @JsonProperty("hCarParkId")
    private Integer hCarParkId;

    @Column(name = "CAR_PARK_ID")
    private String carParkId;

    @Column(name = "CAR_PARK_NAME")
    private String carParkName;

    @Column(name = "STATUS")
    private String status;
}
