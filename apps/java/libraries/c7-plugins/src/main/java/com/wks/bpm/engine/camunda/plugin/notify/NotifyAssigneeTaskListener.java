package com.wks.bpm.engine.camunda.plugin.notify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;





public class NotifyAssigneeTaskListener implements TaskListener {



	public static List<String> assigneeList = new ArrayList<String>();

	private KafkaProducer<String, String> kafkaProducer;
	private static NotifyAssigneeTaskListener instance = null;
	private String kafkaUrl = System.getenv("KAFKA_URL");
//	private String topic = System.getenv("KAFKA_TOPIC_CREATE_HUMAN_TASK");
     private String topic = "case-create";

	public NotifyAssigneeTaskListener() {
		// Initialize Kafka producer configuration
		
		Properties props = new Properties();
		props.put("bootstrap.servers", kafkaUrl);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		// Instantiate Kafka producer
		kafkaProducer = new KafkaProducer<>(props);
	}

	public static NotifyAssigneeTaskListener getInstance() {
		if (instance == null) {
			instance = new NotifyAssigneeTaskListener();
		}
		return instance;
	}

	public void publishToKafka(String topic, String key, String value) {
		// Publish event to Kafka
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
		kafkaProducer.send(record);
	}

	@Override
	public void notify(DelegateTask delegateTask) {

		System.out.println("********** notify()   NotifyAssigneeTaskListener **********");
		System.out.println("taksName: " + delegateTask.getName());
		String businessKey = delegateTask.getExecution().getProcessInstance().getBusinessKey();
		String taskName = delegateTask.getName();
		String taskDefKey = delegateTask.getTaskDefinitionKey();
		String taskId = delegateTask.getId();

	//	String json = "{\"businessKey\": \"" + businessKey + "\", \"taskName\": \"" + taskName + "\", \"taskDefKey\": \"" + taskDefKey + "\", \"taskId\": \"" + taskId + "\" }}";

	String json = String.format(
    "{\"businessKey\": \"%s\", \"taskName\": \"%s\", \"taskDefKey\": \"%s\", \"taskId\": \"%s\"}",
    businessKey, taskName, taskDefKey, taskId
);

	// ObjectMapper mapper = new ObjectMapper();
	// Map<String, String> map = new HashMap<>();
	// map.put("businessKey", businessKey);
	// map.put("taskName", taskName);
	// map.put("taskDefKey", taskDefKey);
	// map.put("taskId", taskId);

	// String json = mapper.writeValueAsString(map);
	// publishToKafka(topic, "taskDefKey", json);

		publishToKafka(topic, "taskDefKey", json);

}
}
