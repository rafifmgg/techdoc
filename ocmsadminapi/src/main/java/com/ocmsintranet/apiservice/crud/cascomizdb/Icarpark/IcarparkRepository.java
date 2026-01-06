package com.ocmsintranet.apiservice.crud.cascomizdb.Icarpark;

import com.ocmsintranet.apiservice.crud.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Icarpark entities
 */
@Repository
public interface IcarparkRepository extends BaseRepository<Icarpark, Integer> {
    /**
     * Find car parks by car park ID
     * 
     * @param carParkId The car park ID to search for
     * @return List of car parks with the specified ID
     */
    List<Icarpark> findByCarParkId(String carParkId);
    @Query(value = "SELECT a.CAR_PARK_NAME FROM CARPARK a WHERE a.CAR_PARK_ID =:carparkId ", nativeQuery = true)
    Optional<String> findCarParkName(@Param("carparkId") String carparkId);
}
