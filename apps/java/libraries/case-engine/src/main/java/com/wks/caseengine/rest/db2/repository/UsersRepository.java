package com.wks.caseengine.rest.db2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.db2.entity.Users;
import com.wks.caseengine.rest.model.UserDTO;

@Repository
public interface UsersRepository  extends JpaRepository<Users, String> {
    Users findByEmailId(String emailId);

    @Query("SELECT new com.wks.caseengine.rest.model.UserDTO(u.userId, u.emailId) FROM User u")
    List<UserDTO> findAllUserIdAndEmail();  
}
