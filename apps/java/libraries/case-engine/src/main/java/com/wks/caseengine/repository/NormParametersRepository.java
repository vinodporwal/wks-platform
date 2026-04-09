package com.wks.caseengine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.NormParameters;

@Repository
public interface NormParametersRepository extends JpaRepository<NormParameters, UUID> {

	List<NormParameters> findAllByType(String type);

	@NativeQuery("SELECT * FROM vwScrnPEConfigurationGrades WHERE Plant_FK_Id = :plantId")
	List<NormParameters> getAllGrades(@Param("plantId") String plantId);
	
	@NativeQuery("SELECT Id FROM NormParameters WHERE Name = :name AND Plant_FK_Id = :plantId")
	UUID findNormParameterIdByNameAndPlant(@Param("name") String name, @Param("plantId") UUID plantId);
	
	@NativeQuery("SELECT Id FROM NormParameters WHERE DisplayName = :name AND Plant_FK_Id = :plantId")
	UUID findNormParameterIdByDisplayNameAndPlant(@Param("name") String name, @Param("plantId") UUID plantId);

    Optional<NormParameters> findFirstOneByName(String string);

    Optional<NormParameters> findFirstOneByNameAndPlantFkId(String string, UUID plantId);
    
    Optional<NormParameters> findByNameAndPlantFkId(String string, UUID plantId);

    Optional<NormParameters> findFirstNameByDisplayNameAndPlantFkId(String normParameterDisplayName, UUID plantFKId);
    
    List<NormParameters> findByPlantFkId(UUID plantFKId);
    
    @NativeQuery("SELECT DisplayName FROM NormParameters WHERE Id = :id")
	String findNormParameterIdByGrade(@Param("id") UUID id);
    
    @NativeQuery("SELECT Name FROM NormParameters WHERE Id = :id")
	String findNormParameterName(@Param("id") UUID id);
    
    @NativeQuery("SELECT Id FROM NormParameters WHERE Name = :name and Plant_FK_Id = :plantId")
	List<UUID> findNormParameterIds(@Param("name") String name,@Param("plantId") UUID plantId);

    @NativeQuery("SELECT Id FROM NormParameters WHERE Plant_FK_Id = :plantFKId AND Name = :name AND Type = :type")
    UUID findIdByPlantFkIdAndNameAndType(@Param("plantFKId") UUID plantFKId,
                                         @Param("name") String name,
                                         @Param("type") String type);
    
    @NativeQuery("SELECT * FROM NormParameters " +
    	              "WHERE Plant_FK_Id = :plantFkId " +
    	              "AND DisplayName = :displayName " +
    	              "AND NormType_FK_Id = :normTypeFkId " +
    	              "AND DependantAttributeId = :dependantAttributeId")
    	    List<NormParameters> findByPlantAndDisplayNameAndNormTypeAndDependantAttribute(
    	        @Param("plantFkId") UUID plantFkId,
    	        @Param("displayName") String displayName,
    	        @Param("normTypeFkId") int normTypeFkId,
    	        @Param("dependantAttributeId") String dependantAttributeId
    	    );


}
