package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wks.caseengine.entity.Sites;

@Repository
public interface SiteRepository extends JpaRepository<Sites, UUID>{
	
	@NativeQuery("select sites.Id, sites.Name, sites.DisplayName, " +
	        "plants.Id, plants.Name, plants.DisplayName, plants.Site_FK_Id " +
	        "  from  Sites sites join   Plants plants " +
	        "   on sites.id = plants.Site_FK_Id")
	List<Object[]> getAllSitesAndPlants();
	
	@NativeQuery("SELECT s.Id AS siteId, s.Name AS siteName, s.DisplayName AS siteDisplayName, " +
            "p.Id AS plantId, p.Name AS plantName, p.DisplayName AS plantDisplayName, " +
            "p.Site_FK_Id AS siteFkId " +
            "FROM Sites s " +
            "JOIN Plants p ON s.id = p.Site_FK_Id")
		List<Object[]> getPlantAndSite();


}
