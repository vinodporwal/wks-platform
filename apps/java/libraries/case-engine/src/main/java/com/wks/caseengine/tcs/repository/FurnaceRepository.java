package com.wks.caseengine.tcs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.DummyEntity;

@Repository
public interface FurnaceRepository extends JpaRepository<DummyEntity, UUID> {

    @NativeQuery("""
            EXEC dbo.GetFurnaceData
                @FinancialYear = :financialYear,
                @Site_FK_Id = :siteId,
                @Plant_FK_Id = :plantId
        """)
    List<FurnaceProjection> getFurnaceData(
        @Param("financialYear") String financialYear,
        @Param("siteId") UUID siteId,
        @Param("plantId") UUID plantId
    );

    // tcs output 
    @NativeQuery("""
            EXEC dbo.GetFurnaceData_Output
                @FinancialYear = :financialYear,
                @Site_FK_Id = :siteId
        """)
    List<FurnaceProjection> getFurnaceOutputData(
        @Param("financialYear") String financialYear,
        @Param("siteId") UUID siteId
      
    );



@NativeQuery("""
            select Id, FinancialYearMonthId, GCalPerHr from Furnace_GCalPerHr_Mapping where FinancialYearMonthId in (:financialYearMonthIds)
        """)
    List<Object[]> getFurnaceGCalPerHrMapping(
        @Param("financialYearMonthIds") List<UUID> financialYearMonthIds
    ); 
}


