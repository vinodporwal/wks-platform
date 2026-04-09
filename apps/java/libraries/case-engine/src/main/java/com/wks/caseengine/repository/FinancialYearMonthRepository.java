package com.wks.caseengine.repository;

import com.wks.caseengine.entity.FinancialYearMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialYearMonthRepository extends JpaRepository<FinancialYearMonth, UUID> {

    Optional<FinancialYearMonth> findByMonthAndYear(Integer month, Integer year);

    List<FinancialYearMonth> findByYear(Integer year);

    @NativeQuery("""
        SELECT Id
        FROM FinancialYearMonth
        WHERE Year = :year AND Month = :month
        """)
    UUID findFinancialMonthId(
            @Param("year") int year,
            @Param("month") int month
    );

    @NativeQuery("""
        SELECT Month, Id from FinancialYearMonth
        WHERE Year = :startYear and Month between 4 and 12 or 
              Year = :endYear and Month between 1 and 3  """)
          List<Object[]> findFinancialYearMonths(
            @Param("startYear") int startYear,
            @Param("endYear") int endYear
    );


}