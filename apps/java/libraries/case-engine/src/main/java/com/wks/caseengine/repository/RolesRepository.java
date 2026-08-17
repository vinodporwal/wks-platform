package com.wks.caseengine.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.Roles;

@Repository
public interface RolesRepository extends JpaRepository<Roles, UUID> {

	Optional<Roles> findByName(String name);

	boolean existsByName(String name);

	void deleteByName(String name);
}
