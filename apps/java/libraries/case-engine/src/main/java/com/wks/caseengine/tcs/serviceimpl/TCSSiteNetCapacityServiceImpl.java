package com.wks.caseengine.tcs.serviceimpl;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;	

import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.tcs.dto.TCSSiteNetCapacityDTO;
import com.wks.caseengine.tcs.entity.TCSSiteNetCapacity;
import com.wks.caseengine.tcs.service.TCSSiteNetCapacityService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class TCSSiteNetCapacityServiceImpl implements TCSSiteNetCapacityService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
	private DataSource dataSource;
    @Autowired
	private PlantsRepository plantsRepository;
    @Autowired
    private SiteRepository siteRepository;
	@Autowired
	private VerticalsRepository verticalRepository;
  
    @Override
    public Map<String, Object> getAll(
        String plantId,
        String aopYear,
        String capacityType,
     //   String uom,
        String siteId,
        String verticalId) {
        


      
Sites site = null;
Verticals vertical = null;
//final Plants plant = null;
        // Validation
        if (plantId != null) {
       Plants plant = plantsRepository
            .findById(UUID.fromString(plantId))
            .orElseThrow(() -> new RuntimeException("Plant not found for ID: " + plantId));

       
         site = siteRepository
            .findById(plant.getSiteFkId())
            .orElseThrow(() -> new RuntimeException("Site not found for ID: " + plantId));

            vertical = verticalRepository
            .findById(plant.getVerticalFKId())
            .orElseThrow(() -> new RuntimeException("Vertical not found for ID: " + plant.getVerticalFKId()));
        }

    else  {
        site = siteRepository
            .findById(UUID.fromString(siteId))
            .orElseThrow(() -> new RuntimeException("Site not found for ID: " + siteId));
    

 
    vertical = verticalRepository
        .findById(UUID.fromString(verticalId))
        .orElseThrow(() -> new RuntimeException("Vertical not found for ID: " + verticalId));
    }

        Map<String, Object> map = new HashMap<>();
        try {
            List<Object[]> results = new ArrayList<>();
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

         
              results = getData(
                plantId,
                aopYear,
                vertical.getName().toUpperCase(),
                site.getId(),
                capacityType
               // uom
            );  
            
        //    for(Object[] row : results) {
        // System.out.println("row1: " + row[0].toString());
        // System.out.println("row2: " + row[1].toString());
        // System.out.println("row3: " + row[2].toString());
        // System.out.println("row4: " + Double.parseDouble(row[3].toString()));
        // System.out.println("row5: " + Double.parseDouble(row[4].toString()));
        // System.out.println("row6: " + row[5].toString());
        // System.out.println("row7: " + dateFormatter.parse(row[6].toString()));
        //     System.out.println(row[0].toString() + " " + row[1].toString() + " " + row[2].toString() + " " + Double.parseDouble(row[3].toString()) + " " + Double.parseDouble(row[4].toString()) + " " + row[5].toString() + " " + dateFormatter.parse(row[6].toString()));
        //    }

            List<TCSUnitCapacityDTO> resultsList = new ArrayList<>();
            for (Object[] row : results) {


                TCSSiteNetCapacityDTO dto = new TCSSiteNetCapacityDTO();
                dto.setId(row[0] != null ? row[0].toString() : null);
                dto.setParticulates(row[1] != null ? row[1].toString() : null);
                dto.setApr(row[2] != null ? Double.parseDouble(row[2].toString()) : null);
                dto.setMay(row[3] != null ? Double.parseDouble(row[3].toString()) : null);
                dto.setJun(row[4] != null ? Double.parseDouble(row[4].toString()) : null);
                dto.setJul(row[5] != null ? Double.parseDouble(row[5].toString()) : null);
                dto.setAug(row[6] != null ? Double.parseDouble(row[6].toString()) : null);
                dto.setSep(row[7] != null ? Double.parseDouble(row[7].toString()) : null);
                dto.setOct(row[8] != null ? Double.parseDouble(row[8].toString()) : null);
                dto.setNov(row[9] != null ? Double.parseDouble(row[9].toString()) : null);
                dto.setDec(row[10] != null ? Double.parseDouble(row[10].toString()) : null);
                dto.setJan(row[11] != null ? Double.parseDouble(row[11].toString()) : null);
                dto.setFeb(row[12] != null ? Double.parseDouble(row[12].toString()) : null);
                dto.setMar(row[13] != null ? Double.parseDouble(row[13].toString()) : null);
             //   dto.setUom(row[2] != null ? row[2].toString() : null);
                dto.setRemark(row[14] != null ? row[14].toString() : null);
                dto.setInsertedDateTime(row[15] != null ? dateFormatter.parse(row[15].toString()) : null);
                resultsList.add(dto);
            }
            map.put("results", resultsList);

            // headers mapping
            List<String> headers = getHeaders(
                plantId,
                aopYear,
                vertical.getName().toUpperCase(),
                site.getName().toUpperCase(),
                capacityType
              //  uom
            );
            map.put("headers", headers);

            List<String> keys = new ArrayList<>();
            for (Field field : TCSSiteNetCapacityDTO.class.getDeclaredFields()) {
                keys.add(field.getName());
            }
            map.put("keys", keys);

            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch data", e);
        }
    }

    private List<Object[]> getData(
        String plantId,
        String aopYear,
        String verticalName,
        UUID siteId,
        String capacityType
    //    String uom
    ) {
            
        try {            
            // Stored Procedure name
            String procedureName = "GetPlantNetCapacity";
            if (!"MEG".equalsIgnoreCase(verticalName)) {
                if(plantId != null) {
                //procedureName = verticalName + "_" + "ALL" + "_GetTcsUnitCapacity";  
                procedureName = "DTA" + "_GetPlantNetCapacity";  
            }
            else  {
               // procedureName = verticalName + "_" + "ALL" + "_GetTcsUnitCapacity_OutPut"; 
                procedureName = "DTA_GetPlantNetCapacity";
            }
            }

            // Prepare native SQL call with parameters
            String sql = "";
            if(plantId != null) {
            sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear, @capacityType = :capacityType";
            }
            else {
              //  sql = "EXEC " + procedureName + " @aopYear = :aopYear, @capacityType = :capacityType";
              sql = "EXEC " + procedureName + " ?, ?, ?";

            }

            // Call the stored procedure
            Query query = entityManager.createNativeQuery(sql);
            if(plantId != null) {
            query.setParameter("plantId", plantId);
            
          
            query.setParameter("aopYear", aopYear);
            query.setParameter("capacityType", capacityType);  }

            else {
                query.setParameter(1, siteId);
                query.setParameter(2, aopYear);      
                query.setParameter(3, capacityType);

            }
         //   query.setParameter("uom", uom);

            return query.getResultList();
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch data", e);
        }
    }

    private List<String> getHeaders(
        String plantId,
        String aopYear,
        String verticalName,
        String siteId,
        String capacityType
    //    String uom
    ) {

        String procedureName = "GetPlantNetCapacity";
        if (!"MEG".equalsIgnoreCase(verticalName)) {
            if(plantId != null) {
            //procedureName = verticalName + "_" + "ALL" + "_GetTcsUnitCapacity";  
                procedureName = "DTA" + "_GetPlantNetCapacity";  
            }
            else  {
               // procedureName = verticalName + "_" + "ALL" + "_GetTcsUnitCapacity_OutPut"; 
                procedureName = "DTA_GetPlantNetCapacity";
            }
        }

        String callableSql = "";
        if(plantId != null) {
        callableSql = "{call " + procedureName + "(?, ?, ?)}";  }
        else {
            callableSql = "{call " + procedureName + "(?, ?, ?)}";
        }

        List<String> headers = new ArrayList<>();
		try (
            Connection conn = dataSource.getConnection();
			CallableStatement stmt = conn.prepareCall(callableSql)) {
              if(plantId != null) {
			stmt.setString(1, plantId);  
                    
			stmt.setString(2, aopYear);
            stmt.setString(3, capacityType);  
             //     stmt.setString(4, uom);
        
        }

        else {
            stmt.setString(1, siteId);
            stmt.setString(2, aopYear);
            stmt.setString(3, capacityType);
        }
      

			boolean hasResultSet = stmt.execute();

			// Move forward until we find a result set
			while (!hasResultSet && stmt.getUpdateCount() != -1) {
				hasResultSet = stmt.getMoreResults();
			}

			// If a result set is found, get metadata and headers
			if (hasResultSet) {
				try (ResultSet rs = stmt.getResultSet()) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();

					for (int i = 1; i <= columnCount; i++)
                    {
                        String columnLabel = metaData.getColumnLabel(i);
                        headers.add(columnLabel);
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to fetch headers", e);
		}

        return headers;
    }
}