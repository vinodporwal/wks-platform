# Plant Requirement Screen - GET API Endpoint Stored Procedure Documentation

## Overview
This document describes the stored procedures used for the Plant Requirement screen GET API endpoint.

## Current Implementation

### Primary Stored Procedure: `CPP_NMD_GetProcessDemandByYear`

**Purpose**: Retrieves plant requirement data (process demand) for a given financial year by combining master data with calculated/actual values.

**Location**: `all_stored_procedures_combined.sql` (lines 2762-2803)

**Endpoint**: `GET /task/plant-requirement/{financialYear}`

**Java Service**: `ConsumptionServiceImpl.getProcessDemand()`

**Repository**: `CalculatedProcessDemandRepository.getProcessDemandByYear()`

---

## Stored Procedure Details

### 1. CPP_NMD_GetProcessDemandByYear (Recommended)

```sql
CREATE PROCEDURE [dbo].[CPP_NMD_GetProcessDemandByYear]
    @FinancialYear VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        -- Use calculated data ID if exists, otherwise generate new GUID
        ISNULL(c.id, NEWID()) AS id,
        @FinancialYear AS financial_year,
        m.process_plant,
        m.process_plant_id,
        m.cpp_utility,
        m.cpp_utility_id,
        m.cpp_plant,
        m.cpp_plant_id,
        m.uom,
        -- Return calculated values or 0 if not exists
        ISNULL(c.apr, 0) AS apr,
        ISNULL(c.may, 0) AS may,
        ISNULL(c.jun, 0) AS jun,
        ISNULL(c.jul, 0) AS jul,
        ISNULL(c.aug, 0) AS aug,
        ISNULL(c.sep, 0) AS sep,
        ISNULL(c.oct, 0) AS oct,
        ISNULL(c.nov, 0) AS nov,
        ISNULL(c.dec, 0) AS dec,
        ISNULL(c.jan, 0) AS jan,
        ISNULL(c.feb, 0) AS feb,
        ISNULL(c.mar, 0) AS mar,
        -- Flag to indicate if data is calculated or default
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS is_calculated,
        c.remarks
    FROM dbo.ProcessDemandMaster m
    LEFT JOIN dbo.CalculatedProcessDemand c 
        ON m.process_plant_id = c.process_plant_id
        AND m.cpp_utility_id = c.cpp_utility_id
        AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
        AND c.financial_year = @FinancialYear
    WHERE m.is_active = 1
    ORDER BY m.process_plant, m.cpp_utility;
END
```

#### Parameters
- **@FinancialYear** (VARCHAR(10)): Financial year in format 'YYYY-YY' (e.g., '2025-26')

#### Returns
Result set with the following columns:

| Column | Type | Description |
|--------|------|-------------|
| id | UNIQUEIDENTIFIER | Record ID (from CalculatedProcessDemand or new GUID) |
| financial_year | VARCHAR(10) | Financial year parameter |
| process_plant | NVARCHAR | Process plant name |
| process_plant_id | NVARCHAR | Process plant code/ID |
| cpp_utility | NVARCHAR | CPP utility name |
| cpp_utility_id | NVARCHAR | CPP utility SAP material code |
| cpp_plant | NVARCHAR | CPP plant name |
| cpp_plant_id | NVARCHAR | CPP plant ID |
| uom | NVARCHAR | Unit of measurement |
| apr | DECIMAL | April value |
| may | DECIMAL | May value |
| jun | DECIMAL | June value |
| jul | DECIMAL | July value |
| aug | DECIMAL | August value |
| sep | DECIMAL | September value |
| oct | DECIMAL | October value |
| nov | DECIMAL | November value |
| dec | DECIMAL | December value |
| jan | DECIMAL | January value |
| feb | DECIMAL | February value |
| mar | DECIMAL | March value |
| is_calculated | INT | 1 if data exists in CalculatedProcessDemand, 0 if default |
| remarks | NVARCHAR | User remarks/comments |

