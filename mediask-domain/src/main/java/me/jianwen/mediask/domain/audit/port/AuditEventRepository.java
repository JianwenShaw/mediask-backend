package me.jianwen.mediask.domain.audit.port;

import me.jianwen.mediask.domain.audit.model.AuditEventRecord;

public interface AuditEventRepository {

    void save(AuditEventRecord record);
}
