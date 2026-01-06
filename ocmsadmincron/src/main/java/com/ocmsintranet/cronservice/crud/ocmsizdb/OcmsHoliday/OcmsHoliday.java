package com.ocmsintranet.cronservice.crud.ocmsizdb.OcmsHoliday;

import com.ocmsintranet.cronservice.crud.BaseEntity;
import com.ocmsintranet.cronservice.crud.annotations.NonEditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocms_holiday", schema = "ocmsizmgr")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcmsHoliday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false)
    @NotBlank
    @NonEditable
    @Id
    private LocalDateTime holidayDate;

    @Column(name = "holiday_description", nullable = false, length = 40)
    @NotBlank
    private String holidayDescription;
}
