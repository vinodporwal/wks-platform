package com.wks.caseengine.cpp.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
//import org.springframework.security.access.method.P;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.AssetPriorityProjection;
import com.wks.caseengine.entity.DummyEntity;
import com.wks.caseengine.repository.ExistingAssetAvailabilityProjection;

// public class AssetPriorityRepository {
    
// }

@Repository
public interface AssetPriorityRepository extends JpaRepository<DummyEntity, UUID> {

  

    @NativeQuery("""
            EXEC dbo.CPP_NMD_GetAssetPriority
                 @CppId = :cppId,
                 @FinancialYear = :financialYear
        """)
    List<AssetPriorityProjection> 
        getAssetAvailabilityPriorityByCPP(
            @Param("cppId") UUID cppId,
            @Param("financialYear") String financialYear
        );


        // code for post

    @NativeQuery("""
        SELECT COUNT(1)
        FROM AssetAvailability WITH(NOLOCK)
        WHERE AssetId = :assetId
          AND FinancialYearMonthId = :fymId
        """)  
    public long exists(@Param("assetId") UUID assetId, @Param("fymId") UUID fymId);

@Modifying
@Transactional
    @NativeQuery("""
        UPDATE AssetAvailability
        SET Priority = :priority
        WHERE AssetId = :assetId
          AND FinancialYearMonthId = :fymId
        """)
    public void updatePriority(@Param("assetId") UUID assetId, @Param("fymId") UUID fymId, @Param("priority") Integer priority);

    @Modifying
@Transactional
    @NativeQuery("""
        INSERT INTO AssetAvailability
        (
            Id,
            AssetId,
            FinancialYearMonthId,
            IsAssetAvailable,
            Priority
        )
        VALUES
        (
            NEWID(),
            :assetId,
            :fymId,
            1,
            :priority
        )
        """)
    public void insertPriority(@Param("assetId") UUID assetId, @Param("fymId") UUID fymId, @Param("priority") Integer priority);

    @NativeQuery("""
        SELECT AssetId as assetId, FinancialYearMonthId as fymId, Priority as priority
        FROM AssetAvailability WITH(NOLOCK)
        WHERE AssetId IN (:assetIds)
          AND FinancialYearMonthId IN (:fymIds)
        """)
    List<ExistingAssetAvailabilityProjection> findExistingByAssetIdsAndFymIds(
        @Param("assetIds") List<UUID> assetIds,
        @Param("fymIds") List<UUID> fymIds
    );

    
    @NativeQuery("""
            SELECT AssetId, FinancialYearMonthId
            FROM AssetAvailability WITH(NOLOCK)
            WHERE AssetId IN (:assetIds)
              AND FinancialYearMonthId IN (:financialYearMonthIds)
            """)
    List<Object[]> getAssetCapacitiesByAssetsAndFYMonths(
            @Param("assetIds") Collection<UUID> assetIds,
            @Param("financialYearMonthIds") Collection<UUID> financialYearMonthIds
    );

@NativeQuery("""
     SELECT 
        a.Id,
        a.AssetId,
        f.[Month]
    FROM AssetAvailability a WITH(NOLOCK)
    LEFT JOIN FinancialYearMonth f WITH(NOLOCK)
        ON f.Id = a.FinancialYearMonthId
    WHERE a.AssetId IN (
        SELECT pga.AssetId
        FROM powergenerationassets pga WITH(NOLOCK)
        WHERE pga.AssetName = :assetName)
    """)
    List<Object[]> getAssetAvailabilityByAssetName(@Param("assetName") String assetName);

}



