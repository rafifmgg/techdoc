package com.ocmsintranet.apiservice.crud.ocmsizdb.referenceCode;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Service;
@Service
public class ReferenceCodeServiceImpl implements ReferenceCodeService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> getReferenceCodeList() {
        // Creating the JPQL query to select distinct reference_codes from StandardCode
        String jpql = "SELECT DISTINCT sc.referenceCode FROM StandardCode sc ORDER BY sc.referenceCode";
        TypedQuery<String> query = entityManager.createQuery(jpql, String.class);
        return query.getResultList(); // Execute the query and return the list of distinct reference_codes
    }

}