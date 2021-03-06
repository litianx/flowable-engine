package org.flowable.test.spring.boot;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.SecurityAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author Josh Long
 */
public class SecurityAutoConfigurationTest {
    private AnnotationConfigApplicationContext applicationContext;

    @After
    public void close() {
        this.applicationContext.close();
    }

    @Test
    public void userDetailsService() throws Throwable {

        this.applicationContext = new AnnotationConfigApplicationContext();
        this.applicationContext.register(SecurityConfiguration.class);
        this.applicationContext.refresh();
        UserDetailsService userDetailsService = this.applicationContext.getBean(UserDetailsService.class);
        Assert.assertNotNull("the userDetailsService should not be null", userDetailsService);
        Assert.assertEquals("there should only be 1 authority", 1, userDetailsService.loadUserByUsername("jlong2").getAuthorities().size());
        Assert.assertEquals("there should be 2 authorities", 2, userDetailsService.loadUserByUsername("jbarrez2").getAuthorities().size());
    }

    @Configuration
    @Import({ DataSourceAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class,
            SecurityAutoConfiguration.class })
    public static class SecurityConfiguration {

        @Autowired
        private IdentityService identityService;

        protected User user(String userName, String f, String l) {
            User u = identityService.newUser(userName);
            u.setFirstName(f);
            u.setLastName(l);
            u.setPassword("password");
            identityService.saveUser(u);
            return u;
        }

        protected Group group(String groupName) {
            Group group = identityService.newGroup(groupName);
            group.setName(groupName);
            group.setType("security-role");
            identityService.saveGroup(group);
            return group;
        }

        @Bean
        InitializingBean init(
                final IdentityService identityService) {
            return new InitializingBean() {
                @Override
                public void afterPropertiesSet() throws Exception {
                    // We have to use different names than from the CommandLineRunner. The reason is that during
                    // testing we are using an in memory database that is not linked to the application context
                    // but it lives in the JVM

                    // install groups & users
                    Group userGroup = group("user2");
                    Group adminGroup = group("admin2");

                    User joram = user("jbarrez2", "Joram", "Barrez");
                    identityService.createMembership(joram.getId(), userGroup.getId());
                    identityService.createMembership(joram.getId(), adminGroup.getId());

                    User josh = user("jlong2", "Josh", "Long");
                    identityService.createMembership(josh.getId(), userGroup.getId());
                }
            };
        }

    }
}
