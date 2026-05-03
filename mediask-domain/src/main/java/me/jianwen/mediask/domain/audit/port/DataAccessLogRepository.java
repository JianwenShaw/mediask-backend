package me.jianwen.mediask.domain.audit.port;

import me.jianwen.mediask.domain.audit.model.DataAccessLogRecord;

public interface DataAccessLogRepository {

    void save(DataAccessLogRecord record);
}
