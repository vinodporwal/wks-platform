package com.wks.caseengine.tcs.serviceimpl;

import com.wks.caseengine.tcs.service.EmailService;

import java.io.IOException;
import java.util.*;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;


public class EmailServiceImpl implements EmailService {
    


    @Autowired
	private JavaMailSender mailSender;

	@Autowired
	private SpringTemplateEngine templateEngine;
	    
	@Value("${spring.mail.fromEmail}")
	private String from;

	    
	
@Override
	public void send(String[] to, String subject, String[] cc, String bcc,
        List<MultipartFile> attachments, String templateName, Map<String, Object> placeholders) {
			System.out.println("********** CaseEmailServiceImpl send method **********");

			if(templateName.equals("tcs-workflow-template")) {  

				System.out.println("********** Task Notification **********");
			}
		 try {
	            MimeMessage message = mailSender.createMimeMessage();
			  
	            MimeMessageHelper helper = new MimeMessageHelper(message, true);
	            helper.setFrom(from);

	            helper.setTo(to);
	            helper.setSubject(subject);

	            if (cc != null ) {
	                helper.setCc(cc);
	            }

	            if (bcc != null && !bcc.isEmpty()) {
	                helper.setBcc(bcc);
	            }

			

	            // Process the template
	            Context context = new Context();
	            context.setVariables(placeholders);
	            String htmlContent = templateEngine.process(templateName, context);
	            helper.setText(htmlContent, true);

	            // Add attachments
	            if (attachments != null && !attachments.isEmpty()) {
	                for (MultipartFile attachment : attachments) {
	                    byte[] bytes = attachment.getBytes();
	                    helper.addAttachment(attachment.getOriginalFilename(), new ByteArrayDataSource(bytes, attachment.getContentType()));

	                }
	            }
	            System.out.println("From Email "+ from);
	            System.out.println("To Email "+ Arrays.toString(to));

	            mailSender.send(message);
	            System.out.println("ratnesh email send");
	        } catch (MessagingException | IOException e) {
	            // Handle exceptions
	            System.out.println(("Error sending email "+e));
	        }		
	}



}
