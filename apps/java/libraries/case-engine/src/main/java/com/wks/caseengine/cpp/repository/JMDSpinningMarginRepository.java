package com.wks.caseengine.cpp.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.DummyEntity;

@Repository
@Transactional
public interface JMDSpinningMarginRepository extends JpaRepository<DummyEntity, Long> {

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPP_SpinningMargin SET " +
        "  Apr = :apr, May = :may, Jun = :jun, Jul = :jul, " +
        "  Aug = :aug, Sep = :sep, Oct = :oct, Nov = :nov, " +
        "  Dec = :dec, Jan = :jan, Feb = :feb, Mar = :mar, " +
        "  Remarks = :remarks, ModifiedOn = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateSpinningMargin(
            @Param("id")      UUID id,
            @Param("apr")     Double apr,
            @Param("may")     Double may,
            @Param("jun")     Double jun,
            @Param("jul")     Double jul,
            @Param("aug")     Double aug,
            @Param("sep")     Double sep,
            @Param("oct")     Double oct,
            @Param("nov")     Double nov,
            @Param("dec")     Double dec,
            @Param("jan")     Double jan,
            @Param("feb")     Double feb,
            @Param("mar")     Double mar,
            @Param("remarks") String remarks);
}
