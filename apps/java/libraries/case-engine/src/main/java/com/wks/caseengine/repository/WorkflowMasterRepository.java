package com.wks.caseengine.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.WorkflowMaster;

@Repository
public interface WorkflowMasterRepository extends JpaRepository<WorkflowMaster, UUID>{



    List<WorkflowMaster> findAllByVerticalFKId(UUID verticalId);

    /**
     * The global definition of a workflow — one that applies to every vertical.
     * A null {@code Vertical_FK_Id} is what marks a row as global; a row that
     * names a vertical is an override for that vertical alone.
     */
    List<WorkflowMaster> findAllByWorkflowIdAndVerticalFKIdIsNull(String workflowId);

    /** A vertical's override of {@code workflowId}, if it has one. */
    List<WorkflowMaster> findAllByWorkflowIdAndVerticalFKId(String workflowId, UUID verticalId);

    /** The global definition behind a case definition id. */
    List<WorkflowMaster> findAllByCaseDefIdAndVerticalFKIdIsNull(String caseDefId);

    /** Every global definition, whatever the workflow. */
    List<WorkflowMaster> findAllByVerticalFKIdIsNull();

}
