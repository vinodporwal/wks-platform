package com.wks.caseengine.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import com.wks.caseengine.entity.NormAttributeTransactionReceipe;

@Repository
public interface NormAttributeTransactionReceipeRepository extends JpaRepository<NormAttributeTransactionReceipe, UUID> {
	
@NativeQuery("SELECT * FROM NormAttributeTransactionReceipe NATR WHERE NATR.AOPYear= :year AND NATR.Plant_FK_ID= :plantUUId \r\n"
			+ " AND NATR.Grade_FK_ID= :gradeUUId AND  NATR.Reciepe_FK_ID= :reciepeUUId ")
	NormAttributeTransactionReceipe findIdByFilters(@Param("year") String year, @Param("plantUUId") UUID plantUUId,
			@Param("gradeUUId") UUID gradeUUId, @Param("reciepeUUId") UUID reciepeUUId);
}
