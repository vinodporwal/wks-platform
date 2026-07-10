package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.wks.caseengine.cpp.entity.CppStgHeatRate;
import java.util.UUID;


@Repository
public interface CppStgHeatRateRepository extends JpaRepository<CppStgHeatRate, UUID> {

    
}
