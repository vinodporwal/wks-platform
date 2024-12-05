package com.wks.caseengine.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;

@Configuration
public class LdapConfig {
	

    @Value("${spring.ldap.url}")
    private String ldapUrl;

    @Value("${spring.ldap.base-dn}")
    private String baseDn;

    @Value("${spring.ldap.username}")
    private String username;

    @Value("${spring.ldap.password}")
    private String password;

    @Value("${ldap.search.base}")
    private String searchBase;

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(baseDn);
        contextSource.setUserDn(username);
        contextSource.setPassword(password);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
    	LdapTemplate temp =  new LdapTemplate(contextSource());
    	return temp;
    }
//	try {
//		temp.afterPropertiesSet(); // Validates connection
//	    System.out.println("LDAP Connection Successful");
//	} catch (Exception e) {
//	    System.out.println("LDAP Connection Failed: " + e.getMessage());
//	}

    // Getter for searchBase if needed in the service
    public String getSearchBase() {
        return searchBase;
    }
}
