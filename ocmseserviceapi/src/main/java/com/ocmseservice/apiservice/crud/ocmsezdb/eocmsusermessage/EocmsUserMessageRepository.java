package com.ocmseservice.apiservice.crud.ocmsezdb.eocmsusermessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EocmsUserMessageRepository extends JpaRepository<EocmsUserMessage, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<EocmsUserMessage> {
    
    /**
     * Find message by error code
     * 
     * @param errorCode The error code to search for
     * @return The error message
     */
    @Query(value = "SELECT error_message FROM eocms_user_message WHERE error_code = :errorCode", nativeQuery = true)
    String findMessageByErrorCode(@Param("errorCode") String errorCode);
}
