package com.wks.caseengine.rest.db2.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.db2.entity.Users;
import com.wks.caseengine.rest.model.UserDTO;

@Repository
public interface UsersRepository  extends JpaRepository<Users, String> {
    Users findByEmailId(String emailId);

    List<Users> findByEmailIn(List<String> emails);  

    @Query("SELECT new com.wks.caseengine.rest.model.UserDTO(u.userId, u.emailId) FROM Users u")
    List<UserDTO> findAllUserIdAndEmail();  

    @Query("""
        SELECT new com.wks.caseengine.rest.model.UserDTO(u.userId, u.emailId)
        FROM Users u """)
    Page<UserDTO> findTopUsers(Pageable pageable);

    @Query("""
        SELECT new com.wks.caseengine.rest.model.UserDTO(u.userId, u.emailId) FROM Users u
        WHERE LOWER(u.userId) LIKE LOWER(CONCAT(:search, '%'))
        OR LOWER(u.emailId) LIKE LOWER(CONCAT(:search, '%'))
        """)
    Page<UserDTO> searchUsers(@Param("search") String search, Pageable pageable);
}
