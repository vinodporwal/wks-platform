package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.VerticalScreenMapping;


@Repository
public interface VerticalScreenMappingRepository extends JpaRepository<VerticalScreenMapping,Long>{
	
	List<VerticalScreenMapping> findAllByVerticalFKIdOrderBySequence(@Param("verticalFKId") UUID verticalFKId);
	List<VerticalScreenMapping> findByScreenDisplayNameInAndVerticalFKIdOrderBySequence(List<String> screenDisplayName, UUID verticalFKId);

	@Query("SELECT v FROM VerticalScreenMapping v WHERE v.verticalFKId = :verticalFKId AND (v.screenCode IN :screens OR v.screenDisplayName IN :screens) ORDER BY v.sequence")
	List<VerticalScreenMapping> findByScreensAndVerticalFKId(@Param("screens") List<String> screens, @Param("verticalFKId") UUID verticalFKId);

}
