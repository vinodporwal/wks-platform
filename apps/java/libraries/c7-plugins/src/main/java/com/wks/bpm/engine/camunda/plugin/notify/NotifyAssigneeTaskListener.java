package com.wks.bpm.engine.camunda.plugin.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

/**
 * Task listener to be executed when a user task is created.
 *
 * <p>Kafka producer creation is lazy and failure-tolerant. The parse listener
 * registers this on every user task at BPMN parse time; eagerly constructing a
 * {@link KafkaProducer} in the constructor previously made Camunda refuse to
 * start any process (including AOP_Approval_v2) whenever Kafka was down or the
 * bootstrap host was unresolvable.</p>
 */
public class NotifyAssigneeTaskListener implements TaskListener {

	public static List<String> assigneeList = new ArrayList<String>();

	private volatile KafkaProducer<String, String> kafkaProducer;

	private static NotifyAssigneeTaskListener instance = null;

	private final String kafkaUrl = System.getenv("KAFKA_URL");
	private final String topic = System.getenv("KAFKA_TOPIC_CREATE_HUMAN_TASK");

	protected NotifyAssigneeTaskListener() {
		// Producer is created on first notify — never during BPMN parse / class load.
	}

	public static synchronized NotifyAssigneeTaskListener getInstance() {
		if (instance == null) {
			instance = new NotifyAssigneeTaskListener();
		}
		return instance;
	}

	private KafkaProducer<String, String> producer() {
		KafkaProducer<String, String> existing = kafkaProducer;
		if (existing != null) {
			return existing;
		}
		synchronized (this) {
			if (kafkaProducer != null) {
				return kafkaProducer;
			}
			if (kafkaUrl == null || kafkaUrl.isBlank()) {
				System.err.println("NotifyAssigneeTaskListener: KAFKA_URL not set; skipping Kafka notify");
				return null;
			}
			try {
				Properties props = new Properties();
				props.put("bootstrap.servers", kafkaUrl);
				props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
				props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
				// Fail fast on bad bootstrap so we don't block the task create path.
				props.put("max.block.ms", "3000");
				kafkaProducer = new KafkaProducer<>(props);
				System.out.println("In Kafka NotifyAssigneeTaskListener c7-plugins");
				return kafkaProducer;
			} catch (Exception e) {
				System.err.println("NotifyAssigneeTaskListener: Kafka producer unavailable (" + e.getMessage()
						+ "); task create will continue without notify");
				return null;
			}
		}
	}

	public void publishToKafka(String topic, String key, String value) {
		KafkaProducer<String, String> producer = producer();
		if (producer == null || topic == null || topic.isBlank()) {
			return;
		}
		try {
			ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
			producer.send(record);
		} catch (Exception e) {
			System.err.println("NotifyAssigneeTaskListener: failed to publish (" + e.getMessage() + ")");
		}
	}

	@Override
	public void notify(DelegateTask delegateTask) {
		String businessKey = delegateTask.getExecution().getProcessInstance().getBusinessKey();
		String taskName = delegateTask.getName();
		String assigneeEmail = delegateTask.getAssignee();

		String json = "{\"businessKey\": \"" + businessKey + "\",\"taskName\": \"" + taskName + "\",\"owner\": {\"email\": \""
				+ assigneeEmail + "\" }}";

		publishToKafka(topic, "taskDefKey", json);
	}

}
