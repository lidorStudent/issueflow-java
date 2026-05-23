package com.att.tdp.issueflow.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "issueflow.attachments")
public record AttachmentProperties(
    @NotBlank String dir, @Positive long maxSizeBytes, @NotEmpty Set<String> allowedMimeTypes) {}
