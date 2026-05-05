package me.jianwen.mediask.application.clinical.command;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePrescriptionItemsCommand(Long encounterId, Long doctorId, List<PrescriptionItemCommand> items) {

    public record PrescriptionItemCommand(
            int sortOrder,
            String drugName,
            String drugSpecification,
            String dosageText,
            String frequencyText,
            String durationText,
            BigDecimal quantity,
            String unit,
            String route) {
    }

    public UpdatePrescriptionItemsCommand {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be null or empty");
        }
    }
}
