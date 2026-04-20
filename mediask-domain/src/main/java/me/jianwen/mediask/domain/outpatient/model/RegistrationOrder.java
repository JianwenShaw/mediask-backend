package me.jianwen.mediask.domain.outpatient.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;

public final class RegistrationOrder {

    private final Long registrationId;
    private final String orderNo;
    private final Long patientId;
    private final Long doctorId;
    private final Long departmentId;
    private final Long sessionId;
    private final Long slotId;
    private final Long sourceAiSessionId;
    private final RegistrationStatus status;
    private final BigDecimal fee;
    private final OffsetDateTime cancelledAt;
    private final String cancellationReason;

    private RegistrationOrder(
            Long registrationId,
            String orderNo,
            Long patientId,
            Long doctorId,
            Long departmentId,
            Long sessionId,
            Long slotId,
            Long sourceAiSessionId,
            RegistrationStatus status,
            BigDecimal fee,
            OffsetDateTime cancelledAt,
            String cancellationReason) {
        this.registrationId = registrationId;
        this.orderNo = orderNo;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.departmentId = departmentId;
        this.sessionId = sessionId;
        this.slotId = slotId;
        this.sourceAiSessionId = sourceAiSessionId;
        this.status = status;
        this.fee = fee;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
    }

    public static RegistrationOrder createPendingPayment(
            Long patientId,
            Long doctorId,
            Long departmentId,
            Long sessionId,
            Long slotId,
            Long sourceAiSessionId,
            BigDecimal fee) {
        Long registrationId = SnowflakeIdGenerator.nextId();
        return new RegistrationOrder(
                registrationId,
                "REG" + registrationId,
                patientId,
                doctorId,
                departmentId,
                sessionId,
                slotId,
                sourceAiSessionId,
                RegistrationStatus.PENDING_PAYMENT,
                fee,
                null,
                null);
    }

    public static RegistrationOrder reconstitute(
            Long registrationId,
            String orderNo,
            Long patientId,
            Long doctorId,
            Long departmentId,
            Long sessionId,
            Long slotId,
            Long sourceAiSessionId,
            RegistrationStatus status,
            BigDecimal fee,
            OffsetDateTime cancelledAt,
            String cancellationReason) {
        return new RegistrationOrder(
                registrationId,
                orderNo,
                patientId,
                doctorId,
                departmentId,
                sessionId,
                slotId,
                sourceAiSessionId,
                status,
                fee,
                cancelledAt,
                cancellationReason);
    }

    public RegistrationOrder cancel(OffsetDateTime cancelledAt) {
        if (status != RegistrationStatus.PENDING_PAYMENT && status != RegistrationStatus.CONFIRMED) {
            throw new BizException(OutpatientErrorCode.INVALID_STATUS_TRANSITION);
        }
        return new RegistrationOrder(
                registrationId,
                orderNo,
                patientId,
                doctorId,
                departmentId,
                sessionId,
                slotId,
                sourceAiSessionId,
                RegistrationStatus.CANCELLED,
                fee,
                cancelledAt,
                cancellationReason);
    }

    public Long registrationId() {
        return registrationId;
    }

    public String orderNo() {
        return orderNo;
    }

    public Long patientId() {
        return patientId;
    }

    public Long doctorId() {
        return doctorId;
    }

    public Long departmentId() {
        return departmentId;
    }

    public Long sessionId() {
        return sessionId;
    }

    public Long slotId() {
        return slotId;
    }

    public Long sourceAiSessionId() {
        return sourceAiSessionId;
    }

    public RegistrationStatus status() {
        return status;
    }

    public BigDecimal fee() {
        return fee;
    }

    public OffsetDateTime cancelledAt() {
        return cancelledAt;
    }

    public String cancellationReason() {
        return cancellationReason;
    }
}
