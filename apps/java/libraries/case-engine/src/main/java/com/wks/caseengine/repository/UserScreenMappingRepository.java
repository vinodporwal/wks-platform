package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.entity.UserScreenMapping;

@Repository
public interface UserScreenMappingRepository extends JpaRepository<UserScreenMapping, UUID>{

	@NativeQuery("SELECT Distinct ScreenCode"
			+ "  FROM [dbo].[UserScreenMapping] where UserId=:userId and PlantFKId=:plantId and VerticalFKId=:verticalId GROUP BY ScreenCode")
	List<String> findByVerticalFKIdAndPlantFKIdandUserId(@Param("verticalId") String verticalId, @Param("plantId") String plantId, @Param("userId") String userId);

	List<UserScreenMapping> findByUserIdAndPlantFKIdAndVerticalFKId(UUID userId, UUID plantId,
			UUID verticalId);
	
	@Modifying
	@Transactional
	@NativeQuery("DELETE FROM [UserScreenMapping] where UserId=:userId and PlantFKId=:plantFKId")
	void deleteAllByUserId(@Param("userId") String userId,@Param("plantFKId") String plantFKId);
	
	@NativeQuery("SELECT Distinct permissions"
			+ "  FROM [dbo].[UserScreenMapping] where UserId=:userId and PlantFKId=:plantId and VerticalFKId=:verticalId")
	List<String> findPermissionsByVerticalFKIdAndPlantFKIdandUserId(@Param("verticalId") String verticalId, @Param("plantId") String plantId, @Param("userId") String userId);



	List<UserScreenMapping> findByUserIdIn(List<UUID> userIds);


}
