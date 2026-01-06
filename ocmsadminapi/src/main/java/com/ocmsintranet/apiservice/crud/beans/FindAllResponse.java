package com.ocmsintranet.apiservice.crud.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response object for paginated query results
 * @param <T> Type of the items in the result list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindAllResponse<T> {
    private long total;
    private int limit;
    private int skip;
    private List<T> data;
}