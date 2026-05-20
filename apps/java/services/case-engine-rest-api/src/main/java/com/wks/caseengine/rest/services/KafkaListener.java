package com.wks.caseengine.rest.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.wks.caseengine.cases.definition.service.KeycloakService;
import com.wks.caseengine.cases.instance.CaseAttribute;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.cases.instance.email.CaseEmailServiceImpl;
import com.wks.caseengine.cases.instance.service.CaseInstanceService;
import com.wks.caseengine.rest.db2.entity.Groups;
import com.wks.caseengine.rest.db2.repository.GroupsRepository;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.tasks.TaskServiceImpl;

@Component
public class KafkaListener {

 //  private  String topic = System.getenv("KAFKA_TOPIC_CREATE_HUMAN_TASK");

//   @Autowired
//   private KeycloakService keycloakService;

  @Autowired
  private TaskServiceImpl taskService;

  @Autowired
  private CaseEmailServiceImpl caseEmailService;

  @Value("${react.frontend.url}")
  private String reactFrontendUrl;

  @Autowired
  private GroupsRepository groupsRepository;

  @Autowired
  private CaseInstanceService caseInstanceService;
  
  private static final String topic = "case-create";
  //private List<String> emails = new ArrayList<>();

    
  @org.springframework.kafka.annotation.KafkaListener(topics = topic, groupId = "case-create-group-B")
    public void listen(String message) {
      
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;
        String businessKey = null;
        String taskName = null;
        String taskDefKey = null;
        String taskId = null;
        try {
            jsonNode = objectMapper.readTree(message);

             businessKey = jsonNode.get("businessKey").asText();
             taskName = jsonNode.get("taskName").asText();
             taskDefKey = jsonNode.get("taskDefKey").asText();
             taskId = jsonNode.get("taskId").asText();
        } catch (JsonMappingException e) {
          
            e.printStackTrace();
        } catch (JsonProcessingException e) {
           
            e.printStackTrace();
        }
    
        // CaseInstance caseInstance = null;
         String caseName = null;
        // try {
        //     caseInstance = caseInstanceService.get(businessKey);
        //     System.out.println("KafkaListener  caseInstance retrieved sucessfully");

        //     List<CaseAttribute> attributes = caseInstance.getAttributes();
        // CaseAttribute attribute = attributes.get(0);
        // System.out.println("KafkaListener  attribute: " + attribute.toString());
        // String attributeValue = attribute.getValue();
        // ObjectMapper objectMapper1 = new ObjectMapper();
        // JsonNode rootNode = objectMapper1.readTree(attributeValue);
        //   caseName = rootNode.path("caseTitle").asText();

        // System.out.println("kafkaListener caseName: " + caseName);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
      
      


    

        // http://localhost:3001/case-list/create?taskId=0a2ec62b-c43a-11f0-894b-fa26691c57d3&caseNo=56596
        
   String url = reactFrontendUrl + "/case-list/create-Asset?taskId=" + taskId + "&caseNo=" + businessKey;

    List<Groups> groups = groupsRepository.findAll();
    System.out.println("********** groups: " + groups + "**********");
    List<String> emails = new ArrayList<>();
       
        if(taskDefKey.equals("aot-processengr")) {
            System.out.println("**** KafkaListner taskName: " + taskName + "****");
           taskService.claim(taskId, "Process_Engineer");
        //   String url = reactFrontendUrl + "/task/" + taskDefKey;
      

        //  url = reactFrontendUrl + "/case-list/create?taskDefKey=" + taskDefKey + "&caseNo=" + businessKey;
        //    emails = keycloakService.getGroupMembers("Process_Engineer").stream().map(user -> user.getEmail()).collect(Collectors.toList());

     //   emails =     groups.filter(group -> group.getGroupId().equals("Process_Engineer")).map(group -> group.getUsers().stream().map(user -> user.getEmail()).collect(Collectors.toList()));
           
emails = groups.stream().filter(group -> group.getGroupId().equals("Process_Engineer")).flatMap(group -> group.getUsers().stream().map(user -> user.getEmailId())).collect(Collectors.toList());

        } else if(taskDefKey.equals("aot-machineryengr")) {
            System.out.println("**** KafkaListner taskName: " + taskName + "****");
            taskService.claim(taskId, "Machinery_Engineer");
            
        //    emails = keycloakService.getGroupMembers("Machinery_Engineer").stream().map(user -> user.getEmail()).collect(Collectors.toList());
        emails = groups.stream().filter(group -> group.getGroupId().equals("Machinery_Engineer")).flatMap(group -> group.getUsers().stream().map(user -> user.getEmailId())).collect(Collectors.toList());

        } else if(taskDefKey.equals("aot-modsengr")) {
            System.out.println("**** KafkaListner taskName: " + taskName + "****");
            taskService.claim(taskId, "Mods_Engineer");
          
       //     emails = keycloakService.getGroupMembers("Mods_Engineer").stream().map(user -> user.getEmail()).collect(Collectors.toList());
        emails = groups.stream().filter(group -> group.getGroupId().equals("Mods_Engineer")).flatMap(group -> group.getUsers().stream().map(user -> user.getEmailId())).collect(Collectors.toList());
        }  

        Map<String, Object> data = new HashMap<>();
        data.put("taskFormUrl", url);
        data.put("caseName", caseName);
        data.put("taskName", taskName);

        if(   emails == null || emails.isEmpty() ) {
            System.out.println("No emails found for the task" + taskDefKey);
            System.out.println("#############################  Received message: " + message + "################");
            return;
        }

        caseEmailService.send(emails.toArray(new String[0]), "New Task Created", null, null, null, "task-notification", data);

        System.out.println("********* KafkaListner emails: " + emails + "****************");

//emails = keycloakService.getGroupMembers("Process_Engineer").stream().map(user -> user.getEmail()).collect(Collectors.toList());

 



       
        
        System.out.println("#############################  Received message: " + message + "################");
        System.out.println("#############################  businesskey: " + businessKey + " taskname: " + taskName + "################");
        System.out.println("#############################  emails: " + emails + "################");
         System.out.println("#############################  taskId: " + taskId + "################");
    }
    
}
