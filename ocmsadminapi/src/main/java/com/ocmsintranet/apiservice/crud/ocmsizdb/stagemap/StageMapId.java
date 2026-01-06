package com.ocmsintranet.apiservice.crud.ocmsizdb.stagemap;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key for StageMap
 * Based on OCMS Data Dictionary
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StageMapId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String lastProcessingStage;
    private String nextProcessingStage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StageMapId that = (StageMapId) o;
        return Objects.equals(lastProcessingStage, that.lastProcessingStage) &&
               Objects.equals(nextProcessingStage, that.nextProcessingStage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastProcessingStage, nextProcessingStage);
    }
}
