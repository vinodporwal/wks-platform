package com.wks.caseengine.crude.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.crude.dto.NormBasisProjection;
import com.wks.caseengine.entity.DummyEntity;

@Repository
public interface NormBasisRepository extends JpaRepository<DummyEntity, Long> {
    
    @NativeQuery("EXEC CRUDE_GetConfiguration_Constant @plantId = :plantId, @aopYear = :aopYear")
List<NormBasisProjection> getAllNormBasis(
     @Param("plantId") UUID plantId,
     @Param("aopYear") String aopYear
);


@NativeQuery("EXEC CRUDE_GetPIMS_Throughput @plantId = :plantId, @aopYear = :aopYear")
List<NormBasisProjection> getPIMSThroughput(
 @Param("plantId") UUID plantId,
 @Param("aopYear") String aopYear
);

}

