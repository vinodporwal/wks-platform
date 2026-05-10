package com.wks.caseengine.tcs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.DummyEntity;

@Repository
public interface FurnaceRepository extends JpaRepository<DummyEntity, UUID> {

    // Fetch furnace data by plant from TCS_Furnace
    // ORDER BY matches original GetFurnaceData SP: F1→F2→F3→F4→F5→F6→Demo→Average→others
    @Query(
        value = """
            SELECT Id, Name, Jan, Feb, Mar, Apr, May, Jun,
                   Jul, Aug, Sep, Oct, Nov, [Dec], Remarks
            FROM TCS_Furnace
            WHERE SourceAOPYear = :sourceAOPYear
              AND Site_FK_Id    = :siteId
              AND Plant_FK_Id   = :plantId
            ORDER BY
                CASE
                    WHEN Name = 'F1'          THEN 1
                    WHEN Name = 'F2'          THEN 2
                    WHEN Name = 'F3'          THEN 3
                    WHEN Name = 'F4'          THEN 4
                    WHEN Name = 'F5'          THEN 5
                    WHEN Name = 'F6'          THEN 6
                    WHEN Name = 'Demo'        THEN 7
                    WHEN Name LIKE 'Average%' THEN 8
                    ELSE 9
                END
        """,
        nativeQuery = true
    )
    List<FurnaceProjection> getFurnaceData(
        @Param("sourceAOPYear") int sourceAOPYear,
        @Param("siteId") UUID siteId,
        @Param("plantId") UUID plantId
    );

    // Fetch furnace output data by vertical from TCS_Furnace
    // ORDER BY Furnace matches original GetFurnaceData_Output SP
    @Query(
        value = """
            SELECT Id, Name, Jan, Feb, Mar, Apr, May, Jun,
                   Jul, Aug, Sep, Oct, Nov, [Dec], Remarks
            FROM TCS_Furnace
            WHERE SourceAOPYear  = :sourceAOPYear
              AND Site_FK_Id     = :siteId
              AND Vertical_FK_ID = :verticalId
            ORDER BY Name
        """,
        nativeQuery = true
    )
    List<FurnaceProjection> getFurnaceOutputData(
        @Param("sourceAOPYear") int sourceAOPYear,
        @Param("verticalId") UUID verticalId,
        @Param("siteId") UUID siteId
    );

    // Fetch GCalPerHr norms (site/plant level) from TCS_Furnace_GCalPerHr
    // Returns a single row:
    //   [0]=Id, [1]=Jan, [2]=Feb,  [3]=Mar,  [4]=Apr,  [5]=May,
    //   [6]=Jun, [7]=Jul, [8]=Aug, [9]=Sep, [10]=Oct, [11]=Nov,
    //   [12]=Dec, [13]=Name, [14]=Remarks
    @Query(
        value = """
            SELECT Id,
                   Jan, Feb, Mar, Apr, May, Jun,
                   Jul, Aug, Sep, Oct, Nov, [Dec],
                   Name, Remarks
            FROM TCS_Furnace_GCalPerHr
            WHERE SourceAOPYear = :sourceAOPYear
              AND Site_FK_Id    = :siteId
              AND Plant_FK_Id   = :plantId
        """,
        nativeQuery = true
    )
    List<Object[]> getGCalPerHrData(
        @Param("sourceAOPYear") int sourceAOPYear,
        @Param("siteId") UUID siteId,
        @Param("plantId") UUID plantId
    );

     @Query(
        value = """
            SELECT Id,
                   Jan, Feb, Mar, Apr, May, Jun,
                   Jul, Aug, Sep, Oct, Nov, [Dec],
                   Name, Remarks
            FROM TCS_Furnace_GCalPerHr
            WHERE SourceAOPYear = :sourceAOPYear
              AND Site_FK_Id    = :siteId
              AND Vertical_FK_Id   = :verticalId
        """,
        nativeQuery = true
    )
    List<Object[]> getGCalPerHrDataByVerticalIdAndSiteId(
        @Param("sourceAOPYear") int sourceAOPYear,
        @Param("siteId") UUID siteId,
        @Param("verticalId") UUID verticalId
    );
}




