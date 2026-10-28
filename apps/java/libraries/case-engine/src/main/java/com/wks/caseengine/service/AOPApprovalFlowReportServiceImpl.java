package com.wks.caseengine.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.wks.caseengine.entity.Plants;

import com.wks.caseengine.entity.Sites;

import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Service
public class AOPApprovalFlowReportServiceImpl implements AOPApprovalFlowReportService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;
    
    @Autowired
	private SiteRepository siteRepository;
    
    @Autowired
	private PlantsRepository plantsRepository;
    
    @Autowired
    @Qualifier("db2DataSource")
    private DataSource db2DataSource;
    
    @Transactional(transactionManager = "db2TransactionManager")
    public int executeDynamicUpdateProcedure(String procedureName, String plantId, String siteId,
			String finYear) {
		try {
			
			String callSql = "{call " + procedureName + "(?, ?, ?)}";

	        try (Connection connection = db2DataSource.getConnection();
	             CallableStatement stmt = connection.prepareCall(callSql)) {

	           
	            stmt.setString(1, siteId.toString()); 
	            stmt.setString(2, plantId);
	            stmt.setString(3, finYear); 

	            
	            int rowsAffected = stmt.executeUpdate();

	           
	            if (!connection.getAutoCommit()) {
	                connection.commit();
	            }

	            return rowsAffected;

	        } catch (SQLException e) {
	            e.printStackTrace();
	            return 0;
	        }

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
    @Override
    @Transactional(transactionManager = "db2TransactionManager")
    public AOPMessageVM loadAOPApprovalFlowReportDataPlantwise(String plantId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = "Load_AOPApprovalFlowReportData_Plantwise";
			
			Integer result=  executeDynamicUpdateProcedure(storedProcedure, plantId, site.getId().toString(),year);
			
			aopMessageVM.setCode(200);
	        aopMessageVM.setMessage("SP Executed successfully");
	        aopMessageVM.setData(result);
	        return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return aopMessageVM;
	}


}
