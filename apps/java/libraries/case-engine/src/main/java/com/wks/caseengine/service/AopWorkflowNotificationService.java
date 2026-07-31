package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Stage notifications for the AOP approval workflow.
 *
 * <p>Mirrors the TCS email path (JavaMailSender + Thymeleaf) but is fully
 * separate from it — TCS is untouched. Recipients are resolved from the gate's
 * Keycloak roles (role -> user emails), so "email at every stage to the
 * respective people" needs no per-user configuration: whoever holds the gate
 * role receives it.</p>
 *
 * <p>Email failures are logged and swallowed — a mail outage must never roll
 * back or block an approval transaction.</p>
 */
@Service
@Slf4j
public class AopWorkflowNotificationService {

    public static final String TEMPLATE = "aop-workflow-template";

    /**
     * Optional: Spring only auto-configures a JavaMailSender when spring.mail.* is
     * set. Mail being unconfigured (or Thymeleaf absent) must never stop the API
     * from starting or block an approval — we degrade to logging instead.
     */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private SpringTemplateEngine templateEngine;

    @Autowired
    private KeycloakUserService keycloakUserService;

    @Value("${spring.mail.fromEmail:no-reply@aop.local}")
    private String from;

    /**
     * Resolve the distinct, non-blank email addresses of all users holding any
     * of the given gate roles.
     */
    public List<String> resolveEmailsForRoles(List<String> roles) {
        Set<String> emails = new LinkedHashSet<>();
        if (roles == null) {
            return new ArrayList<>();
        }
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            try {
                for (UserRepresentation user : keycloakUserService.getUsersWithRole(role)) {
                    String email = user.getEmail();
                    if (email != null && !email.isBlank()) {
                        emails.add(email);
                    }
                }
            } catch (Exception ex) {
                log.error("AOP notify: failed to resolve emails for role {}: {}", role, ex.getMessage());
            }
        }
        return new ArrayList<>(emails);
    }

    /**
     * Send a stage notification to everyone holding any of {@code toRoles},
     * optionally cc'ing everyone holding any of {@code ccRoles} (the diagram's
     * "parallel info flow"). No-op if there are no resolvable recipients.
     */
    public void notifyRoles(String subject, List<String> toRoles, List<String> ccRoles,
            Map<String, Object> placeholders) {

        if (mailSender == null || templateEngine == null) {
            log.warn("AOP notify: mail not configured (spring.mail.*) - skipping '{}' for roles {}",
                    subject, toRoles);
            return;
        }

        List<String> to = resolveEmailsForRoles(toRoles);
        if (to.isEmpty()) {
            log.warn("AOP notify: no recipients resolved for roles {}, skipping email", toRoles);
            return;
        }
        List<String> cc = resolveEmailsForRoles(ccRoles);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            if (!cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }

            Context context = new Context();
            context.setVariables(placeholders);
            helper.setText(templateEngine.process(TEMPLATE, context), true);

            mailSender.send(message);
            log.info("AOP notify: sent '{}' to {} (cc {})", subject, to, cc);
        } catch (Exception ex) {
            log.error("AOP notify: failed to send '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
