package com.att.tdp.issueflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// Top level wiring. Turns on:
//  - JPA auditing so @CreatedDate and @LastModifiedDate fill in automatically.
//  - Scheduled tasks for escalation and deny list purge.
//  - Configuration properties scanning.
// The Clock bean is injected wherever we use timestamps so tests can swap in a fixed clock.
@Configuration
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan("com.att.tdp.issueflow")
public class ApplicationConfig {

  @Bean
  @SuppressWarnings("unused")
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  @SuppressWarnings("unused")
  ObjectMapper auditObjectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
