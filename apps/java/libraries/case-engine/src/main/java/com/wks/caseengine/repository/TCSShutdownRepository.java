package com.wks.caseengine.repository;

import com.wks.caseengine.entity.TCSShutdown;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TCSShutdownRepository extends JpaRepository<TCSShutdown, UUID> {

    List<TCSShutdown> findByParticulates(String particulates);

    List<TCSShutdown> findByTentativeMonth(String tentativeMonth);

    List<TCSShutdown> findBySdTotalDurationInDays(Integer sdTotalDurationInDays);

    Optional<TCSShutdown> findByParticulatesAndTentativeMonth(String particulates, String tentativeMonth);
}
