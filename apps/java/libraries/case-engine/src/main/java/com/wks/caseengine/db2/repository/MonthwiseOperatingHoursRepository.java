package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.MonthwiseOperatingHours;


@Repository
public interface MonthwiseOperatingHoursRepository extends JpaRepository<MonthwiseOperatingHours, UUID> {

    List<MonthwiseOperatingHours> findByPlantFkIdAndYear(UUID plantFkId, String year);
}

