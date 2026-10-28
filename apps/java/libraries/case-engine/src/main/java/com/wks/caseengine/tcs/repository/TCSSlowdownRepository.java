package com.wks.caseengine.tcs.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
//import java.util.List;
//import java.util.Optional;

import com.wks.caseengine.tcs.entity.TCSSlowdown;

@Repository
public interface TCSSlowdownRepository extends JpaRepository<TCSSlowdown, UUID> {

    // List<TCSSlowdown> findByParticulates(String particulates);

    // List<TCSSlowdown> findByTentativeMonth(String tentativeMonth);

    // List<TCSSlowdown> findByTentativeDurationInDays(Integer tentativeDurationInDays);

    // Optional<TCSSlowdown> findByParticulatesAndTentativeMonth(String particulates, String tentativeMonth);

    /**
     * Retrieves all editable field keys for the specified roles and screen.
     * 
     * @param roleNames List of role names to query for (e.g., 'plant_manager', 'cts_admin', 'cts_head')
     * @param screenName The screen name (e.g., 'tcs-slowdown')
     * @return List of FieldKey values that are editable for the given roles and screen
     */
    @Query(value = """
        SELECT FieldKey
        FROM Role_Field_Access
        WHERE RoleName IN :roleNames
          AND ScreenName = :screenName
          AND IsEditable = 1
        """, nativeQuery = true)
    List<String> getEditableFieldsByRolesAndScreen(
        @Param("roleNames") List<String> roleNames,
        @Param("screenName") String screenName
    );
    
}



