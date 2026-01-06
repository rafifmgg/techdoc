package com.ocmsintranet.cronservice.crud.ocmsizdb.BatchJobs;

import com.ocmsintranet.cronservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OcmsBatchJobRepository extends BaseRepository<OcmsBatchJob, Integer> {
    // No custom methods - using only methods from BaseRepository
    @Query(value = """
    select top 1 * 
    from ocms_batch_job a
    where a.name = :name
      and cast(a.cre_date as date) = cast(getdate() as date)
    order by a.batch_job_id desc
""", nativeQuery = true)
    OcmsBatchJob findLatestByNameToday(@Param("name") String name);
}
