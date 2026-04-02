package com.wks.caseengine.repository;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.DummyEntity;

@Repository
public interface AnnualConfigPrizeRepository extends JpaRepository<DummyEntity, Long> {
      

    @Query(value = "Update Config_Annual_Price set Price = :price, Remarks = :remarks where Id = :id", nativeQuery = true)
    void updateAnnualConfigPrize(@Param("price") Double price, @Param("remarks") String remarks, @Param("id") UUID id);
}
