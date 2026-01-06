package com.ocmsintranet.cronservice.crud.ocmsizdb.parameter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ocmsintranet.cronservice.crud.BaseRepository;

@Repository
public interface ParameterRepository extends BaseRepository<Parameter, ParameterId>{
    Optional<Parameter> findById(ParameterId id);
}