package me.jianwen.mediask.infra.security.authz;

import me.jianwen.mediask.infra.persistence.mapper.DataScopeRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataScopeCustomRuleStartupGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataScopeCustomRuleStartupGuard.class);

    private final DataScopeRuleMapper dataScopeRuleMapper;

    public DataScopeCustomRuleStartupGuard(DataScopeRuleMapper dataScopeRuleMapper) {
        this.dataScopeRuleMapper = dataScopeRuleMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        long customRuleCount = dataScopeRuleMapper.countActiveCustomRules();
        if (customRuleCount <= 0) {
            return;
        }
        log.error(
                "Detected unsupported active CUSTOM data-scope rules during startup, count={}. Failing fast to prevent incorrect authorization behavior.",
                customRuleCount);
        throw new IllegalStateException(
                "Unsupported ACTIVE CUSTOM data-scope rules detected. Disable CUSTOM rules or implement CUSTOM authorization evaluation.");
    }
}
