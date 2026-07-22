package com.wks.caseengine.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.OtherDocumentsDTO;
import com.wks.caseengine.dto.OtherDocumnetInformationDTO;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.rest.entity.Vertical;
import com.wks.caseengine.utility.Utility;

@Service
public class OtherDocumentsServiceImpl implements OtherDocumentsService {

    @Autowired
    private VerticalsRepository verticalRepository;
    
    private final DataSource dataSource;

    public OtherDocumentsServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    private List<OtherDocumentsDTO> fetchDocumentsFromSP(String procedureName, String verticalId, String aopYear) throws SQLException {
        String callSql = "{call [RIL.AOP].[dbo]." + procedureName + "(?, ?)}";
        List<OtherDocumentsDTO> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(callSql)) {

            stmt.setString(1, verticalId);
            stmt.setString(2, aopYear);

            boolean hasResultSet = stmt.execute();
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    while (rs.next()) {
                        String rawTransactionId = rs.getString("TransactionId");
                        byte[] contentBytes = rs.getBytes("Content");

                        OtherDocumentsDTO dto = OtherDocumentsDTO.builder()
                                .transactionId(rawTransactionId)
                                .masterId(rs.getString("MasterId"))
                                .documentName(rs.getString("DocumentName"))
                                .contentType(rs.getString("ContentType"))
                                .uploadedDateTime(rs.getTimestamp("UploadedDateTime") != null
                                        ? new Date(rs.getTimestamp("UploadedDateTime").getTime())
                                        : null)
                                .uploadedBy(rs.getString("UploadedBy"))
                                .verticalId(rs.getString("VerticalId"))
                                .aopYear(rs.getString("AOPYear"))
                                .modifiedBy(rs.getString("ModifiedBy"))
                                .modifiedOn(rs.getTimestamp("ModifiedOn") != null
                                        ? new Date(rs.getTimestamp("ModifiedOn").getTime())
                                        : null)
                                .content(contentBytes != null
                                        ? Base64.getEncoder().encodeToString(contentBytes)
                                        : null)
                                .build();
                        result.add(dto);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public AOPMessageVM getDocuments(String verticalId, String aopYear) {
        AOPMessageVM response = new AOPMessageVM();
        Verticals vertical = verticalRepository.findById(UUID.fromString(verticalId)).get();
        try {
            String procedureName = vertical.getName() + "_GetRefineryOtherDocuments";
            List<OtherDocumentsDTO> documents = fetchDocumentsFromSP(procedureName, verticalId, aopYear);
            response.setCode(200);
            response.setMessage("Data fetched successfully");
            response.setData(documents);
        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(500);
            response.setMessage("Error fetching documents: " + e.getMessage());
        }
        return response;
    }

    @Override
    public AOPMessageVM uploadOrUpdateDocument(String transactionId, String masterId,
            String verticalId, String aopYear, MultipartFile file) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            byte[] content = file.getBytes();
            String contentType = file.getContentType();
            String currentUser = Utility.getUserName();
            Timestamp now = Timestamp.from(Instant.now());

            if (transactionId == null || transactionId.isBlank()) {
                insertDocument(masterId, verticalId, aopYear, content, contentType, currentUser, now);
                response.setCode(200);
                response.setMessage("Document uploaded successfully");
            } else {
                updateDocument(transactionId, content, contentType, currentUser, now);
                response.setCode(200);
                response.setMessage("Document updated successfully");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(500);
            response.setMessage("Error saving document: " + e.getMessage());
        }
        return response;
    }

    private void insertDocument(String masterId, String verticalId, String aopYear,
            byte[] content, String contentType, String uploadedBy, Timestamp uploadedDateTime)
            throws SQLException {
        String sql = "INSERT INTO RefineryOtherDocumentTransaction "
                + "(Id, MasterId, VerticalId, AOPYear, Content, ContentType, UploadedBy, UploadedDateTime) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, masterId);
            stmt.setString(3, verticalId);
            stmt.setString(4, aopYear);
            stmt.setBytes(5, content);
            stmt.setString(6, contentType);
            stmt.setString(7, uploadedBy);
            stmt.setTimestamp(8, uploadedDateTime);
            stmt.executeUpdate();
        }
    }

