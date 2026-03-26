package me.jianwen.mediask.worker.config;

import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.authz.ResourceAccessResolverPort;
import me.jianwen.mediask.application.authz.ResourceReferenceAssemblerPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthzModuleConfig {

    @Bean
    public AuthorizationDecisionService authorizationDecisionService(
            ObjectProvider<ResourceReferenceAssemblerPort> resourceReferenceAssemblers,
            ObjectProvider<ResourceAccessResolverPort> resourceAccessResolvers) {
        return new AuthorizationDecisionService(
                resourceReferenceAssemblers.orderedStream().toList(),
                resourceAccessResolvers.orderedStream().toList());
    }
}
