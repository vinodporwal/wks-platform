package com.wks.caseengine.tcs.serviceimpl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.camunda.community.rest.client.dto.TaskDto;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wks.bpm.engine.client.VariablesMapper;
import com.wks.bpm.engine.client.facade.BpmEngineClientFacade;
import com.wks.bpm.engine.model.spi.ProcessInstance;
import com.wks.bpm.engine.model.spi.ProcessVariable;
import com.wks.bpm.engine.model.spi.Task;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.UserScreenMapping;
import com.wks.caseengine.exception.RestResourceNotFoundException;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.UserScreenMappingRepository;
import com.wks.caseengine.service.KeycloakUserService;
import com.wks.caseengine.service.PlantService;
import com.wks.caseengine.service.VerticalsService;
import com.wks.caseengine.tcs.dto.camundadto.PlantSubmissionAuditTrailDTO;
import com.wks.caseengine.tcs.dto.camundadto.PlantSubmissionAuditTrailProjection;
import com.wks.caseengine.tcs.enums.Roles;
import com.wks.caseengine.tcs.enums.Status;
import com.wks.caseengine.tcs.repository.tcsworkflow.TCSAuditTrailRepository;
import com.wks.caseengine.tcs.service.TCSWorkFlowService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;

@Service
public class TCSWorkFlowServiceImpl implements TCSWorkFlowService {  

    // define constant for process definition key
    private static final String PROCESS_DEFINITION_KEY = "TCS_APPROVAL_PROCESS";

    private static final String SUBMIT_PLANT_TASK_DEFINITION_KEY = "SubmitPlantData";

    private static final String CTS_TECH_SUBMISSION_TASK_DEFINITION_KEY = "CTS_Tech_Approval";

    //private static final String EBS_APPROVAL_TASK_DEFINITION_KEY = "EPS_Approval"; AOP_Approval

    // stage 2
    private static final String AOM_APPROVAL_TASK_DEFINITION_KEY = "AOM_Approval";

    // stage 3
    private static final String CTS_APPROVAL_TASK_DEFINITION_KEY = "CTS_APPROVAL";

    // stage 4
    private static final String EPS_APPROVAL_TASK_DEFINITION_KEY = "EPS_APPROVAL"; 

  // stage 5
    private static final String CLUSTER_HEAD_APPROVAL_TASK_DEFINITION_KEY = "Cluster_Head_APPROVAL";

    //approval Status variables
    private static final String AOM_SUBMISSION_VARIABLE_NAME = "aom_approved";
    private static final String CTS_SUBMISSION_VARIABLE_NAME = "cts_approved";
    private static final String EPS_SUBMISSION_VARIABLE_NAME = "eps_approved";
    private static final String CLUSTER_HEAD_APPROVAL_VARIABLE_NAME = "cluster_head_approved";

    private static final String TOTAL_PLANTS_VARIABLE_NAME = "total_plants";
    private static final String APPROVED_PLANTS_VARIABLE_NAME = "approved_plants";
    private static final String ALL_PLANTS_APPROVED_VARIABLE_NAME = "all_plants_approved";

    

    // for email notification

    private static final String screenCode = "menu.tcsinput";
    private static final String vertical = "CRUDE";

    @Value("${camunda.process.id.tcs.output.workflow}")
    private String tcsOutputWorkflowProcessId;
    
    @Autowired
    private BpmEngineClientFacade processEngineClientFacade;

    @Autowired
    private PlantService plantService;

    @Autowired
    private VerticalsService verticalsService;

    @Autowired
    private TCSAuditTrailRepository tcsAuditTrailRepository;

    @Autowired
	private VariablesMapper<Map<String, VariableValueDto>> c7VariablesMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KeycloakUserService keycloakUserService;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private UserScreenMappingRepository userScreenMappingRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public String generateBusinessKey(String verticalId, String siteId, String finacialYear) {  

        VerticalsDTO vertical = verticalsService.getVerticalById(verticalId);

        if(vertical == null) {  
            throw new RestResourceNotFoundException("Vertical not found");
        }

        return vertical.getName() + "-" + siteId + "-" + finacialYear;
    }

    @Override
    public List<String> getPlantList(UUID verticalId, UUID siteId) {  
  
        //    fetch the plants for given vertical if atleast one user has permission  and plant is mapped to menu.tcs in UserScreenMapping  (8)
         //  List<Plants> plants = plantService.findUniqueNamesPlantsByVerticalAndSite(verticalId, siteId, screenCode);

           // fetch all the plants under crude and given site (37)
         List<Plants> plants = plantService.getPlantListForWorkflow(verticalId, siteId);
        
        
            List<String> plantList1 = plants.stream().map(Plants::getDisplayName).toList();

        System.out.println("total plants: " + plantList1.size());
                System.out.println("plantList1: " + plantList1);

        // hardcoded to filter only cdu-1 and cdu-2 plants
        List<String> plantList = plantList1.stream().filter(plantName -> plantName.equals("CDU-1") || plantName.equals("CDU-2")).toList();

        return plantList;

    }



    @Override
    public void startProcess(String verticalId, String siteId, String finacialYear) {

        VerticalsDTO vertical = verticalsService.getVerticalById(verticalId);

        if(vertical == null) {
            throw new RestResourceNotFoundException("Vertical not found");
        }
        
        String key = tcsOutputWorkflowProcessId;
        // business key = siteId-finacialYear
        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);