    private void updateDocument(String transactionId, byte[] content, String contentType,
            String modifiedBy, Timestamp modifiedOn) throws SQLException {
        String sql = "UPDATE RefineryOtherDocumentTransaction "
                + "SET Content = ?, ContentType = ?, ModifiedBy = ?, ModifiedOn = ?, "
                + "UploadedBy = ?, UploadedDateTime = ? "
                + "WHERE Id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBytes(1, content);
            stmt.setString(2, contentType);
            stmt.setString(3, modifiedBy);
            stmt.setTimestamp(4, modifiedOn);
            stmt.setString(5, modifiedBy);
            stmt.setTimestamp(6, modifiedOn);
            stmt.setString(7, transactionId);
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // DELETE: remove a transaction record by TransactionId
    // -------------------------------------------------------------------------
    @Override
    public AOPMessageVM deleteDocument(String transactionId) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            String sql = "DELETE FROM RefineryOtherDocumentTransaction WHERE Id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, transactionId);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    response.setCode(200);
                    response.setMessage("Document deleted successfully");
                } else {
                    response.setCode(404);
                    response.setMessage("No document found with TransactionId: " + transactionId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(500);
            response.setMessage("Error deleting document: " + e.getMessage());
        }
        return response;
    }

    @Override
    public AOPMessageVM getOtherDocumentInformation(String verticalId, String aopYear) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            String sql = "SELECT * FROM OtherDocumnetInformation WHERE VerticalId = ? AND AOPYear = ?";
            List<OtherDocumnetInformationDTO> otherDocumentInformation = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, verticalId);
                stmt.setString(2, aopYear);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    OtherDocumnetInformationDTO dto = OtherDocumnetInformationDTO.builder()
                            .id(rs.getString("Id"))
                            .otherInformation(rs.getString("OtherInformation"))
                            .verticalId(rs.getString("VerticalId"))
                            .aopYear(rs.getString("AOPYear"))
                            .modifiedBy(rs.getString("ModifiedBy"))
                            .modifiedOn(rs.getTimestamp("ModifiedOn") != null
                                    ? new Date(rs.getTimestamp("ModifiedOn").getTime()) : null)
                            .build();
                    otherDocumentInformation.add(dto);
                }

                response.setCode(200);
                response.setMessage("Other document information fetched successfully");
                response.setData(otherDocumentInformation);
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setCode(500);
            response.setMessage("Error fetching other document information: " + e.getMessage());
            return response;
        }

       
    }

     @Override
     @Transactional
    public AOPMessageVM saveOrUpdateOtherDocumentInformation  (String verticalId, String aopYear, List<OtherDocumnetInformationDTO> otherDocumentInformation) {
        AOPMessageVM response = new AOPMessageVM();
        String currentUser = Utility.getUserName();
        Timestamp now = Timestamp.from(Instant.now());
       
        String insertSql = "INSERT INTO OtherDocumnetInformation (Id, OtherInformation, VerticalId, AOPYear, ModifiedBy, ModifiedOn) VALUES (?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE OtherDocumnetInformation set OtherInformation = ?, ModifiedBy = ?, ModifiedOn = ? WHERE Id = ?";
            for (OtherDocumnetInformationDTO dto : otherDocumentInformation) {

                if(dto.getId() == null) {
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        stmt.setString(1, UUID.randomUUID().toString());
                        stmt.setString(2, dto.getOtherInformation());
                        stmt.setString(3, verticalId);
                        stmt.setString(4, aopYear);
                        stmt.setString(5, currentUser);
                        stmt.setTimestamp(6, now);
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                        stmt.setString(1, dto.getOtherInformation());
                        stmt.setString(2, currentUser);
                        stmt.setTimestamp(3, now);
                        stmt.setString(4, dto.getId());
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            response.setCode(200);
            response.setMessage("Other document information saved successfully");
            return response;
        }

}