#### Database Tables Used

**1. ProcessDemandMaster** (Master Data)
- Contains the master list of all valid process plant and CPP utility combinations
- Fields: process_plant, process_plant_id, cpp_utility, cpp_utility_id, cpp_plant, cpp_plant_id, uom, is_active

**2. CalculatedProcessDemand** (Transactional Data)
- Contains actual/calculated values for each financial year
- Fields: id, financial_year, process_plant_id, cpp_utility_id, cpp_plant_id, apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar, remarks, created_at, updated_at

#### Logic Flow
1. Retrieves all active records from `ProcessDemandMaster`
2. LEFT JOINs with `CalculatedProcessDemand` for the specified financial year
3. Returns master data with calculated values (or 0 if no data exists)
4. Includes `is_calculated` flag to indicate whether data has been entered
5. Orders results by process plant and CPP utility

---

### 2. CPP_NMD_GetPlantConsumptionByMaterial (Legacy)

**Purpose**: Retrieves plant consumption data aggregated by material for a specific CPP plant.

**Location**: `all_stored_procedures_combined.sql` (lines 2486-2596)

**Endpoint**: `GET /task/plant-requirement/{plantId}/{financialYear}` (Legacy endpoint)

**Java Service**: `ConsumptionServiceImpl.getCppConsumptions()`

**Repository**: `PlantsRepository.findPlantConsumptionByMaterial()`

**Status**: ⚠️ Legacy - Use `CPP_NMD_GetProcessDemandByYear` instead

```sql
CREATE PROCEDURE [dbo].[CPP_NMD_GetPlantConsumptionByMaterial]
    @CPPPlantId UNIQUEIDENTIFIER,
    @AOPYear NVARCHAR(10) = NULL
AS
BEGIN
    -- Resolves financial year if not provided
    -- Retrieves mapped plant IDs
    -- Aggregates consumption data from AOPConsumptionNorm table
    -- Returns data grouped by CPP utility
END
```

#### Key Differences from New SP
- Requires `@CPPPlantId` parameter (plant-specific)
- Aggregates data from `AOPConsumptionNorm` table
- Multiplies values by 100 for display
- Returns `GrandTotal` column
- Does not have `is_calculated` flag

---

## API Integration

### Java Controller Endpoint
```java
@GetMapping(value = "/plant-requirement/{financialYear}")
public ResponseEntity<List<CalculatedProcessDemandDTO>> getProcessDemand(
    @PathVariable String financialYear) {
    List<CalculatedProcessDemandDTO> listOfCppConsumptions = 
        consumptionService.getProcessDemand(financialYear);
    return ResponseEntity.ok(listOfCppConsumptions);
}
```

### Frontend API Call
```javascript
async function getPlantRequirementData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/plant-requirement/${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const resp = await fetch(url, { method: 'GET', headers })
  return json(keycloak, resp)
}
```

### Response DTO Mapping
```java
private CalculatedProcessDemandDTO mapRowToDTO(Object[] row) {
    return CalculatedProcessDemandDTO.builder()
        .id(row[0] != null ? UUID.fromString(row[0].toString()) : null)
        .financialYear(row[1] != null ? row[1].toString() : null)
        .processPlant(row[2] != null ? row[2].toString() : null)
        .processPlantId(row[3] != null ? row[3].toString() : null)
        .cppUtility(row[4] != null ? row[4].toString() : null)
        .cppUtilityId(row[5] != null ? row[5].toString() : null)
        .cppPlant(row[6] != null ? row[6].toString() : null)
        .cppPlantId(row[7] != null ? row[7].toString() : null)
        .uom(row[8] != null ? row[8].toString() : null)
        .apr(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0)
        .may(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0)
        .jun(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0)
        .jul(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0)
        .aug(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0)
        .sep(row[14] != null ? Double.parseDouble(row[14].toString()) : 0.0)
        .oct(row[15] != null ? Double.parseDouble(row[15].toString()) : 0.0)
        .nov(row[16] != null ? Double.parseDouble(row[16].toString()) : 0.0)
        .dec(row[17] != null ? Double.parseDouble(row[17].toString()) : 0.0)
        .jan(row[18] != null ? Double.parseDouble(row[18].toString()) : 0.0)
        .feb(row[19] != null ? Double.parseDouble(row[19].toString()) : 0.0)
        .mar(row[20] != null ? Double.parseDouble(row[20].toString()) : 0.0)
        .isCalculated(row[21] != null && Integer.parseInt(row[21].toString()) == 1)
        .remarks(row[22] != null ? row[22].toString() : null)
        .build();
}
```

