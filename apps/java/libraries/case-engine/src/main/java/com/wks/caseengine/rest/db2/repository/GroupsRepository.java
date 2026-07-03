package com.wks.caseengine.rest.db2.repository;

import com.wks.caseengine.rest.db2.entity.Groups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupsRepository extends JpaRepository<Groups, String> {
    
    Groups findByGroupId(String groupId);

}
