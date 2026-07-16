package com.wks.caseengine.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.ShutdownDetailsDTO;
import com.wks.caseengine.db2.entity.PlannedShutdownDetails;
import com.wks.caseengine.db2.entity.RoutineShutdownDetails;
import com.wks.caseengine.db2.entity.RoutineShutdownPreviousYears;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownDetailsServiceImpl implements ShutdownDetailsService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getShutdownDetails(String plantId, String year, String type) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            String sql = "EXEC Sp_GetShutdownDetails @PlantId = :plantId, @Year = :year, @Type = :type";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("year", year);
            query.setParameter("type", type);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<ShutdownDetailsDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                ShutdownDetailsDTO dto = new ShutdownDetailsDTO();

                if ("PlannedShutdown".equalsIgnoreCase(type)) {
                    // Id, Activities, ShutdownFrom, ShutdownTo, DurationHrs, Remarks, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(row[0] != null ? row[0].toString() : "");
                    dto.setActivities(row[1] != null ? row[1].toString() : "");
                    dto.setShutdownFrom(toTimestampAsDate(row, 2));
                    dto.setShutdownTo(toTimestampAsDate(row, 3));
                    dto.setDurationHrs(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                    dto.setRemarks(row[5] != null ? row[5].toString() : "");

                } else if ("RoutineShutdown".equalsIgnoreCase(type)) {
                    // Id, Activities, April..March, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(row[0] != null ? row[0].toString() : "");
                    dto.setActivities(row[1] != null ? row[1].toString() : "");
                    dto.setApril(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                    dto.setMay(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                    dto.setJune(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                    dto.setJuly(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                    dto.setAugust(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                    dto.setSeptember(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                    dto.setOctober(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
                    dto.setNovember(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
                    dto.setDecember(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
                    dto.setJanuary(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
                    dto.setFebruary(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
                    dto.setMarch(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
                    dto.setRemarks(row[19] != null ? row[19].toString() : "");

                } else {
                    // RoutineShutdownPreviousYears
                    // Id, Activities, PrevYear1..PrevYear4, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(row[0] != null ? row[0].toString() : null);
                    dto.setActivities(row[1] != null ? row[1].toString() : null);
                    dto.setPrevYear1(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                    dto.setPrevYear2(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                    dto.setPrevYear3(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                    dto.setPrevYear4(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                    dto.setYear(row[6] != null ? row[6].toString() : null);
                    dto.setPlantFkId(row[7] != null ? row[7].toString() : null);
                    dto.setCreatedOn(toTimestampAsDate(row, 8));
                    dto.setModifiedOn(toTimestampAsDate(row, 9));
                    dto.setUpdatedBy(row[10] != null ? row[10].toString() : null);
                }

                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("shutdownDetailsList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch shutdown details", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveShutdownDetails(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            Date now = new Date();

            if (shutdownDetailsDTOs == null) {
                shutdownDetailsDTOs = new ArrayList<>();
            }

            int savedCount = 0;
            for (ShutdownDetailsDTO dto : shutdownDetailsDTOs) {
                if (dto == null) {
                    continue;
                }

                upsertPlannedShutdownDetails(dto, plantUuid, year, now);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
        	e.printStackTrace();
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
        	e.printStackTrace();
            throw e;
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new RuntimeException("Failed to save shutdown details", ex);
        }
    }
    
    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveRoutineShutdwn(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            Date now = new Date();

            if (shutdownDetailsDTOs == null) {
                shutdownDetailsDTOs = new ArrayList<>();
            }

            int savedCount = 0;
            for (ShutdownDetailsDTO dto : shutdownDetailsDTOs) {
                if (dto == null) {
                    continue;
                }

                updateRoutineShutdwnDetails(dto, plantUuid, year, now);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
        	e.printStackTrace();
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
        	e.printStackTrace();
            throw e;
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new RuntimeException("Failed to save shutdown details", ex);
        }
    }


    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveRoutineShutdownPreviousYears(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        List<RoutineShutdownPreviousYears> routineShutdownPreviousYears = new ArrayList<RoutineShutdownPreviousYears>();
        try {

            UUID plantUuid = UUID.fromString(plantId);
            Date now = new Date();
            for(ShutdownDetailsDTO shutdownDetailsDTO:shutdownDetailsDTOs) {
            	routineShutdownPreviousYears.add(upsertRoutineShutdownPreviousYears(shutdownDetailsDTO, plantUuid, year, now));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("save", routineShutdownPreviousYears);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save routine shutdown previous years", ex);
        }
    }

    private RoutineShutdownPreviousYears upsertRoutineShutdownPreviousYears(
            ShutdownDetailsDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

        RoutineShutdownPreviousYears entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(RoutineShutdownPreviousYears.class, id);
            isUpdate = entity != null;

            // If id is provided but row doesn't exist, insert using the provided id.
            if (entity == null) {
                entity = new RoutineShutdownPreviousYears();
                entity.setId(id);
            }
        } else {
            entity = new RoutineShutdownPreviousYears();
        }

        entity.setActivities(dto.getActivities());
        entity.setPrevYear1(dto.getPrevYear1());
        entity.setPrevYear2(dto.getPrevYear2());
        entity.setPrevYear3(dto.getPrevYear3());
        entity.setPrevYear4(dto.getPrevYear4());
        entity.setYear(year != null ? year : dto.getYear());
        entity.setPlantFkId(plantUuid);
        entity.setUpdatedBy(Utility.getUserName());

        if (isUpdate) {
            entity.setModifiedOn(now);
        } else {
            entity.setCreatedOn(now);
            entityManager.persist(entity);
        }

        return entity;
    }

    
    
    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM deleteRoutineShutdown(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID uuid = parseUuidOrNull(id);
            if (uuid == null) {
                throw new RestInvalidArgumentException("id", new IllegalArgumentException("empty or invalid id"));
            }

            RoutineShutdownDetails entity = entityManager.find(RoutineShutdownDetails.class, uuid);
            if (entity == null) {
                throw new RestInvalidArgumentException("RoutineShutdownDetails id", new RuntimeException("not found"));
            }

            entityManager.remove(entity);

            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", 1);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data deleted successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for id", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete routine shutdown details", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM deletePlannedShutdownDetails(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID uuid = parseUuidOrNull(id);
            if (uuid == null) {
                throw new RestInvalidArgumentException("id", new IllegalArgumentException("empty or invalid id"));
            }

            PlannedShutdownDetails entity = entityManager.find(PlannedShutdownDetails.class, uuid);
            if (entity == null) {
                throw new RestInvalidArgumentException("PlannedShutdownDetails id", new RuntimeException("not found"));
            }

            entityManager.remove(entity);

            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", 1);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data deleted successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for id", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete planned shutdown details", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM deleteRoutineShutdownPreviousYears(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID uuid = parseUuidOrNull(id);
            if (uuid == null) {
                throw new RestInvalidArgumentException("id", new IllegalArgumentException("empty or invalid id"));
            }

            RoutineShutdownPreviousYears entity = entityManager.find(RoutineShutdownPreviousYears.class, uuid);
            if (entity == null) {
                throw new RestInvalidArgumentException("RoutineShutdownPreviousYears id", new RuntimeException("not found"));
            }

            entityManager.remove(entity);

            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", 1);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data deleted successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for id", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete routine shutdown previous years", ex);
        }
    }

    private PlannedShutdownDetails upsertPlannedShutdownDetails(
            ShutdownDetailsDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

        PlannedShutdownDetails entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(PlannedShutdownDetails.class, id);
            isUpdate = entity != null;

            // If id is provided but row doesn't exist, insert using the provided id.
            if (entity == null) {
                entity = new PlannedShutdownDetails();
                entity.setId(id);
            }
        } else {
            entity = new PlannedShutdownDetails();
        }

        entity.setActivities(dto.getActivities());
        entity.setShutdownFrom(dto.getShutdownFrom());
        entity.setShutdownTo(dto.getShutdownTo());
        entity.setDurationHrs(dto.getDurationHrs());
        entity.setRemarks(dto.getRemarks());
        entity.setYear(year != null ? year : dto.getYear());
        entity.setPlantFkId(plantUuid);
        entity.setUpdatedBy(Utility.getUserName());

        if (isUpdate) {
            entity.setModifiedOn(now);
        } else {
            entity.setCreatedOn(now);
            entityManager.persist(entity);
        }

        return entity;
    }

    private RoutineShutdownDetails updateRoutineShutdwnDetails(
            ShutdownDetailsDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

    	RoutineShutdownDetails entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(RoutineShutdownDetails.class, id);
            isUpdate = entity != null;

            
            if (entity == null) {
                entity = new RoutineShutdownDetails();
                entity.setId(id);
            }
        } else {
            entity = new RoutineShutdownDetails();
        }

        entity.setActivities(dto.getActivities());
        entity.setApril(dto.getApril());
        entity.setMay(dto.getMay());
        entity.setJune(dto.getJune());
        entity.setJuly(dto.getJuly());
        entity.setAugust(dto.getAugust());
        entity.setSeptember(dto.getSeptember());
        entity.setOctober(dto.getOctober());
        entity.setNovember(dto.getNovember());
        entity.setDecember(dto.getDecember());
        entity.setJanuary(dto.getJanuary());
        entity.setFebruary(dto.getFebruary());
        entity.setMarch(dto.getMarch());
        entity.setRemarks(dto.getRemarks());
        entity.setUpdatedBy(Utility.getUserName());
        entity.setPlantFkId(plantUuid);
        entity.setYear(year);

        if (isUpdate) {
            entity.setModifiedOn(now);
        } else {
            entity.setCreatedOn(now);
            entityManager.persist(entity);
        }

        return entity;
    }

    private static UUID parseUuidOrNull(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return UUID.fromString(trimmed);
    }

    private static java.util.Date parseDateOrNull(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim();
        if (s.isEmpty()) {
            return null;
        }

        try {
            // Most common: yyyy-MM-dd
            LocalDate ld = LocalDate.parse(s);
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            // continue to try other formats
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(s);
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            // continue to try other formats
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(s);
            return Date.from(odt.toInstant());
        } catch (Exception ignored) {
            // continue to try other formats
        }

        // Last resort for non-ISO formats: dd/MM/yyyy or dd-MM-yyyy
        String[] patterns = new String[] { "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "dd.MM.yyyy" };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(s);
            } catch (Exception ignored) {
                // try next pattern
            }
        }

       return null;
    }

    private static java.util.Date toTimestampAsDate(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return null;
        }
        Object value = row[index];
        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }
        if (value instanceof Timestamp) {
            return new java.util.Date(((Timestamp) value).getTime());
        }
        return null;
    }
}
