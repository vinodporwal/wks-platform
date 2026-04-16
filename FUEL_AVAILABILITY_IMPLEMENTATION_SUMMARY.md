# JCB Fuel Availability Module - Implementation Summary

## Overview
Complete implementation of JCB Fuel Availability module for CPP (Combined Power Plant) with REST API endpoints, service layer, repository, entity, and database schema.

---

## 📁 Files Created

### 1. SQL Scripts
**Location:** `sql/fuel_availability_sql_changes.sql`

Contains:
- Table creation script for `CPPFuelAvailability`
- Indexes for performance optimization
- Stored procedures:
  - `usp_GetFuelAvailability` - Retrieve fuel data
  - `usp_SaveFuelAvailability` - Insert/Update fuel data
  - `usp_DeleteFuelAvailability` - Delete fuel data
- Sample data (commented out)

### 2. Java Entity
**Location:** `apps/java/libraries/case-engine/src/main/java/com/wks/caseengine/entity/CPPFuelAvailability.java`

JPA Entity with:
- UUID primary key
- Monthly fuel data fields (Apr-Mar)
- Audit fields (CreatedDate, UpdatedDate, CreatedBy, UpdatedBy)
- Lombok annotations for getters/setters

### 3. DTO (Data Transfer Object)
**Location:** `apps/java/libraries/case-engine/src/main/java/com/wks/caseengine/dto/FuelAvailabilityDto.java`

Simple POJO with:
- All entity fields
- Lombok annotations (@Data, @NoArgsConstructor, @AllArgsConstructor)

### 4. Repository
**Location:** `apps/java/libraries/case-engine/src/main/java/com/wks/caseengine/repository/FuelAvailabilityRepository.java`

JPA Repository with custom queries:
- `findByCppIdAndFinancialYear()` - Get all fuels for a CPP and year
- `findByCppIdAndFinancialYearAndFuelType()` - Filter by fuel type
- `findByCppIdAndFinancialYearAndFuelName()` - Get specific fuel

### 5. Service Interface
**Location:** `apps/java/libraries/case-engine/src/main/java/com/wks/caseengine/cpp/service/FuelAvailabilityService.java`

Methods:
- `getFuelAvailability()` - Retrieve fuel data
- `saveFuelAvailability()` - Save single record
- `saveFuelAvailabilityBulk()` - Save multiple records
- `deleteFuelAvailability()` - Delete record

### 6. Service Implementation
**Location:** `apps/java/libraries/case-engine/src/main/java/com/wks/caseengine/cpp/serviceimpl/FuelAvailabilityServiceImpl.java`

Features:
- Input validation
- Automatic upsert logic (insert or update)
- Bulk save support
- Entity-DTO conversion
- Transaction management

### 7. REST Controller
**Location:** `apps/java/services/case-engine-rest-api/src/main/java/com/wks/caseengine/rest/cpp/FuelAvailabilityController.java`

Endpoints:
- `GET /task/fuel-availability/{cppId}/{financialYear}?fuelType={optional}`
- `POST /task/fuel-availability/{cppId}/{financialYear}`
- `DELETE /task/fuel-availability/{id}`
- `GET /task/fuel-availability/export/{cppId}/{financialYear}?fuelType={optional}`

---

## 🗄️ Database Schema

### Table: CPPFuelAvailability

| Column | Type | Description |
|--------|------|-------------|
| Id | UNIQUEIDENTIFIER | Primary key |
| CPPId | UNIQUEIDENTIFIER | Foreign key to CPPPlant |
| FuelName | NVARCHAR(100) | Name of fuel (e.g., Natural Gas, Diesel) |
| FuelCategory | NVARCHAR(50) | Category (Gas, Liquid, Solid) |
| UOM | NVARCHAR(20) | Unit of measurement |
| Apr-Mar | DECIMAL(18,4) | Monthly fuel availability data |
| FinancialYear | NVARCHAR(10) | Financial year (e.g., 2025-26) |
| Remarks | NVARCHAR(500) | Additional notes |
| CreatedDate | DATETIME2 | Record creation timestamp |
| UpdatedDate | DATETIME2 | Last update timestamp |
| CreatedBy | NVARCHAR(100) | User who created |
| UpdatedBy | NVARCHAR(100) | User who last updated |

### Constraints
- Primary Key: `Id`
- Foreign Key: `CPPId` → `CPPPlant(Id)` with CASCADE DELETE
- Unique Constraint: `(CPPId, FuelName, FinancialYear)`

### Indexes
- `IX_CPPFuelAvailability_CPPId`
- `IX_CPPFuelAvailability_FinancialYear`
- `IX_CPPFuelAvailability_FuelCategory`
- `IX_CPPFuelAvailability_CPP_Year`

---

## 🔌 API Endpoints

### 1. Get Fuel Availability
```http
GET /task/fuel-availability/{cppId}/{financialYear}?fuelType={optional}
```

**Parameters:**
- `cppId` (UUID, required) - CPP Plant ID
- `financialYear` (String, required) - Financial year (e.g., "2025-26")
- `fuelType` (String, optional) - Filter by fuel category (Gas, Liquid, Solid)

