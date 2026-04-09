package com.wks.caseengine.tcs.repository.tcsworkflow;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.DummyEntity;
import com.wks.caseengine.tcs.dto.camundadto.PlantSubmissionAuditTrailProjection;

@Repository
@Transactional
public interface TCSAuditTrailRepository extends JpaRepository<DummyEntity, Long> {
    
    @Modifying
    @Transactional
    @NativeQuery("""
            INSERT INTO TCS_Submission_History
            (Plant_Id, PlantName, PlantStatus, Site_Id, Vertical_Id, SubmittedBy, UserName, SubmissionDate,
             SubmissionRemark, VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type, BusinessKey)
            VALUES
            (:plantId, :plantName, :plantStatus, :siteId, :verticalId, :submittedBy, :userName, :submissionDateTime,
             :submissionRemark, :verifiedDateTime, :verifiedBy, :verifiedRemark,
             :status, :type, :businessKey)
            """)
    void savePlantSubmissionAuditTrail(
            @Param("plantId") UUID plantId,
            @Param("plantName") String plantName,
            @Param("plantStatus") String plantStatus,
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("submittedBy") String submittedBy,
            @Param("userName") String userName,
            @Param("submissionDateTime") Date submissionDateTime,
            @Param("submissionRemark") String submissionRemark,
            @Param("verifiedDateTime") Date verifiedDateTime,
            @Param("verifiedBy") String verifiedBy,
            @Param("verifiedRemark") String verifiedRemark,
            @Param("status") String status,
            @Param("type") String type,
            @Param("businessKey") String businessKey
           
    );
    
    @NativeQuery("SELECT Plant_Id, PlantName, PlantStatus, Site_Id, Vertical_Id, SubmittedBy, UserName, SubmissionDate, SubmissionRemark, VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type FROM TCS_Submission_History WHERE BusinessKey = :businessKey order by submissiondate")
    List<PlantSubmissionAuditTrailProjection> getAuditTrail(@Param("businessKey") String businessKey);


    // get the existing audit trail for given plant, site and vertical
    @NativeQuery("SELECT Plant_Id, PlantName, Site_Id, Vertical_Id, SubmittedBy, SubmissionDate, SubmissionRemark, VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type FROM TCS_Submission_History WHERE Plant_Id = :plantId AND Site_Id = :siteId AND Vertical_Id = :verticalId AND BusinessKey = :businessKey AND Type = :type")
    List<PlantSubmissionAuditTrailProjection> getPlantSubmissionAuditTrail(@Param("plantId") UUID plantId, @Param("siteId") UUID siteId, @Param("verticalId") UUID verticalId, @Param("businessKey") String businessKey, @Param("type") String type);


    @NativeQuery("SELECT PlantName, Site_Id, Vertical_Id, SubmittedBy, SubmissionDate, SubmissionRemark, VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type FROM TCS_Submission_History WHERE Site_Id = :siteId AND Vertical_Id = :verticalId AND BusinessKey = :businessKey AND Type = :type")
    List<PlantSubmissionAuditTrailProjection> getEbsSubmissionAuditTrail( @Param("siteId") UUID siteId, @Param("verticalId") UUID verticalId, @Param("businessKey") String businessKey, @Param("type") String type);



    @NativeQuery("""
        SELECT
            Id,
            Plant_Id,
            PlantName,
            Site_Id,
            Vertical_Id,
            SubmittedBy,
            SubmissionDate,
            SubmissionRemark,
            VerifiedDate,
            VerifiedBy,
            VerifiedRemark,
            Status,
            Type
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (
                       PARTITION BY PlantName
                       ORDER BY SubmissionDate DESC
                   ) AS rn
            FROM TCS_Submission_History
            WHERE Site_Id = :siteId
              AND Vertical_Id = :verticalId
              AND BusinessKey = :businessKey
              AND Type = :type
              AND VerifiedDate IS NULL
              AND PlantName IS NOT NULL
              
        ) t
        WHERE rn = 1
    """)
    List<PlantSubmissionAuditTrailProjection> getLatestPlantWiseSubmissionAuditTrail(
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("businessKey") String businessKey,
            @Param("type") String type
            
    );




    @NativeQuery("""
        SELECT
            Id,
            Plant_Id,
            PlantName,
            PlantStatus,
            Site_Id,
            Vertical_Id,
            SubmittedBy,
            SubmissionDate,
            SubmissionRemark,
            VerifiedDate,
            VerifiedBy,
            VerifiedRemark,
            Status,
            Type
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (
                       PARTITION BY PlantName
                       ORDER BY SubmissionDate DESC
                   ) AS rn
            FROM TCS_Submission_History
            WHERE Site_Id = :siteId
              AND Vertical_Id = :verticalId
              AND BusinessKey = :businessKey
              AND Type = :type
              AND PlantStatus = :plantStatus
              AND PlantName IS NOT NULL
              
        ) t
        WHERE rn = 1
    """)
    List<PlantSubmissionAuditTrailProjection> getLatestPendingPlantWiseSubmissionAuditTrail(
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("businessKey") String businessKey,
            @Param("type") String type,
            @Param("plantStatus") String plantStatus
            
    );