    List<String> plantList = getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId));


    System.out.println("plantList: " + plantList);


        Map<String, Boolean> submissionStatusMap = new HashMap<>();
        Map<String, Boolean> ctsTechSubmissionStatusMap = new HashMap<>();
        Map<String, Boolean> approvalStatusMap = new HashMap<>();
        Map<String, Integer> plantCountMap = new HashMap<>();


        for(String plantName : plantList) {
            System.out.println("putting submissionStatusMap for plantName: " + plantName);
            submissionStatusMap.put(plantName, false);
            ctsTechSubmissionStatusMap.put(plantName, false);
        }

        approvalStatusMap.put(AOM_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(CTS_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(EPS_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(CLUSTER_HEAD_APPROVAL_VARIABLE_NAME, false);

        plantCountMap.put(TOTAL_PLANTS_VARIABLE_NAME, plantList.size());
        plantCountMap.put(APPROVED_PLANTS_VARIABLE_NAME, 0);
        
        List<ProcessVariable> processVariables = new ArrayList<>();

		ObjectMapper objectMapper = new ObjectMapper();

String submissionStatusJson = null;
String ctsTechSubmissionStatusJson = null;
String plantListJson = null;
String approvalStatusJson = null;
String plantCountJson = null;
try {
	submissionStatusJson = objectMapper.writeValueAsString( submissionStatusMap );
    ctsTechSubmissionStatusJson = objectMapper.writeValueAsString( ctsTechSubmissionStatusMap );
    plantListJson = objectMapper.writeValueAsString(plantList);
    approvalStatusJson = objectMapper.writeValueAsString( approvalStatusMap );
    plantCountJson = objectMapper.writeValueAsString( plantCountMap );

    // totalPlantsJson = objectMapper.writeValueAsString( totalPlants );
    // approvedPlantsJson = objectMapper.writeValueAsString( approvedPlants );
    // allPlantsApprovedJson = objectMapper.writeValueAsString( allPlantsApproved );
} catch (JsonProcessingException e) {

	throw new RestResourceNotFoundException("Error converting submissionStatusDTO to JSON: " + e.getMessage());
}


	ProcessVariable submissionStatus = ProcessVariable.builder()
    .name("submissionStatus")
    .value(submissionStatusJson)   // String JSON
    .type("Json")
    .build();

    ProcessVariable ctsTechSubmissionStatus = ProcessVariable.builder()
    .name("ctsTechSubmissionStatus")
    .value(ctsTechSubmissionStatusJson)   // String JSON
    .type("Json")
    .build();

    ProcessVariable approvalStatus = ProcessVariable.builder()
    .name("approvalStatus")
    .value(approvalStatusJson)   // String JSON
    .type("Json")
    .build();

ProcessVariable plantListVariable = ProcessVariable.builder()
    .name("plantList")
    // .value(plantListJson)          // String JSON array
    // .type("Json")
	.value(plantListJson)
     .type("Object")
	 .valueInfo(Map.of(     // Add valueInfo metadata
        "objectTypeName", "java.util.ArrayList",
        "serializationDataFormat", "application/json"
    ))
    .build();

// adding new process variable for exclusion gate way 
ProcessVariable approvedVariable = ProcessVariable.builder()
    .name("approved")
    .value(false)
    .type("Boolean")
    .build();

  ProcessVariable plantCountVariable = ProcessVariable.builder()
    .name("plantCount")
    .value(plantCountJson)
    .type("Json")
    .build();

		processVariables.add(submissionStatus);
		processVariables.add(ctsTechSubmissionStatus);
		processVariables.add(plantListVariable);
        processVariables.add(approvalStatus);

        processVariables.add(plantCountVariable);

        processVariables.add(approvedVariable);

		ProcessInstance processInstance = processEngineClientFacade.startProcess(key, Optional.ofNullable(businessKey), processVariables);
    }

   

    @Override
    public String deleteProcess(String verticalId, String siteId, String finacialYear) {

       String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

    ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

		if(processInstances.length == 0) {
			throw new RestResourceNotFoundException("No process instance found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
		}

		processEngineClientFacade.deleteProcessInstance(processInstances[0].getId());

        return "Process deleted successfully";


    }
    
    @Override
    public String resetAuditTrail(String businessKey) {
        
        tcsAuditTrailRepository.deleteAuditTrailByBusinessKey(businessKey);

        return "Audit trail reset successfully";
        
        
    }

    public Date getISTDateTime() {  

        ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        Date date = Date.from(istTime.toInstant());

        return date;

    }

    @Override
    public void resetProcessVariables(String businessKey) {

        ObjectMapper objectMapper = new ObjectMapper();

        ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(
                Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

        if (processInstances.length == 0) {
            throw new RuntimeException("No process instance found for business key: " + businessKey
                    + " and process definition key: " + PROCESS_DEFINITION_KEY);
        }

        if (processInstances.length > 1) {
            throw new RuntimeException("Multiple process instances found for business key: " + businessKey
                    + " and process definition key: " + PROCESS_DEFINITION_KEY);
        }

        ProcessInstance processInstance = processInstances[0];

        // --- Reset submissionStatus: set all plant entries back to false ---
        List<ProcessVariable> submissionStatusVars = Arrays.stream(
                processEngineClientFacade.findVariables(processInstance.getId()))
                .filter(v -> v.getName().equals("submissionStatus"))
                .toList();

        if (submissionStatusVars.isEmpty()) {
            throw new RuntimeException(
                    "submissionStatus variable not found for process instance: " + processInstance.getId());
        }

        if (submissionStatusVars.size() > 1) {
            throw new RuntimeException(
                    "Multiple submissionStatus variables found for process instance: " + processInstance.getId());
        }

        ProcessVariable submissionStatusVar = submissionStatusVars.get(0);

        try {
            Map<String, Boolean> submissionStatusMap = objectMapper.readValue(
                    submissionStatusVar.getValue().toString(),
                    new TypeReference<Map<String, Boolean>>() {});
            submissionStatusMap.replaceAll((plant, status) -> false);
            submissionStatusVar.setValue(objectMapper.writeValueAsString(submissionStatusMap));
        } catch (IOException e) {
            throw new RuntimeException("Error resetting submissionStatus variable: " + e.getMessage());
        }

        Map<String, VariableValueDto> submissionVarsMap = c7VariablesMapper.toEngineFormat(submissionStatusVars);
        VariableValueDto submissionStatusDto = submissionVarsMap.get("submissionStatus");
        processEngineClientFacade.updateProcessVariable(processInstance.getId(), "submissionStatus", submissionStatusDto);

        // --- Reset ctsTechSubmissionStatus: set all plant entries back to false ---
       
        List<ProcessVariable> ctsTechSubmissionStatusVars = Arrays.stream(
            processEngineClientFacade.findVariables(processInstance.getId()))
            .filter(v -> v.getName().equals("ctsTechSubmissionStatus"))
            .toList();

    if (ctsTechSubmissionStatusVars.isEmpty()) {
        throw new RuntimeException(
                "ctsTechSubmissionStatus variable not found for process instance: " + processInstance.getId());
    }

    if (ctsTechSubmissionStatusVars.size() > 1) {
        throw new RuntimeException(
                "Multiple ctsTechSubmissionStatus variables found for process instance: " + processInstance.getId());
    }

    ProcessVariable ctsTechSubmissionStatusVar = ctsTechSubmissionStatusVars.get(0);

    try {
        Map<String, Boolean> ctsTechSubmissionStatusMap = objectMapper.readValue(
                ctsTechSubmissionStatusVar.getValue().toString(),
                new TypeReference<Map<String, Boolean>>() {});
        ctsTechSubmissionStatusMap.replaceAll((plant, status) -> false);
        ctsTechSubmissionStatusVar.setValue(objectMapper.writeValueAsString(ctsTechSubmissionStatusMap));
    } catch (IOException e) {
        throw new RuntimeException("Error resetting ctsTechSubmissionStatus variable: " + e.getMessage());
    }

    Map<String, VariableValueDto> ctsTechSubmissionStatusVarsMap = c7VariablesMapper.toEngineFormat(ctsTechSubmissionStatusVars);
    VariableValueDto ctsTechSubmissionStatusDto = ctsTechSubmissionStatusVarsMap.get("ctsTechSubmissionStatus");
    processEngineClientFacade.updateProcessVariable(processInstance.getId(), "ctsTechSubmissionStatus", ctsTechSubmissionStatusDto);

        // --- Reset approvalStatus: restore all approval flags to false ---
        Map<String, Boolean> approvalStatusMap = new HashMap<>();
        
        approvalStatusMap.put(AOM_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(CTS_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(EPS_SUBMISSION_VARIABLE_NAME, false);
        approvalStatusMap.put(CLUSTER_HEAD_APPROVAL_VARIABLE_NAME, false);

        String approvalStatusJson;
        try {
            approvalStatusJson = objectMapper.writeValueAsString(approvalStatusMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing approvalStatus variable: " + e.getMessage());
        }

        ProcessVariable approvalStatusVar = ProcessVariable.builder()
                .name("approvalStatus")
                .value(approvalStatusJson)
                .type("Json")
                .build();

        Map<String, VariableValueDto> approvalVarsMap = c7VariablesMapper.toEngineFormat(List.of(approvalStatusVar));
        VariableValueDto approvalStatusDto = approvalVarsMap.get("approvalStatus");
        processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", approvalStatusDto);
    }

    public void dataValidation(String siteId, String finacialYear, String verticalId) {  

       
        if(siteId == null || siteId.isEmpty()) {  
            
            throw new RuntimeException("Site id is required");
        }
    
        if(finacialYear == null || finacialYear.isEmpty()) {   
            
            throw new RuntimeException("Financial year is required");
        }


        if(verticalId == null || verticalId.isEmpty()) {
            throw new RuntimeException("Vertical id is required");
        }

        VerticalsDTO vertical = verticalsService.getVerticalById(verticalId);

        if(vertical == null) {
            throw new RestResourceNotFoundException("Vertical not found");
        }


        
    }

    public List<TaskDto> getTasks(String businessKey) {    

        // get tasks for given business key and process definition key
        List<TaskDto> tasks = processEngineClientFacade.findTasksByBusinessKeyAndProcessDefinitionKey(Optional.ofNullable(businessKey), Optional.ofNullable(PROCESS_DEFINITION_KEY));

        if(tasks.isEmpty()) {  
            throw new RuntimeException("No task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        }

        return tasks;
    }

    public ProcessInstance getProcessInstance(String businessKey) {      

         // get process Instance for given business key and process definition key
         ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

         if(processInstances.length == 0) {
             throw new RuntimeException("No process instance found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
         }
 
         if(processInstances.length > 1) {
             throw new RuntimeException("Multiple process instances found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
         }
 
         ProcessInstance processInstance = processInstances[0];
         return processInstance;
    }

    public void createAuditTrail(PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String type, String plantName, String status, String businessKey) {    



    }

    public String plantNamesFormat(List<String> plantList) {
        return plantList.stream()
                .collect(Collectors.joining(", "));
    }



    @Override
        public void completePlantSubmissionTask(String plantName, String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {
        
            String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

            if(plantName == null || plantName.isEmpty()) {  
                throw new RuntimeException("Plant name is required");
            }

            dataValidation( siteId, finacialYear, verticalId);

              String businessKey =  generateBusinessKey(verticalId, siteId, finacialYear);

        ObjectMapper objectMapper = new ObjectMapper();

        
   List<TaskDto> tasks = getTasks(businessKey);

        ProcessInstance processInstance = getProcessInstance(businessKey);
// **************   // variable update and audit trail logic for re-submission *******************

     int totalSubmissionTasks =   tasks.stream().filter(t -> SUBMIT_PLANT_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey())).toList().size();

     System.out.println("completePlantSubmissionTask totalSubmissionTasks: " + totalSubmissionTasks);

          if(totalSubmissionTasks == 0) {  

            List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("submissionStatus")).toList();
            if(submissionStatusVariables.isEmpty()) {
                throw new RuntimeException("No submission status variables found for given process instance");
            }
            if(submissionStatusVariables.size() > 1) {
                throw new RuntimeException("Multiple submission status variables found for given process instance");
            }
            updatesubmissionStatusVariable(submissionStatusVariables, plantName, objectMapper, true);

            Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);

            // get variable with name "submissionStatus"
            VariableValueDto submissionStatusVariable = variablesMap.get("submissionStatus");
    
            processEngineClientFacade.updateProcessVariable(processInstance.getId(), "submissionStatus", submissionStatusVariable);
    

             plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
           //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
          

            System.out.println("submissionDateTime: " + plantSubmissionAuditTrailDTO.getSubmissionDateTime());

            plantSubmissionAuditTrailDTO.setType("PLANT");
            plantSubmissionAuditTrailDTO.setPlantName(plantName);
            plantSubmissionAuditTrailDTO.setPlantStatus(Status.PENDING.name());
         //   plantSubmissionAuditTrailDTO.setStatus("PENDING");
         plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
           

            tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

         return;

        }

     // **************    finished audit trail logic for re-submission *******************

      List<TaskDto> taskForPlant = tasks.stream()
        .filter(t -> SUBMIT_PLANT_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
        .toList();

        if(taskForPlant.isEmpty()) {  
            throw new RuntimeException("No Plant Submission task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        }
        
     // compelete one of the pending multi-instance task
      TaskDto taskToComplete = taskForPlant.get(0);

      System.out.println("taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

      // update process variable corresponding to given Plant 
      List<ProcessVariable> processVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("submissionStatus")).toList();

      if(processVariables.isEmpty()) { 
        throw new RuntimeException("No process variables found for given process instance");
      }

      if(processVariables.size() > 1) { 
        throw new RuntimeException("Multiple process variables found for given process instance");
      }
     
          updatesubmissionStatusVariable(processVariables, plantName, objectMapper, true);
    
      System.out.println("processVariables: " + processVariables);

      

      processEngineClientFacade.complete(taskToComplete.getId(), processVariables);

      // code for audit trail   

      DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

           plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
      //    plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());

          System.out.println("submissionDateTime: " + plantSubmissionAuditTrailDTO.getSubmissionDateTime());

          plantSubmissionAuditTrailDTO.setType("PLANT");
          plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
          plantSubmissionAuditTrailDTO.setPlantStatus(Status.PENDING.name());

          tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
    }


    @Override
    public void completeCTSTechTask(String plantName, String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {
    
        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

        if(plantName == null || plantName.isEmpty()) {  
            throw new RuntimeException("Plant name is required");
        }

        dataValidation( siteId, finacialYear, verticalId);

          String businessKey =  generateBusinessKey(verticalId, siteId, finacialYear);

    ObjectMapper objectMapper = new ObjectMapper();

    
List<TaskDto> tasks = getTasks(businessKey);

    ProcessInstance processInstance = getProcessInstance(businessKey);
// **************   // variable update and audit trail logic for re-submission *******************

 int totalSubmissionTasks =   tasks.stream().filter(t -> CTS_TECH_SUBMISSION_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey())).toList().size();

 System.out.println("completeCTSTechTask totalSubmissionTasks: " + totalSubmissionTasks);

      if(totalSubmissionTasks == 0) {  

        List<ProcessVariable> ctsTechSubmissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("ctsTechSubmissionStatus")).toList();
        if(ctsTechSubmissionStatusVariables.isEmpty()) {
            throw new RuntimeException("No submission status variables found for given process instance");
        }
        if(ctsTechSubmissionStatusVariables.size() > 1) {
            throw new RuntimeException("Multiple submission status variables found for given process instance");
        }
        updatesubmissionStatusVariable(ctsTechSubmissionStatusVariables, plantName, objectMapper, true);

        Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(ctsTechSubmissionStatusVariables);

        // get variable with name "submissionStatus"
        VariableValueDto ctsTechSubmissionStatusVariable = variablesMap.get("ctsTechSubmissionStatus");

        processEngineClientFacade.updateProcessVariable(processInstance.getId(), "ctsTechSubmissionStatus", ctsTechSubmissionStatusVariable);


         plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
       //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
      

        System.out.println("submissionDateTime: " + plantSubmissionAuditTrailDTO.getSubmissionDateTime());

        plantSubmissionAuditTrailDTO.setType("PLANT");
        plantSubmissionAuditTrailDTO.setPlantName(plantName);
        plantSubmissionAuditTrailDTO.setPlantStatus(Status.PENDING.name());
     //   plantSubmissionAuditTrailDTO.setStatus("PENDING");
     plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
       

        tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

     return;

    }

 // **************    finished audit trail logic for re-submission *******************

  List<TaskDto> taskForPlant = tasks.stream()
    .filter(t -> CTS_TECH_SUBMISSION_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
    .toList();

    if(taskForPlant.isEmpty()) {  
        throw new RuntimeException("No CTS Tech Submission task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
    }
    
 // compelete one of the pending multi-instance task
  TaskDto taskToComplete = taskForPlant.get(0);

  System.out.println("taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

  // update process variable corresponding to given Plant 
  List<ProcessVariable> processVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("ctsTechSubmissionStatus")).toList();

  if(processVariables.isEmpty()) { 
    throw new RuntimeException("No process variables found for given process instance");
  }

  if(processVariables.size() > 1) { 
    throw new RuntimeException("Multiple process variables found for given process instance");
  }
 
      updatesubmissionStatusVariable(processVariables, plantName, objectMapper, true);

  System.out.println("processVariables: " + processVariables);

  

  processEngineClientFacade.complete(taskToComplete.getId(), processVariables);

  // code for audit trail   

  DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

       plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
  //    plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());

      System.out.println("submissionDateTime: " + plantSubmissionAuditTrailDTO.getSubmissionDateTime());

      plantSubmissionAuditTrailDTO.setType("PLANT");
      plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
      plantSubmissionAuditTrailDTO.setPlantStatus(Status.PENDING.name());

      tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
}


@Override
public void AOMApproval(String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {  

    String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

  dataValidation(siteId, finacialYear, verticalId);

    String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

    ObjectMapper objectMapper = new ObjectMapper();


    ProcessInstance processInstance = getProcessInstance(businessKey);
    List<TaskDto> tasks = getTasks(businessKey);

  List<TaskDto> taskForPlant = tasks.stream()
    .filter(t -> AOM_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
    .toList();

    System.out.println("AOMApproval taskForPlant: " + taskForPlant);
    
 // compelete one of the pending multi-instance task
  if(taskForPlant.isEmpty()) {  
    throw new RuntimeException("No AOM Approval task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);

    // ************** variable update and audit trail for ebs re-submission *******************

//     List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();
//     List<ProcessVariable> plantCountVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("plantCount")).toList();
//     List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

   

//     if(submissionStatusVariables.isEmpty()) {
//         throw new RuntimeException("No submission status variables found for given process instance");
//     }

//     if(submissionStatusVariables.size() > 1) {
//         throw new RuntimeException("Multiple submission status variables found for given process instance");
//     }

//     if(plantCountVariables.isEmpty()) { 
//         throw new RuntimeException("No plant count variables found for given process instance");
//     }

//     if(plantCountVariables.size() > 1) { 
//         throw new RuntimeException("Multiple plant count variables found for given process instance");
//     }

//     updatesubmissionStatusVariable(submissionStatusVariables, AOM_SUBMISSION_VARIABLE_NAME, objectMapper, true);

//     // reset the approved plants count to 0 for ebs submission
//     updatePlantCountVariable(plantCountVariables, APPROVED_PLANTS_VARIABLE_NAME, objectMapper, false, true);

//     updateApprovedVariable(approvedVariables, true);

//     Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);

//     Map<String, VariableValueDto> plantCountVariablesMap = c7VariablesMapper.toEngineFormat(plantCountVariables);

//     Map<String, VariableValueDto> approvedVariablesMap = c7VariablesMapper.toEngineFormat(approvedVariables);

//     // get variable with name "submissionStatus"
//     VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");
//     VariableValueDto plantCountVariable = plantCountVariablesMap.get("plantCount");
//     VariableValueDto approvedVariable = approvedVariablesMap.get("approved");

//     processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);
//     processEngineClientFacade.updateProcessVariable(processInstance.getId(), "plantCount", plantCountVariable);
//     processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approved", approvedVariable);

//      plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
//   //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());

//     System.out.println("submissionDateTime: " + plantSubmissionAuditTrailDTO.getSubmissionDateTime());

//   plantSubmissionAuditTrailDTO.setType("AOM");
// //  plantSubmissionAuditTrailDTO.setStatus("PENDING");
// plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());

//   if(plantSubmissionAuditTrailDTO.getSiteId() == null ||  plantSubmissionAuditTrailDTO.getVerticalId() == null) {  
    
//     throw new RuntimeException(" missing Site id and vertical id in the request body");
//   }

//   //  get the comma seperated Plant Names from PlantList
//   String plantNames =  plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));

  

// tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);


//     return;

  }

  // ************** finished variable update and audit trail for ebs re-submission *******************



  if(taskForPlant.size() > 1) {  
    throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
  }

  TaskDto taskToComplete = taskForPlant.get(0);

  System.out.println(" AOM Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

  // update process variable corresponding to given Plant 
  List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

  List<ProcessVariable> plantCountVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("plantCount")).toList();

  List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();


  updatesubmissionStatusVariable(submissionStatusVariables, AOM_SUBMISSION_VARIABLE_NAME, objectMapper, true);

  updatePlantCountVariable(plantCountVariables, APPROVED_PLANTS_VARIABLE_NAME, objectMapper, true, true);

  updateApprovedVariable(approvedVariables, true);


  System.out.println("submissionStatusVariables: " + submissionStatusVariables);



  //processEngineClientFacade.complete(taskToComplete.getId(), submissionStatusVariables);

  processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), plantCountVariables.get(0), approvedVariables.get(0)));

//   processEngineClientFacade.complete(taskToComplete.getId(), );

  // *************** save audit trail for ebs approval history *************************

  plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
//  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
  plantSubmissionAuditTrailDTO.setType("AOM");
//  plantSubmissionAuditTrailDTO.setStatus("PENDING");
plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());

String plantNames =  plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));

 // tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

 tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);


}



@Override
public void AOMApproveReject(String plantName, String siteId, boolean approvalStatus, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {  



    String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

    dataValidation( siteId, finacialYear, verticalId);

    String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

    ObjectMapper objectMapper = new ObjectMapper();

    // get the process instance 

   ProcessInstance processInstance = getProcessInstance(businessKey);

    List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("submissionStatus")).toList();

    List<ProcessVariable> ctsTechSubmissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("ctsTechSubmissionStatus")).toList();

    List<ProcessVariable> plantCountVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("plantCount")).toList();

    if(submissionStatusVariables.isEmpty() || ctsTechSubmissionStatusVariables.isEmpty()) {
        throw new RuntimeException("No submission status variables found for given process instance");
    }

    if(submissionStatusVariables.size() > 1) { 
        throw new RuntimeException("Multiple submission status variables found for given process instance");
    }

    if(plantCountVariables.isEmpty()) {  
        throw new RuntimeException("No plant count variables found for given process instance");
    }

    if(plantCountVariables.size() > 1) {  
        throw new RuntimeException("Multiple plant count variables found for given process instance");
    }

    updatesubmissionStatusVariable(submissionStatusVariables, plantName, objectMapper, approvalStatus);

    updatesubmissionStatusVariable(ctsTechSubmissionStatusVariables, plantName, objectMapper, approvalStatus);

    updatePlantCountVariable(plantCountVariables, APPROVED_PLANTS_VARIABLE_NAME, objectMapper, approvalStatus, false);

    //  **************  update process variable  *******************
    Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);
    Map<String, VariableValueDto> plantCountVariablesMap = c7VariablesMapper.toEngineFormat(plantCountVariables);
    Map<String, VariableValueDto> ctsTechSubmissionStatusVariablesMap = c7VariablesMapper.toEngineFormat(ctsTechSubmissionStatusVariables);

    // get variable with name "submissionStatus"
    VariableValueDto submissionStatusVariable = variablesMap.get("submissionStatus");
    VariableValueDto plantCountVariable = plantCountVariablesMap.get("plantCount");
    VariableValueDto ctsTechSubmissionStatusVariable = ctsTechSubmissionStatusVariablesMap.get("ctsTechSubmissionStatus");

    
    processEngineClientFacade.updateProcessVariable(processInstance.getId(), "submissionStatus", submissionStatusVariable);
    processEngineClientFacade.updateProcessVariable(processInstance.getId(), "plantCount", plantCountVariable);
    processEngineClientFacade.updateProcessVariable(processInstance.getId(), "ctsTechSubmissionStatus", ctsTechSubmissionStatusVariable);

    // *************** finished updating process variable  *******************



    // *************** save audit trail for submission history *************************

//    PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestPlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey,"PLANT");

//             if(existingAuditTrail == null) { 

//                 throw new RuntimeException("No audit trail found for given plant, site and vertical");
//             }
// pick any audit history to get remark as it is comman for all
      

        plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());

        // plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
        // plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
        // plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());


       plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
   //     plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
        

        plantSubmissionAuditTrailDTO.setType("PLANT");

        // set the status of new entry
        plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? Status.APPROVED.name() : Status.REJECTED.name());

        

     //   get the latest plant submission and set the status to pending
        // PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestPlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "PLANT");


        List<PlantSubmissionAuditTrailProjection> latestPlantSubmission = tcsAuditTrailRepository.getLatestPendingPlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "PLANT", Status.PENDING.name());

        if(latestPlantSubmission.isEmpty()  || latestPlantSubmission.size() > 2)  {
            throw new RuntimeException("No latest plant submission found for given site and vertical");
        }
        // tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()), approvalStatus ? "APPROVED" : "REJECTED");
for(PlantSubmissionAuditTrailProjection plantSubmissionAuditTrailProjection : latestPlantSubmission) {
        tcsAuditTrailRepository.updatePlantSubmissionStatusById(UUID.fromString(plantSubmissionAuditTrailProjection.getId()), approvalStatus ? Status.APPROVED.name() : Status.REJECTED.name());
}

        
     tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
 
     // *************** finished saving audit trail for submission history *************************

}




    @Override
    public void ctsApproval(String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {    

        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

      dataValidation(siteId, finacialYear, verticalId);

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        ObjectMapper objectMapper = new ObjectMapper();

      ProcessInstance processInstance = getProcessInstance(businessKey);
      List<TaskDto> tasks = getTasks(businessKey);


      List<TaskDto> taskForPlant = tasks.stream()
        .filter(t -> CTS_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
        .toList();
        
     // compelete one of the pending multi-instance task
      if(taskForPlant.isEmpty()) {  
            // ******* logic for resubmission

            throw new RuntimeException("No CTS Approval task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);

    //         List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

    //         if(submissionStatusVariables.isEmpty()) {
    //             throw new RuntimeException("No submission status variables found for given process instance");
    //         }
    
    //         if(submissionStatusVariables.size() > 1) {
    //             throw new RuntimeException("Multiple submission status variables found for given process instance");
    //         }
    
    //         updatesubmissionStatusVariable(submissionStatusVariables, CTS_SUBMISSION_VARIABLE_NAME, objectMapper, true);
    
    //         Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);
    
    //         // get variable with name "submissionStatus"
    //         VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");
    
    //         processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);
    
    //         plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
    //     //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
    //       plantSubmissionAuditTrailDTO.setType("CTS");
    //     //  plantSubmissionAuditTrailDTO.setStatus("PENDING");
    //  //   plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
    //  plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());
    
    //       // plantName is null for cts submission

    //       String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));

      
    //      tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

           
        
    //         return;

     // ************** finished cts approve-reject logic (applicable only for approved as cts submit == cts approved) *******************


    }

    if(taskForPlant.size() > 1) {  
        throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
      }

      TaskDto taskToComplete = taskForPlant.get(0);

      System.out.println(" CTS Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

      // update process variable corresponding to given Plant 
      List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

      List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

    
      updatesubmissionStatusVariable(submissionStatusVariables, CTS_SUBMISSION_VARIABLE_NAME, objectMapper, true);
      updateApprovedVariable(approvedVariables, true);
    
    
      System.out.println("submissionStatusVariables: " + submissionStatusVariables);

      processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

      // *************** save audit trail for cts approval history *************************

      plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
  //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
      plantSubmissionAuditTrailDTO.setType("CTS");
      //plantSubmissionAuditTrailDTO.setStatus("PENDING");
      plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());

      String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));


//  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

      // ************** cts approve-reject logic (applicable only for approved as cts submit == cts approved) *******************


//       PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "EBS");
    
//       if(existingAuditTrail == null) { 
   
//           throw new RuntimeException("No audit trail found for given site and vertical");
//       }
//    // pick any audit history to get remark as it is comman for all
      
   
//       plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());
   
//       plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
//       plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
//       plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());
//       plantSubmissionAuditTrailDTO.setType("EBS");
//       plantSubmissionAuditTrailDTO.setStatus("APPROVED");
   
//       // PlantName is null for resubmission 
//    tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
   
//      // get the latest plant submission and set the status to pending
//      PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey,  "EBS");
   
//      if(latestPlantSubmission == null)  {
//          throw new RuntimeException("No latest ebs submission found for given site and vertical");
//      }
//      tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()),"APPROVED");



}

    @Override
    public void ctsApproveReject(String siteId, boolean approvalStatus, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {  

        if (approvalStatus)   return;



        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

      dataValidation( siteId, finacialYear, verticalId);

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

 ObjectMapper objectMapper = new ObjectMapper();
 
 ProcessInstance processInstance = getProcessInstance(businessKey);
 List<TaskDto> tasks = getTasks(businessKey);


 List<TaskDto> taskForPlant = tasks.stream()
   .filter(t -> CTS_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
   .toList();


if(taskForPlant.isEmpty()) {  
    throw new RuntimeException("No task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
}

if(taskForPlant.size() > 1) {  
    throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
}

TaskDto taskToComplete = taskForPlant.get(0);

System.out.println(" CTS Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

 // update process variable corresponding to given Plant 
 List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

 List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();


 updatesubmissionStatusVariable(submissionStatusVariables, CTS_SUBMISSION_VARIABLE_NAME, objectMapper, false);

 updateApprovedVariable(approvedVariables, false);


 System.out.println("submissionStatusVariables: " + submissionStatusVariables);

 processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

        resetProcessVariables(businessKey);

        // ObjectMapper objectMapper = new ObjectMapper();

        // // get the process instance 

        // ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

        // if(processInstances.length == 0) {
        //     throw new RuntimeException("No process instance found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        // }

        // if(processInstances.length > 1) {
        //     throw new RuntimeException("Multiple process instances found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        // }

        // ProcessInstance processInstance = processInstances[0];

        // List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

        // if(submissionStatusVariables.isEmpty()) { 
        //     throw new RuntimeException("No submission status variables found for given process instance");
        // }

        // if(submissionStatusVariables.size() > 1) { 
        //     throw new RuntimeException("Multiple submission status variables found for given process instance");
        // }

        // updatesubmissionStatusVariable(submissionStatusVariables, EBS_SUBMISSION_VARIABLE_NAME, objectMapper, approvalStatus);

        // Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);

        // VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");

        // processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);

       

           // *************** save audit trail for eps submission history *************************

//    PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "EBS");
    
//    if(existingAuditTrail == null) { 

//        throw new RuntimeException("No audit trail found for given site and vertical");
//    }
// pick any audit history to get remark as it is comman for all
   

   plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());

//    plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
//    plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
//    plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());


 plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
//plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());


   plantSubmissionAuditTrailDTO.setType("AOM");
 //  plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? "APPROVED" : "REJECTED");
 plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? Status.APPROVED.name() : Status.REJECTED.name());

   // PlantName is null for resubmission 

   String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));
//tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);


  // get the latest plant submission and set the status to pending
//   PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey,  "EBS");

//   if(latestPlantSubmission == null)  {
//       throw new RuntimeException("No latest ebs submission found for given site and vertical");
//   }
//   tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()), approvalStatus ? "APPROVED" : "REJECTED");

//   // reset the status to PENDING for all plant submissions
//   List<PlantSubmissionAuditTrailProjection> plantWiseLatestSubmissions = tcsAuditTrailRepository.getLatestPlantWiseSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "PLANT");

//   List<Object[]> statusUpdates = new ArrayList<>();
//   for(PlantSubmissionAuditTrailProjection plantSubmission : plantWiseLatestSubmissions) {  

//     String status = approvalStatus ? "APPROVED" : "PENDING";
// statusUpdates.add(new Object[] { status, plantSubmission.getId() });


//   }

//   if(!statusUpdates.isEmpty()) {
//     String updateSql = "UPDATE TCS_Submission_History SET Status = ? WHERE Id = ?";
//     jdbcTemplate.batchUpdate(updateSql, statusUpdates);
//   }

   


// *************** finished saving audit trail for submission history *************************
    
          
        
    }





    @Override
    public void epsApproval(String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {    

        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

      dataValidation(siteId, finacialYear, verticalId);

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        ObjectMapper objectMapper = new ObjectMapper();

      ProcessInstance processInstance = getProcessInstance(businessKey);
      List<TaskDto> tasks = getTasks(businessKey);


      List<TaskDto> taskForPlant = tasks.stream()
        .filter(t -> EPS_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
        .toList();
        
     // compelete one of the pending multi-instance task
      if(taskForPlant.isEmpty()) {  
        throw new RuntimeException("No EPS Approval task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);

            // ******* logic for resubmission

    //         List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

    //         List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

    //         if(submissionStatusVariables.isEmpty()) {
    //             throw new RuntimeException("No submission status variables found for given process instance");
    //         }
    
    //         if(submissionStatusVariables.size() > 1) {
    //             throw new RuntimeException("Multiple submission status variables found for given process instance");
    //         }
    
    //         updatesubmissionStatusVariable(submissionStatusVariables, EPS_SUBMISSION_VARIABLE_NAME, objectMapper, true);

    //         updateApprovedVariable(approvedVariables, false);
    
    //         Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);
    //         Map<String, VariableValueDto> approvedVariablesMap = c7VariablesMapper.toEngineFormat(approvedVariables);
    
    //         // get variable with name "submissionStatus"
    //         VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");
    //         VariableValueDto approvedVariable = approvedVariablesMap.get("approved");
    
    //         processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);
    //         processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approved", approvedVariable);
    
    //         plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
    //     //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
    //       plantSubmissionAuditTrailDTO.setType("EPS");
    //     //  plantSubmissionAuditTrailDTO.setStatus("PENDING");
    //  //   plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
    //  plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());
    
    //       // plantName is null for cts submission

    //       String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));


    
    //      tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

    //         // ************** cts approve-reject logic (applicable only for approved as cts submit == cts approved) *******************


        
    //         return;

     // ************** finished cts approve-reject logic (applicable only for approved as cts submit == cts approved) *******************


    }

    if(taskForPlant.size() > 1) {  
        throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
      }

      TaskDto taskToComplete = taskForPlant.get(0);

      System.out.println(" EPS Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

      // update process variable corresponding to given Plant 
      List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

      List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

    
      updatesubmissionStatusVariable(submissionStatusVariables, EPS_SUBMISSION_VARIABLE_NAME, objectMapper, true);

      updateApprovedVariable(approvedVariables, true);
    
    
      System.out.println("submissionStatusVariables: " + submissionStatusVariables);

      processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

      // *************** save audit trail for cts approval history *************************

      plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
  //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
      plantSubmissionAuditTrailDTO.setType("EPS");
      //plantSubmissionAuditTrailDTO.setStatus("PENDING");
      plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());

      String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));


//  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

      // ************** cts approve-reject logic (applicable only for approved as cts submit == cts approved) *******************


//       PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "EBS");
    
//       if(existingAuditTrail == null) { 
   
//           throw new RuntimeException("No audit trail found for given site and vertical");
//       }
//    // pick any audit history to get remark as it is comman for all
      
   
//       plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());
   
//       plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
//       plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
//       plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());
//       plantSubmissionAuditTrailDTO.setType("EBS");
//       plantSubmissionAuditTrailDTO.setStatus("APPROVED");
   
//       // PlantName is null for resubmission 
//    tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
   
//      // get the latest plant submission and set the status to pending
//      PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey,  "EBS");
   
//      if(latestPlantSubmission == null)  {
//          throw new RuntimeException("No latest ebs submission found for given site and vertical");
//      }
//      tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()),"APPROVED");



}



@Override
public void epsApproveReject(String siteId, boolean approvalStatus, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {  

    if (approvalStatus)   return;


 ObjectMapper objectMapper = new ObjectMapper();

    String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

  dataValidation( siteId, finacialYear, verticalId);

    String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

    
 ProcessInstance processInstance = getProcessInstance(businessKey);
 List<TaskDto> tasks = getTasks(businessKey);


 List<TaskDto> taskForPlant = tasks.stream()
   .filter(t -> EPS_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
   .toList();

if(taskForPlant.isEmpty()) {  

    throw new RuntimeException("No task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
}

if(taskForPlant.size() > 1) {  
   throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
 }

 TaskDto taskToComplete = taskForPlant.get(0);

 System.out.println(" EPS Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

 // update process variable corresponding to given Plant 
 List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

 List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();


 updatesubmissionStatusVariable(submissionStatusVariables, EPS_SUBMISSION_VARIABLE_NAME, objectMapper, false);

 updateApprovedVariable(approvedVariables, false);


 System.out.println("submissionStatusVariables: " + submissionStatusVariables);

 processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

    resetProcessVariables(businessKey);

    // ObjectMapper objectMapper = new ObjectMapper();

    // // get the process instance 

    // ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

    // if(processInstances.length == 0) {
    //     throw new RuntimeException("No process instance found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
    // }

    // if(processInstances.length > 1) {
    //     throw new RuntimeException("Multiple process instances found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
    // }

    // ProcessInstance processInstance = processInstances[0];

    // List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

    // if(submissionStatusVariables.isEmpty()) { 
    //     throw new RuntimeException("No submission status variables found for given process instance");
    // }

    // if(submissionStatusVariables.size() > 1) { 
    //     throw new RuntimeException("Multiple submission status variables found for given process instance");
    // }

    // updatesubmissionStatusVariable(submissionStatusVariables, EBS_SUBMISSION_VARIABLE_NAME, objectMapper, approvalStatus);

    // Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);

    // VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");

    // processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);

   

       // *************** save audit trail for eps submission history *************************

//    PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "EBS");

//    if(existingAuditTrail == null) { 

//        throw new RuntimeException("No audit trail found for given site and vertical");
//    }
// pick any audit history to get remark as it is comman for all


plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());

//    plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
//    plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
//    plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());


plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
//plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());


plantSubmissionAuditTrailDTO.setType("CTS");
//  plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? "APPROVED" : "REJECTED");
plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? Status.APPROVED.name() : Status.REJECTED.name());

// PlantName is null for resubmission 

String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));
//tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);


// get the latest plant submission and set the status to pending
//   PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey,  "EBS");

//   if(latestPlantSubmission == null)  {
//       throw new RuntimeException("No latest ebs submission found for given site and vertical");
//   }
//   tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()), approvalStatus ? "APPROVED" : "REJECTED");

//   // reset the status to PENDING for all plant submissions
//   List<PlantSubmissionAuditTrailProjection> plantWiseLatestSubmissions = tcsAuditTrailRepository.getLatestPlantWiseSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "PLANT");

//   List<Object[]> statusUpdates = new ArrayList<>();
//   for(PlantSubmissionAuditTrailProjection plantSubmission : plantWiseLatestSubmissions) {  

//     String status = approvalStatus ? "APPROVED" : "PENDING";
// statusUpdates.add(new Object[] { status, plantSubmission.getId() });


//   }

//   if(!statusUpdates.isEmpty()) {
//     String updateSql = "UPDATE TCS_Submission_History SET Status = ? WHERE Id = ?";
//     jdbcTemplate.batchUpdate(updateSql, statusUpdates);
//   }




// *************** finished saving audit trail for submission history *************************

      
    
}





    @Override
    public void clusterHeadApproveReject(String siteId, boolean approvalStatus, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {   


        if (approvalStatus)  return;

        
        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

        dataValidation(siteId, finacialYear, verticalId);

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        ObjectMapper objectMapper = new ObjectMapper();

 ProcessInstance processInstance = getProcessInstance(businessKey);
      List<TaskDto> tasks = getTasks(businessKey);


      List<TaskDto> taskForPlant = tasks.stream()
        .filter(t -> CLUSTER_HEAD_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
        .toList();

if(taskForPlant.isEmpty()) {  
    throw new RuntimeException("No task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
}

 if(taskForPlant.size() > 1) {  
        throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
      }

      TaskDto taskToComplete = taskForPlant.get(0);

      System.out.println(" Cluster Head Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

      // update process variable corresponding to given Plant 
      List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

      List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

    
      updatesubmissionStatusVariable(submissionStatusVariables, CLUSTER_HEAD_APPROVAL_VARIABLE_NAME, objectMapper, false);

      updateApprovedVariable(approvedVariables, false);
    
    
      System.out.println("submissionStatusVariables: " + submissionStatusVariables);

      processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

        resetProcessVariables(businessKey);

        // ObjectMapper objectMapper = new ObjectMapper();

        // // get the process instance 

        // ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

        // if(processInstances.length == 0) {
        //     throw new RuntimeException("No process instance found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        // }

        // if(processInstances.length > 1) { 
        //     throw new RuntimeException("Multiple process instances found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
        // }

        // ProcessInstance processInstance = processInstances[0];

        // List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

        // if(submissionStatusVariables.isEmpty()) { 
        //     throw new RuntimeException("No submission status variables found for given process instance");
        // }

        // if(submissionStatusVariables.size() > 1) { 
        //     throw new RuntimeException("Multiple submission status variables found for given process instance");
        // }

        // updatesubmissionStatusVariable(submissionStatusVariables, CTS_SUBMISSION_VARIABLE_NAME, objectMapper, approvalStatus);

        // Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);

        // VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");

        // processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);


        // *************** save audit trail for cts head approval history *************************
        
    //     PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "CTS");

    //    if(existingAuditTrail == null) {
           
    //     throw new RuntimeException("No audit trail found for given site and vertical");
    //    }

        

        plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());

        // plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
        // plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
        // plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());

        plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
   //   plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());


        plantSubmissionAuditTrailDTO.setType("EPS");
    //    plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? "APPROVED" : "REJECTED");

    plantSubmissionAuditTrailDTO.setStatus(approvalStatus ? Status.APPROVED.name() : Status.REJECTED.name());

    String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));

    //    tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

    tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
    
        // PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "CTS");

        // if(latestPlantSubmission == null)  {
        //     throw new RuntimeException("No latest plant submission found for given site and vertical");
        // }
        // tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()), approvalStatus ? "APPROVED" : "REJECTED");

       

    }


    @Override
    public void clusterHeadApproval(String siteId, PlantSubmissionAuditTrailDTO plantSubmissionAuditTrailDTO, String finacialYear) {    

        String verticalId = String.valueOf(plantSubmissionAuditTrailDTO.getVerticalId());

        dataValidation(siteId, finacialYear, verticalId);

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        ObjectMapper objectMapper = new ObjectMapper();

        // get process Instance for given business key and process definition key
        ProcessInstance processInstance = getProcessInstance(businessKey);
        List<TaskDto> tasks = getTasks(businessKey);



      List<TaskDto> taskForPlant = tasks.stream()
        .filter(t -> CLUSTER_HEAD_APPROVAL_TASK_DEFINITION_KEY.equals(t.getTaskDefinitionKey()))
        .toList();
        
     // compelete one of the pending multi-instance task
      if(taskForPlant.isEmpty()) {  

            // ******* logic for resubmission

            throw new RuntimeException("No Cluster Head Approval task found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);

    //         List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

    //         if(submissionStatusVariables.isEmpty()) {
    //             throw new RuntimeException("No submission status variables found for given process instance");
    //         }
    
    //         if(submissionStatusVariables.size() > 1) {
    //             throw new RuntimeException("Multiple submission status variables found for given process instance");
    //         }
    
    //         updatesubmissionStatusVariable(submissionStatusVariables, CLUSTER_HEAD_APPROVAL_VARIABLE_NAME, objectMapper, true);
    
    //         Map<String, VariableValueDto> variablesMap = c7VariablesMapper.toEngineFormat(submissionStatusVariables);
    
    //         // get variable with name "submissionStatus"
    //         VariableValueDto submissionStatusVariable = variablesMap.get("approvalStatus");
    
    //         processEngineClientFacade.updateProcessVariable(processInstance.getId(), "approvalStatus", submissionStatusVariable);
    
    //        plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
    //  //   plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
    //       plantSubmissionAuditTrailDTO.setType("CLUSTER_HEAD");
    //    //  plantSubmissionAuditTrailDTO.setStatus("PENDING");
    //    plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());
    
    //       // plantName is null for cts submission

    //       String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));
         
    
    //       tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);


           
    //         return;

    }

    if(taskForPlant.size() > 1) {  
        throw new RuntimeException("Multiple tasks found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
      }

      TaskDto taskToComplete = taskForPlant.get(0);

      System.out.println(" Cluster Head Approval taskToComplete Id: " + taskToComplete.getId() + "name: " + taskToComplete.getName());

      // update process variable corresponding to given Plant 
      List<ProcessVariable> submissionStatusVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approvalStatus")).toList();

      List<ProcessVariable> approvedVariables = Arrays.stream(processEngineClientFacade.findVariables(processInstance.getId())).filter(v -> v.getName().equals("approved")).toList();

    
      updatesubmissionStatusVariable(submissionStatusVariables, CLUSTER_HEAD_APPROVAL_VARIABLE_NAME, objectMapper, true);

      updateApprovedVariable(approvedVariables, true);
    
    
      System.out.println("submissionStatusVariables: " + submissionStatusVariables);

      processEngineClientFacade.complete(taskToComplete.getId(), List.of(submissionStatusVariables.get(0), approvedVariables.get(0)));

      // *************** save audit trail for cts approval history *************************

      plantSubmissionAuditTrailDTO.setSubmissionDateTime(new Date());
  //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(getISTDateTime());
      plantSubmissionAuditTrailDTO.setType("CLUSTER_HEAD");
    //  plantSubmissionAuditTrailDTO.setStatus("PENDING");
  //  plantSubmissionAuditTrailDTO.setStatus(Status.SUBMITTED.name());
  plantSubmissionAuditTrailDTO.setStatus(Status.APPROVED.name());

    String plantNames = plantNamesFormat(getPlantList(UUID.fromString(verticalId), UUID.fromString(siteId)));
    //  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

    tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantNames, plantSubmissionAuditTrailDTO.getPlantStatus(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getUserName(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);

      
            // ************** cluster head approve-reject logic (applicable only for approved as cluster head submit == cluster head approved) *******************

            // PlantSubmissionAuditTrailProjection existingAuditTrail = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "CTS");

            // if(existingAuditTrail == null) {
                
            //  throw new RuntimeException("No audit trail found for given site and vertical");
            // }
     
             
     
            //  plantSubmissionAuditTrailDTO.setVerifiedDateTime(new Date());
     
            //  plantSubmissionAuditTrailDTO.setSubmissionDateTime(existingAuditTrail.getSubmissionDate());
            //  plantSubmissionAuditTrailDTO.setSubmissionRemark(existingAuditTrail.getSubmissionRemark());
            //  plantSubmissionAuditTrailDTO.setSubmittedBy(existingAuditTrail.getSubmittedBy());
            //  plantSubmissionAuditTrailDTO.setType("CTS");
            //  plantSubmissionAuditTrailDTO.setStatus("APPROVED");
     
            //  tcsAuditTrailRepository.savePlantSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getPlantId(), plantSubmissionAuditTrailDTO.getPlantName(), plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), plantSubmissionAuditTrailDTO.getSubmittedBy(), plantSubmissionAuditTrailDTO.getSubmissionDateTime(), plantSubmissionAuditTrailDTO.getSubmissionRemark(), plantSubmissionAuditTrailDTO.getVerifiedDateTime(), plantSubmissionAuditTrailDTO.getVerifiedBy(), plantSubmissionAuditTrailDTO.getVerifiedRemark(), plantSubmissionAuditTrailDTO.getStatus(), plantSubmissionAuditTrailDTO.getType(), businessKey);
         
            //   // reset the status for type CTS to approved / rejected
            //  PlantSubmissionAuditTrailProjection latestPlantSubmission = tcsAuditTrailRepository.getLatestEbsSubmissionAuditTrail(plantSubmissionAuditTrailDTO.getSiteId(), plantSubmissionAuditTrailDTO.getVerticalId(), businessKey, "CTS");
     
            //  if(latestPlantSubmission == null)  {
            //      throw new RuntimeException("No latest plant submission found for given site and vertical");
            //  }
            //  tcsAuditTrailRepository.updateSubmissionStatusById(UUID.fromString(latestPlantSubmission.getId()), "APPROVED");
    
            // ************** finished cluster head approve-reject logic (applicable only for approved as cluster head submit == cluster head approved) *******************

}





    public void updatesubmissionStatusVariable(List<ProcessVariable> variables, String variableName, ObjectMapper objectMapper, boolean submissionStatus)  {

        for (ProcessVariable variable : variables) {

            try {
                JsonNode rootNode;
        
                Object value = variable.getValue();
        
                if (value instanceof String) {
                    // Value is already JSON string
                    rootNode = objectMapper.readTree((String) value);
                } else {
                    // Value is Map / LinkedHashMap / Object
                    rootNode = objectMapper.valueToTree(value);
                }
        
                if (!rootNode.isObject()) {
                    throw new IllegalStateException("submissionStatus is not a JSON object");
                }
        
                ObjectNode jsonNode = (ObjectNode) rootNode;
        
                jsonNode.put(variableName, submissionStatus);
                // IMPORTANT: set back as JSON string for Camunda
                variable.setValue(objectMapper.writeValueAsString(jsonNode));
        
            } catch (Exception e) {
                throw new RuntimeException(
                    "Error processing submissionStatusDTO JSON", e
                    );
                }
            }
        
    }

    public void updatePlantCountVariable(List<ProcessVariable> variables, String variableName, ObjectMapper objectMapper, boolean submissionStatus, boolean isReset)  {

        for (ProcessVariable variable : variables) {

            try {
                JsonNode rootNode;
        
                Object value = variable.getValue();
        
                if (value instanceof String) {
                    // Value is already JSON string
                    rootNode = objectMapper.readTree((String) value);
                } else {
                    // Value is Map / LinkedHashMap / Object
                    rootNode = objectMapper.valueToTree(value);
                }
        
                if (!rootNode.isObject()) {
                    throw new IllegalStateException("submissionStatus is not a JSON object");
                }
        
                ObjectNode jsonNode = (ObjectNode) rootNode;

          


         
        if(isReset) { 

            jsonNode.put(variableName, Integer.valueOf(0));
        }
        else {
            Integer approvedPlants = (Integer) jsonNode.get(variableName).asInt();
            if(submissionStatus) {
                approvedPlants++;  }

                jsonNode.put(variableName, approvedPlants); 
            }

                // IMPORTANT: set back as JSON string for Camunda
                variable.setValue(objectMapper.writeValueAsString(jsonNode));
        
            } catch (Exception e) {
                throw new RuntimeException(
                    "Error processing submissionStatusDTO JSON", e
                    );
                }
            }
        
    }


    public void updateApprovedVariable(List<ProcessVariable> variables, boolean approved) {

        for (ProcessVariable variable : variables) {
      System.out.println("looping through approved variables: variable: " + variable);
            if ("approved".equals(variable.getName())) {
                variable.setValue(approved);
                break;
            }
        }
    }


    @Override
    public List<PlantSubmissionAuditTrailDTO> getAuditTrail(String verticalId, String siteId, String finacialYear) { 
  
     

        if(siteId == null || siteId.isEmpty()) {
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) {
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) {
            throw new RuntimeException("Vertical id is required");
        }

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

         List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getAuditTrail(businessKey);

       

         return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
          
         .plantName(auditTrail.getPlantName())
         .submittedBy(auditTrail.getSubmittedBy())
         .userName(auditTrail.getUserName())
         .submissionDateTime(auditTrail.getSubmissionDate())
         .submissionRemark(auditTrail.getSubmissionRemark())
         .status(auditTrail.getStatus())
         .build()).toList();



    }


  
    @Override
    public List<PlantSubmissionAuditTrailDTO> getPlantSubmissionAuditTrail(String plantId, String siteId, String verticalId, String type, String finacialYear) { 
  
        if(plantId == null || plantId.isEmpty()) {  
            throw new RuntimeException("Plant id is required");
        }

        if(siteId == null || siteId.isEmpty()) {
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) {
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) {
            throw new RuntimeException("Vertical id is required");
        }

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

         List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getPlantSubmissionAuditTrail(UUID.fromString(plantId), UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type);

         

         return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
         .plantId(UUID.fromString(auditTrail.getPlant_Id()))
         .plantName(auditTrail.getPlantName())
         .siteId(UUID.fromString(auditTrail.getSite_Id()))
         .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
         .submittedBy(auditTrail.getSubmittedBy())
         .submissionDateTime(auditTrail.getSubmissionDate())
         .submissionRemark(auditTrail.getSubmissionRemark())
         .verifiedDateTime(auditTrail.getVerifiedDate())
         .verifiedBy(auditTrail.getVerifiedBy())
         .verifiedRemark(auditTrail.getVerifiedRemark())
         .status(auditTrail.getStatus())
         .type(auditTrail.getType())
         .build()).toList();

    }

    @Override
    public List<PlantSubmissionAuditTrailDTO> getLatestPlantWiseSubmissionAuditTrail(String siteId, String verticalId, String type, String finacialYear) { 

        if(siteId == null || siteId.isEmpty()) {  
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) { 
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RuntimeException("Vertical id is required");
        }
        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        // List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getLatestPlantWiseSubmissionAuditTrail(UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type);

        List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getLatestPendingPlantWiseSubmissionAuditTrail(UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type, Status.PENDING.name());




        return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
        .plantId(UUID.fromString(auditTrail.getPlant_Id()))
        .plantName(auditTrail.getPlantName())
        .plantStatus(auditTrail.getPlantStatus())
        .siteId(UUID.fromString(auditTrail.getSite_Id()))
        .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
        .submittedBy(auditTrail.getSubmittedBy())
        .submissionDateTime(auditTrail.getSubmissionDate())
        .submissionRemark(auditTrail.getSubmissionRemark())
        .verifiedDateTime(auditTrail.getVerifiedDate())
        .verifiedBy(auditTrail.getVerifiedBy())
        .verifiedRemark(auditTrail.getVerifiedRemark())
        .status(auditTrail.getStatus())
        .type(auditTrail.getType())
        .build()).toList();
       
    }

    @Override
    // get bps approve/reject history
    public List<PlantSubmissionAuditTrailDTO> getPlantSubmissionAuditTrailByVerfiedDate(String plantId, String siteId, String verticalId, String type, String finacialYear) { 
     
        
        if(siteId == null || siteId.isEmpty()) {  
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) { 
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RuntimeException("Vertical id is required");
        }
        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getPlantSubmissionAuditTrailByVerfiedDate(UUID.fromString(plantId), UUID.fromString(siteId), UUID.fromString(verticalId),businessKey, type);
       

        return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
        .plantId(UUID.fromString(auditTrail.getPlant_Id()))
        .plantName(auditTrail.getPlantName())
        .siteId(UUID.fromString(auditTrail.getSite_Id()))
        .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
        .submittedBy(auditTrail.getSubmittedBy())
        .submissionDateTime(auditTrail.getSubmissionDate())
        .submissionRemark(auditTrail.getSubmissionRemark())
        .verifiedDateTime(auditTrail.getVerifiedDate())
        .verifiedBy(auditTrail.getVerifiedBy())
        .verifiedRemark(auditTrail.getVerifiedRemark())
        .status(auditTrail.getStatus())
        .type(auditTrail.getType())
        .build()).toList();
    }

    @Override
    public List<PlantSubmissionAuditTrailDTO> getEbsSubmissionAuditTrailByVerfiedDate(String siteId, String verticalId, String type, String finacialYear) {
       
        
        if(siteId == null || siteId.isEmpty()) {  
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) { 
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RuntimeException("Vertical id is required");
        }
        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getEbsSubmissionAuditTrailByVerfiedDate(UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type);

        return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
        .plantName(auditTrail.getPlantName())
        .siteId(UUID.fromString(auditTrail.getSite_Id()))
        .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
        .build()).toList();
    }

    




    @Override
    public PlantSubmissionAuditTrailDTO getLatestAOMSubmissionAuditTrail(String siteId, String verticalId, String type, String finacialYear) {
       
        
        if(siteId == null || siteId.isEmpty()) {  
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) { 
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RuntimeException("Vertical id is required");
        }
        String businessKey =  generateBusinessKey(verticalId, siteId, finacialYear);

        PlantSubmissionAuditTrailProjection auditTrail = tcsAuditTrailRepository.getLatestAOMSubmissionAuditTrail(UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type);
        return PlantSubmissionAuditTrailDTO.builder()
        .plantName(auditTrail.getPlantName())
        .siteId(UUID.fromString(auditTrail.getSite_Id()))
        .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
        .submittedBy(auditTrail.getSubmittedBy())
        .submissionDateTime(auditTrail.getSubmissionDate())
        .submissionRemark(auditTrail.getSubmissionRemark())
        .build();
    }

    @Override
    public List<PlantSubmissionAuditTrailDTO> getEBSSubmissionAuditTrail(String siteId,
            String verticalId, String type, String finacialYear) {
       
                
        if(siteId == null || siteId.isEmpty()) {  
            throw new RuntimeException("Site id is required");
        }

        if(finacialYear == null || finacialYear.isEmpty()) { 
            throw new RuntimeException("Financial year is required");
        }

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RuntimeException("Vertical id is required");
        }

        String businessKey = generateBusinessKey(verticalId, siteId, finacialYear);

        List<PlantSubmissionAuditTrailProjection> auditTrails = tcsAuditTrailRepository.getEbsSubmissionAuditTrail(UUID.fromString(siteId), UUID.fromString(verticalId), businessKey, type);
        return auditTrails.stream().map(auditTrail -> PlantSubmissionAuditTrailDTO.builder()
        .plantName(auditTrail.getPlantName())
        .siteId(UUID.fromString(auditTrail.getSite_Id()))
        .verticalId(UUID.fromString(auditTrail.getVertical_Id()))
        .submittedBy(auditTrail.getSubmittedBy())
        .submissionDateTime(auditTrail.getSubmissionDate())
        .submissionRemark(auditTrail.getSubmissionRemark())
        .verifiedDateTime(auditTrail.getVerifiedDate())
        .verifiedBy(auditTrail.getVerifiedBy())
        .verifiedRemark(auditTrail.getVerifiedRemark())
        .status(auditTrail.getStatus())
        .build()).toList();
    }


    // schedular for email notification

   // @Scheduled(cron = "0 0 12 * * ?")
  @Override
    public void notifyPlantManagers() {
          
         // get all the users with role Plant_Manager
             ObjectMapper objectMapper = new ObjectMapper();
             Map<String, List<UUID>> result = new HashMap<>();

            List<UserRepresentation> userRepresentations;
            try {
                userRepresentations = keycloakUserService.getUsersWithRole(Roles.Plant_Manager.name());
            } catch (Exception e) {
                throw new RestResourceNotFoundException("Error getting users with role Plant_Manager: " + e.getMessage());
            }


     // ************ filter userIds which has access to tcs input screen  | Map plant name to user ids eg : {"CDU-1": ["123", "456"], "CDU-2": ["112"]}  ***************
            String userIdsJson;
            try {
                userIdsJson = objectMapper.writeValueAsString(userRepresentations.stream().map(UserRepresentation::getId).map(UUID::fromString).collect(Collectors.toList()));
            } catch (JsonProcessingException e) {
                throw new RestResourceNotFoundException("Error converting userIds to JSON: " + e.getMessage());
            }

            StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("GetUserIdsByPlantAndScreen_JSON");

    query.registerStoredProcedureParameter("UserIdsJson", String.class, jakarta.persistence.ParameterMode.IN);
    query.registerStoredProcedureParameter("ScreenCode", String.class, jakarta.persistence.ParameterMode.IN);

    query.setParameter("UserIdsJson", userIdsJson);
    query.setParameter("ScreenCode", screenCode);

    // Execute
    String jsonResult = (String) query.getSingleResult();

    // Parse JSON response
    JsonNode root;
    try {
        root = objectMapper.readTree(jsonResult);
    } catch (JsonMappingException e) {
        throw new RestResourceNotFoundException("Error converting submissionStatusDTO to JSON: " + e.getMessage());
    } catch (JsonProcessingException e) {
        throw new RestResourceNotFoundException("Error converting submissionStatusDTO to JSON: " + e.getMessage());
    }


    JsonNode dataArray = root.get("data");

    if (dataArray != null && dataArray.isArray()) {
        for (JsonNode node : dataArray) {

            String plantName = node.get("PlantName").asText();
            List<UUID> users = new ArrayList<>();

            JsonNode userIdsNode = node.get("UserIds");

            if (userIdsNode != null && userIdsNode.isArray()) {
                for (JsonNode userNode : userIdsNode) {
                    UUID userId = UUID.fromString(userNode.get("UserId").asText());
                    users.add(userId);
                }
            }

            result.put(plantName, users);
        }
    }

    //   ********************* Finished :  Map plant name to user ids     ******************



// ***************** Map Plant name to email Ids eg : {"CDU-1": ["123@gmail.com", "456@gmail.com"], "CDU-2": ["112@gmail.com"]}  ******************
    Map<UUID, String> userIdToEmailMap = userRepresentations.stream()
        .collect(Collectors.toMap(
                user -> UUID.fromString(user.getId()),
                UserRepresentation::getEmail
        ));


        Map<String, List<String>> plantToEmails = new HashMap<>();

for (Map.Entry<String, List<UUID>> entry : result.entrySet()) {
    String plant = entry.getKey();
    List<UUID> userIds = entry.getValue();

    List<String> emails = userIds.stream()
            .map(userIdToEmailMap::get)   // get email from map
            .filter(Objects::nonNull)     // remove missing users (safety)
            .collect(Collectors.toList());

    plantToEmails.put(plant, emails);
}

System.out.println("plantToEmails: " + plantToEmails);

// ********************* Finished :  Map Plant name to email Ids     ******************

 

List<UUID> sites = tcsAuditTrailRepository.getSitesByVerticalName(vertical);

UUID verticalId = tcsAuditTrailRepository.getVerticalIdByName(vertical);

int year = LocalDate.now().getYear();
int nextYear = year + 1;



// Format: 2025-26
 String finacialYear = year + "-" + String.valueOf(nextYear).substring(2);

 List<String> emailsToNotify = new ArrayList<>();

for(UUID siteId : sites) {  

   

    String businessKey = generateBusinessKey(String.valueOf(verticalId).toUpperCase(), String.valueOf(siteId).toUpperCase(), finacialYear);

    



    ProcessInstance[] processInstances = processEngineClientFacade.findProcessInstances(Optional.ofNullable(PROCESS_DEFINITION_KEY), Optional.ofNullable(businessKey), Optional.empty());

			if(processInstances.length > 1) {
				throw new RestResourceNotFoundException("Multiple process instances found for business key: " + businessKey + " and process definition key: " + PROCESS_DEFINITION_KEY);
			}

    if(processInstances.length == 0) {
        continue;
    }

    ProcessVariable[] processVariables = processEngineClientFacade.findVariables(processInstances[0].getId());


   

    for(ProcessVariable processVariable : processVariables) {
        System.out.println("processVariable: " + processVariable.getName());
        if(processVariable.getName().equals("submissionStatus")) {
            String submissionStatus = processVariable.getValue().toString();
           

            String submissionStatusJson = processVariable.getValue().toString();

            objectMapper = new ObjectMapper();
    
            // Convert JSON -> Map<String, Boolean>
            Map<String, Boolean> submissionStatusMap;
            try {
                submissionStatusMap = objectMapper.readValue(
                        submissionStatusJson,
                        new TypeReference<Map<String, Boolean>>() {}
                );
            } catch (JsonMappingException e) {
                throw new RestResourceNotFoundException("Error converting submissionStatusDTO to JSON: " + e.getMessage());
                
            } catch (JsonProcessingException e) {
               
                throw new RestResourceNotFoundException("Error converting submissionStatusDTO to JSON: " + e.getMessage());
            }
    
            // Iterate over map
            System.out.println("submissionStatusMap: " + submissionStatusMap);
            for (Map.Entry<String, Boolean> entry : submissionStatusMap.entrySet()) {
                String plant = entry.getKey();
                Boolean status = entry.getValue();
    
                // If status is false, collect emails
                if (Boolean.FALSE.equals(status)) {
                    List<String> emails = plantToEmails.get(plant);
                    if (emails != null) {
                        System.out.println("emails: " + emails);
                        emailsToNotify.addAll(emails);
                    }
                }
        }
    }

}



    }

    System.out.println("emailsToNotify: " + emailsToNotify);


        


    // @Override
    // public void notifyPlantManagers() {
          
    //      // get all the users with role Plant_Manager
    //      try {

    //          ObjectMapper objectMapper = new ObjectMapper();
    //          Map<String, List<UUID>> result = new HashMap<>();

    //          // get all the users with role Plant_Manager
    //        Map<String, Object> users = keycloakUserService.getUsers();

    //         System.out.println("users: " + users);
    //      } catch (Exception e) {
    //         e.printStackTrace();
    //      }
    // }

}

}