/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.cases.instance.email;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.wks.caseengine.command.CommandExecutor;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Component
public class CaseEmailServiceImpl implements CaseEmailService {

	private static final Logger log = LoggerFactory.getLogger(CaseEmailServiceImpl.class);

	@Autowired
	private CommandExecutor commandExecutor;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private SpringTemplateEngine templateEngine;
	    
	@Value("${spring.mail.fromEmail}")
	private String from;

	    
	@Override
	public List<CaseEmail> find(Optional<String> businessKey) {
		return commandExecutor.execute(new FindCaseEmailCmd(businessKey));
	}

	@Override
	public void start(CaseEmail caseEmail) {
		if (!caseEmail.isOutbound()) {
			commandExecutor.execute(new StartCaseEmailCmd(caseEmail));
		} else {
			commandExecutor.execute(new StartCaseEmailOutboundCmd(caseEmail));
		}
	}

	@Override
	public CaseEmail save(CaseEmail caseEmail) {
		return commandExecutor.execute(new SaveCaseEmailCmd(caseEmail));
	}

	@Override
	public void markAsSent(final String id, final Date sentDateTime) {
		commandExecutor.execute(new MarkAsSentCaseEmailCmd(id, sentDateTime));
	}

	@Override
	public void patch(final String id, final CaseEmail mergePatch) {
		commandExecutor.execute(new PatchCaseEmailCmd(id, mergePatch));
	}

    @Async
//	@Override
	public void send(String from1, String to, String subject, String[] cc, String bcc,
			List<MultipartFile> attachments, String templateName, Map<String, Object> placeholders) {
		 try {
	            MimeMessage message = mailSender.createMimeMessage();
	            MimeMessageHelper helper = new MimeMessageHelper(message, true);
	            helper.setFrom(from);

	            helper.setTo(to);
	            helper.setSubject(subject);

	            if (cc != null && cc.length > 0) {
	                log.info("MimeMessageHelper CC input: {}", Arrays.toString(cc));
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
	            System.out.println("To Email "+ to);

	            mailSender.send(message);
	            System.out.println("ratnesh email send");
	        } catch (MessagingException | IOException e) {
	            // Handle exceptions
	            System.out.println(("Error sending email "+e));
	        }		
	}
}
