package me.jianwen.mediask.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mediask.security")
public record ApiSecurityProperties(@DefaultValue("false") boolean publicDocsEnabled) {}
