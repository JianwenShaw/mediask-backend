package me.jianwen.mediask.application.user.query;

public record GetCurrentUserQuery(Long userId) {

    public GetCurrentUserQuery {
        if (userId == null || userId <= 0L) {
            throw new IllegalArgumentException("userId must be greater than 0");
        }
    }
}