**Response:**
```json
[
  {
    "id": "uuid",
    "cppId": "uuid",
    "fuelName": "Natural Gas",
    "fuelCategory": "Gas",
    "uom": "MMSCM",
    "apr": 100.5,
    "may": 105.2,
    "jun": 98.7,
    "jul": 110.3,
    "aug": 115.6,
    "sep": 108.9,
    "oct": 112.4,
    "nov": 107.8,
    "dec": 103.5,
    "jan": 109.2,
    "feb": 106.7,
    "mar": 111.3,
    "financialYear": "2025-26",
    "remarks": "Primary fuel source",
    "createdDate": "2025-03-19T12:00:00",
    "updatedDate": "2025-03-19T12:00:00",
    "createdBy": "user@example.com",
    "updatedBy": "user@example.com"
  }
]
```

### 2. Save Fuel Availability (Bulk)
```http
POST /task/fuel-availability/{cppId}/{financialYear}
```

**Request Body:**
```json
[
  {
    "id": "uuid (optional for update, null for insert)",
    "cppId": "uuid",
    "fuelName": "Natural Gas",
    "fuelCategory": "Gas",
    "uom": "MMSCM",
    "apr": 100.5,
    "may": 105.2,
    "jun": 98.7,
    "jul": 110.3,
    "aug": 115.6,
    "sep": 108.9,
    "oct": 112.4,
    "nov": 107.8,
    "dec": 103.5,
    "jan": 109.2,
    "feb": 106.7,
    "mar": 111.3,
    "financialYear": "2025-26",
    "remarks": "Primary fuel source"
  }
]
```

**Response:**
```json
{
  "code": 0,
  "message": "Fuel availability data saved successfully",
  "data": null
}
```

### 3. Delete Fuel Availability
```http
DELETE /task/fuel-availability/{id}
```

**Parameters:**
- `id` (UUID, required) - Fuel availability record ID

**Response:**
```json
{
  "code": 0,
  "message": "Fuel availability record deleted successfully",
  "data": null
}
```

### 4. Export to Excel
```http
GET /task/fuel-availability/export/{cppId}/{financialYear}?fuelType={optional}
```

**Parameters:**
- Same as Get endpoint

**Response:**
- Excel file download (.xlsx)

---

## 🚀 Deployment Steps

### 1. Database Changes
Execute the SQL script on the target database:
```bash
# Run this file on SQL Server
sql/fuel_availability_sql_changes.sql
```

### 2. Build Java Application
```bash
cd apps/java
mvn clean install
```

### 3. Deploy Application
- Deploy the updated WAR/JAR file to your application server
- Restart the application

### 4. Verify Deployment
Test the health endpoint:
```bash
curl -X GET http://localhost:8080/task/fuel-availability/{cppId}/2025-26
```

---

## 📝 Usage Examples

### Example 1: Get All Fuels for a CPP
```bash
curl -X GET "http://localhost:8080/task/fuel-availability/123e4567-e89b-12d3-a456-426614174000/2025-26"
```

### Example 2: Get Only Gas Fuels
```bash
curl -X GET "http://localhost:8080/task/fuel-availability/123e4567-e89b-12d3-a456-426614174000/2025-26?fuelType=Gas"
```

### Example 3: Save Fuel Data
```bash
curl -X POST "http://localhost:8080/task/fuel-availability/123e4567-e89b-12d3-a456-426614174000/2025-26" \
  -H "Content-Type: application/json" \
  -d '[{
    "cppId": "123e4567-e89b-12d3-a456-426614174000",
    "fuelName": "Natural Gas",
    "fuelCategory": "Gas",
    "uom": "MMSCM",
    "apr": 100.5,
    "may": 105.2,
    "financialYear": "2025-26"
  }]'
```

### Example 4: Delete Fuel Record
```bash
curl -X DELETE "http://localhost:8080/task/fuel-availability/123e4567-e89b-12d3-a456-426614174000"
```

---

## 🔍 Testing Checklist

- [ ] Database table created successfully
- [ ] Stored procedures created and tested
- [ ] Java application builds without errors
- [ ] GET endpoint returns data correctly
- [ ] POST endpoint saves data successfully
- [ ] DELETE endpoint removes records
- [ ] Export endpoint generates Excel file
- [ ] Fuel type filtering works
- [ ] Validation errors handled properly
- [ ] Duplicate fuel names prevented (unique constraint)

---

## 📊 Fuel Categories

Recommended fuel categories:
- **Gas**: Natural Gas, LNG, CNG
- **Liquid**: Diesel, Fuel Oil, Naphtha
- **Solid**: Coal, Biomass, Pet Coke

---

## 🔐 Security Considerations

- All endpoints should be secured with proper authentication
- Validate CPPId belongs to user's organization
- Implement role-based access control (RBAC)
- Audit all create/update/delete operations

---

## 📈 Future Enhancements

1. **Excel Import**: Add endpoint to import fuel data from Excel
2. **Validation Rules**: Add business rules for fuel quantity limits
3. **Historical Tracking**: Track changes over time
4. **Forecasting**: Add fuel consumption forecasting
5. **Alerts**: Notify when fuel availability is low
6. **Integration**: Connect with fuel procurement system

---

## 📞 Support

For issues or questions:
- Check logs in application server
- Review SQL error messages
- Verify database connectivity
- Check API request/response format

---

**Created:** 2026-03-19  
**Version:** 1.0  
**Status:** Ready for Testing
