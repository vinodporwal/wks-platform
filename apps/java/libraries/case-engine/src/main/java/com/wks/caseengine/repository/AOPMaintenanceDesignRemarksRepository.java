package com.wks.caseengine.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.AOPMaintenanceDesignBasis;
import com.wks.caseengine.entity.AOPMaintenanceDesignRemarks;

@Repository
public interface AOPMaintenanceDesignRemarksRepository extends JpaRepository<AOPMaintenanceDesignRemarks,UUID>{
	
	@NativeQuery("SELECT * from AOPMaintenanceDesignRemarks where Plant_FK_Id = :plantId AND AOPYear=:year ")
	AOPMaintenanceDesignRemarks getData(@Param("plantId") UUID plantId,@Param("year") String year);

	
}



