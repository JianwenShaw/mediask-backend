package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListAdminPatientsQuery(String keyword) {

    public ListAdminPatientsQuery {
        keyword = ArgumentChecks.blankToNull(keyword);
    }
}
