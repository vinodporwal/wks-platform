package com.wks.caseengine.repository;

import com.wks.caseengine.entity.CapexPIOPlanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CapexPIOPlanTransactionRepository extends JpaRepository<CapexPIOPlanTransaction, UUID> {
}