---

## Usage Examples

### Example 1: Get Plant Requirement Data for FY 2025-26
```sql
EXEC dbo.CPP_NMD_GetProcessDemandByYear @FinancialYear = '2025-26'
```

### Example 2: Check if Data Exists for a Year
```sql
SELECT COUNT(*) AS RecordsWithData
FROM (
    EXEC dbo.CPP_NMD_GetProcessDemandByYear @FinancialYear = '2025-26'
) AS Result
WHERE is_calculated = 1
```

---

## Related Stored Procedures

### Save/Update Operations
- **Update Endpoint**: `POST /task/plant-requirement/{financialYear}`
- **Service Method**: `ConsumptionServiceImpl.updateProcessDemand()`
- **Database Operation**: Direct JPA save to `CalculatedProcessDemand` table (no SP)

### Excel Import/Export
- **Export**: Uses `ConsumptionServiceImpl.exportConsumption()` - Java-based Excel generation
- **Import**: Uses `ConsumptionServiceImpl.importExcel()` - Java-based Excel parsing

---

## Performance Considerations

1. **Indexing**: Ensure indexes on:
   - `ProcessDemandMaster`: (process_plant_id, cpp_utility_id, is_active)
   - `CalculatedProcessDemand`: (financial_year, process_plant_id, cpp_utility_id)

2. **Data Volume**: Typical result set size: 50-200 rows per financial year

3. **Query Optimization**: LEFT JOIN ensures all master records are returned even without calculated data

---

## Migration Notes

If migrating from legacy SP to new SP:

1. **Frontend Changes**: Update API URL from `/plant-requirement/{plantId}/{financialYear}` to `/plant-requirement/{financialYear}`
2. **Data Mapping**: Update field names (e.g., `april` → `apr`, `cppUtilities` → `cppUtility`)
3. **Remove Plant Filter**: New SP returns all plants; filter on frontend if needed
4. **Handle is_calculated Flag**: Use this to show which rows have user-entered data

---

## Testing

### Test Query
```sql
-- Test with sample financial year
DECLARE @TestYear VARCHAR(10) = '2025-26'

EXEC dbo.CPP_NMD_GetProcessDemandByYear @FinancialYear = @TestYear

-- Verify master data exists
SELECT COUNT(*) AS MasterRecords 
FROM ProcessDemandMaster 
WHERE is_active = 1

-- Verify calculated data exists
SELECT COUNT(*) AS CalculatedRecords 
FROM CalculatedProcessDemand 
WHERE financial_year = @TestYear
```

---

## Troubleshooting

### Issue: No data returned
**Solution**: Check if `ProcessDemandMaster` has active records with `is_active = 1`

### Issue: All values are 0
**Solution**: This is expected if no data exists in `CalculatedProcessDemand` for the financial year

### Issue: Duplicate records
**Solution**: Check for duplicate entries in `ProcessDemandMaster` with same (process_plant_id, cpp_utility_id, cpp_plant_id) combination

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-03 | 1.0 | Initial documentation for CPP_NMD_GetProcessDemandByYear |
| 2026-02 | 1.1 | Added legacy SP documentation and migration notes |

---

## Contact
For questions or issues, contact the CPP development team.
