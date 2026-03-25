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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.ShutdownDetailsDTO;
import com.wks.caseengine.entity.PlannedShutdownDetails;
import com.wks.caseengine.entity.RoutineShutdownPreviousYears;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlannedShutdownDetailsRepository;
import com.wks.caseengine.repository.RoutineShutdownPreviousYearsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownDetailsServiceImpl implements ShutdownDetailsService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Autowired
    private PlannedShutdownDetailsRepository plannedShutdownDetailsRepository;

    @Autowired
    private RoutineShutdownPreviousYearsRepository routineShutdownPreviousYearsRepository;

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
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setShutdownFrom(toTimestampAsDate(row, 2));
                    dto.setShutdownTo(toTimestampAsDate(row, 3));
                    dto.setDurationHrs(toDouble(row, 4));
                    dto.setRemarks(toStringOrEmpty(row, 5));
                    dto.setYear(toStringOrEmpty(row, 6));
                    dto.setPlantFkId(toStringOrEmpty(row, 7));
                    dto.setCreatedOn(toTimestampAsDate(row, 8));
                    dto.setModifiedOn(toTimestampAsDate(row, 9));
                    dto.setUpdatedBy(toStringOrEmpty(row, 10));

                } else if ("RoutineShutdown".equalsIgnoreCase(type)) {
                    // Id, Activities, April..March, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setApril(toDouble(row, 2));
                    dto.setMay(toDouble(row, 3));
                    dto.setJune(toDouble(row, 4));
                    dto.setJuly(toDouble(row, 5));
                    dto.setAugust(toDouble(row, 6));
                    dto.setSeptember(toDouble(row, 7));
                    dto.setOctober(toDouble(row, 8));
                    dto.setNovember(toDouble(row, 9));
                    dto.setDecember(toDouble(row, 10));
                    dto.setJanuary(toDouble(row, 11));
                    dto.setFebruary(toDouble(row, 12));
                    dto.setMarch(toDouble(row, 13));
                    dto.setYear(toStringOrEmpty(row, 14));
                    dto.setPlantFkId(toStringOrEmpty(row, 15));
                    dto.setCreatedOn(toTimestampAsDate(row, 16));
                    dto.setModifiedOn(toTimestampAsDate(row, 17));
                    dto.setUpdatedBy(toStringOrEmpty(row, 18));

                } else {
                    // RoutineShutdownPreviousYears
                    // Id, Activities, PrevYear1..PrevYear4, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setPrevYear1(toDouble(row, 2));
                    dto.setPrevYear2(toDouble(row, 3));
                    dto.setPrevYear3(toDouble(row, 4));
                    dto.setPrevYear4(toDouble(row, 5));
                    dto.setYear(toStringOrEmpty(row, 6));
                    dto.setPlantFkId(toStringOrEmpty(row, 7));
                    dto.setCreatedOn(toTimestampAsDate(row, 8));
                    dto.setModifiedOn(toTimestampAsDate(row, 9));
                    dto.setUpdatedBy(toStringOrEmpty(row, 10));
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

                PlannedShutdownDetails entity =
                        upsertPlannedShutdownDetails(dto, plantUuid, year, now);
                plannedShutdownDetailsRepository.save(entity);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save shutdown details", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveRoutineShutdownPreviousYears(String plantId, String year, ShutdownDetailsDTO shutdownDetailsDTO) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {

            UUID plantUuid = UUID.fromString(plantId);
            Date now = new Date();

            RoutineShutdownPreviousYears entity =
                    upsertRoutineShutdownPreviousYears(shutdownDetailsDTO, plantUuid, year, now);
            routineShutdownPreviousYearsRepository.save(entity);

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", 1);
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

    private com.wks.caseengine.entity.RoutineShutdownPreviousYears upsertRoutineShutdownPreviousYears(
            ShutdownDetailsDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

        RoutineShutdownPreviousYears entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = routineShutdownPreviousYearsRepository.findById(id).orElse(null);
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
        }

        return entity;
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

            boolean exists = plannedShutdownDetailsRepository.findById(uuid).isPresent();
            if (!exists) {
                throw new RestInvalidArgumentException("PlannedShutdownDetails id", new RuntimeException("not found"));
            }

            plannedShutdownDetailsRepository.deleteById(uuid);

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

            boolean exists = routineShutdownPreviousYearsRepository.findById(uuid).isPresent();
            if (!exists) {
                throw new RestInvalidArgumentException("RoutineShutdownPreviousYears id", new RuntimeException("not found"));
            }

            routineShutdownPreviousYearsRepository.deleteById(uuid);

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
            entity = plannedShutdownDetailsRepository.findById(id).orElse(null);
            isUpdate = entity != null;

            // If id is provided but row doesn't exist, insert using the provided id.
            if (entity == null) {
                entity = new com.wks.caseengine.entity.PlannedShutdownDetails();
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

    private static String toStringOrEmpty(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    private static Double toDouble(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return 0.0;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
