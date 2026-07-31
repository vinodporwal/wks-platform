package com.wks.caseengine.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.Workflow;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID>{

    

    List<Workflow> findAllByYearAndPlantFKIdAndSiteFKIdAndVerticalFKId(String year, UUID fromString, UUID fromString2,
            UUID fromString3);

    /**
     * Active (non-deleted) workflow instances for a plant + year. Used to
     * enforce the single-active-workflow-per-(plant,year) invariant.
     */
    List<Workflow> findAllByYearAndPlantFKIdAndIsDeletedFalse(String year, UUID plantFKId);

    /** All active (non-deleted) workflow instances for a given case definition. */
    List<Workflow> findAllByCaseDefIdAndIsDeletedFalse(String caseDefId);



}
