package com.wks.caseengine.cpp.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.STGExtractionLookup;

@Repository
public interface STGExtractionLookupRepository extends JpaRepository<STGExtractionLookup, UUID> {

    // Find all records ordered by LoadMW
    List<STGExtractionLookup> findAllByOrderByLoadMWAsc();

    // Find exact match by LoadMW
    Optional<STGExtractionLookup> findByLoadMW(BigDecimal loadMW);

    // Find the closest lower LoadMW for interpolation
    @NativeQuery("SELECT TOP 1 * FROM STGExtractionLookup WITH(NOLOCK) WHERE LoadMW <= :loadMW ORDER BY LoadMW DESC")
    Optional<STGExtractionLookup> findClosestLowerLoad(@Param("loadMW") BigDecimal loadMW);

    // Find the closest higher LoadMW for interpolation
    @NativeQuery("SELECT TOP 1 * FROM STGExtractionLookup WITH(NOLOCK) WHERE LoadMW >= :loadMW ORDER BY LoadMW ASC")
    Optional<STGExtractionLookup> findClosestHigherLoad(@Param("loadMW") BigDecimal loadMW);

}