    @NativeQuery("""
    SELECT TOP 1 
        Id, Plant_Id, PlantName, Site_Id, Vertical_Id,
           SubmittedBy, SubmissionDate, SubmissionRemark,
           VerifiedDate, VerifiedBy, VerifiedRemark,
           Status, Type
    FROM TCS_Submission_History
    WHERE Plant_Id = :plantId
      AND Site_Id = :siteId
      AND Vertical_Id = :verticalId
      AND BusinessKey = :businessKey
      AND Type = :type
      AND VerifiedDate IS NULL
    ORDER BY SubmissionDate DESC
    """)
PlantSubmissionAuditTrailProjection getLatestPlantSubmissionAuditTrail(
        @Param("plantId") UUID plantId,
        @Param("siteId") UUID siteId,
        @Param("verticalId") UUID verticalId,
        @Param("businessKey") String businessKey,
        @Param("type") String type);


        @NativeQuery("""
            SELECT TOP 1 
                Id, Plant_Id, PlantName, Site_Id, Vertical_Id,
                   SubmittedBy, SubmissionDate, SubmissionRemark,
                   VerifiedDate, VerifiedBy, VerifiedRemark,
                   Status, Type
            FROM TCS_Submission_History
            WHERE Plant_Id = :plantId
              AND Site_Id = :siteId
              AND Vertical_Id = :verticalId
              AND BusinessKey = :businessKey
              AND Type = :type
              AND PlantStatus = :plantStatus
            ORDER BY SubmissionDate DESC
            """)
        PlantSubmissionAuditTrailProjection getLatestPendingPlantSubmissionAuditTrail(
                @Param("plantId") UUID plantId,
                @Param("siteId") UUID siteId,
                @Param("verticalId") UUID verticalId,
                @Param("businessKey") String businessKey,
                @Param("type") String type,
                @Param("plantStatus") String plantStatus);


        @NativeQuery("""
            SELECT TOP 1 
                  Id, Site_Id, Vertical_Id,
                   SubmittedBy, SubmissionDate, SubmissionRemark,
                   VerifiedDate, VerifiedBy, VerifiedRemark,
                   Status, Type
            FROM TCS_Submission_History
            WHERE 
             Site_Id = :siteId
              AND Vertical_Id = :verticalId
              AND BusinessKey = :businessKey
              AND Type = :type
              AND VerifiedDate IS NULL
            ORDER BY SubmissionDate DESC
            """)
        PlantSubmissionAuditTrailProjection getLatestEbsSubmissionAuditTrail(
                @Param("siteId") UUID siteId,
                @Param("verticalId") UUID verticalId,
                @Param("businessKey") String businessKey,
                @Param("type") String type);
  
// ebs approval history
    @NativeQuery("SELECT Plant_Id, PlantName, Site_Id, Vertical_Id, SubmittedBy, SubmissionDate, SubmissionRemark, " +
                "VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type " +
                "FROM TCS_Submission_History " +
                "WHERE Plant_Id = :plantId " +
                "AND Site_Id = :siteId " +
                "AND Vertical_Id = :verticalId " +
                "AND BusinessKey = :businessKey " +
                "AND Type = :type " +
                "AND VerifiedDate IS NOT NULL")
    List<PlantSubmissionAuditTrailProjection> getPlantSubmissionAuditTrailByVerfiedDate(
            @Param("plantId") UUID plantId,
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("businessKey") String businessKey,
            @Param("type") String type
           
    );

    @NativeQuery("SELECT Plant_Id, PlantName, Site_Id, Vertical_Id, SubmittedBy, SubmissionDate, SubmissionRemark, " +
                "VerifiedDate, VerifiedBy, VerifiedRemark, Status, Type " +
                "FROM TCS_Submission_History " +
                "WHERE Site_Id = :siteId " +
                "AND Vertical_Id = :verticalId " +
                "AND BusinessKey = :businessKey " +
                "AND Type = :type " +
                "AND VerifiedDate IS NOT NULL")
    List<PlantSubmissionAuditTrailProjection> getEbsSubmissionAuditTrailByVerfiedDate(
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("businessKey") String businessKey,
            @Param("type") String type
           
    );


    @Modifying
    @Transactional
    @NativeQuery("UPDATE TCS_Submission_History SET Status = :status WHERE Plant_Id = :plantId AND Site_Id = :siteId AND Vertical_Id = :verticalId AND BusinessKey = :businessKey AND Type = :type")
    void updateSubmissionStatus(
            @Param("plantId") UUID plantId,
            @Param("siteId") UUID siteId,
            @Param("verticalId") UUID verticalId,
            @Param("businessKey") String businessKey,
            @Param("type") String type,
            @Param("status") String status
            
    );

    @Modifying
    @Transactional
    @NativeQuery("UPDATE TCS_Submission_History SET Status = :status WHERE Id = :id")
    void updateSubmissionStatusById(
            @Param("id") UUID id,
            @Param("status") String status
            
    );


    @Modifying
    @Transactional
    @NativeQuery("UPDATE TCS_Submission_History SET PlantStatus = :plantStatus WHERE Id = :id")
    void updatePlantSubmissionStatusById(
            @Param("id") UUID id,
            @Param("plantStatus") String plantStatus
            
    );


    @NativeQuery("SELECT DISTINCT SiteId " +
"FROM vwVerticalSitePlantMapping " +
"WHERE VerticalId in (select Id from Verticals v where v.Name = :verticalName);")
    List<UUID> getSitesByVerticalName(@Param("verticalName") String verticalName);

    @NativeQuery("select Id from Verticals where name = :verticalName")
    UUID getVerticalIdByName(@Param("verticalName") String verticalName);

    @Modifying
    @Transactional
    @NativeQuery("DELETE FROM TCS_Submission_History WHERE BusinessKey = :businessKey")
    void deleteAuditTrailByBusinessKey(@Param("businessKey") String businessKey);


}
