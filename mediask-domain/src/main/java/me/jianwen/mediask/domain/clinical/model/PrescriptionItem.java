package me.jianwen.mediask.domain.clinical.model;

import java.math.BigDecimal;

public record PrescriptionItem(
        Long itemId,
        int sortOrder,
        String drugName,
        String drugSpecification,
        String dosageText,
        String frequencyText,
        String durationText,
        BigDecimal quantity,
        String unit,
        String route) {

    public PrescriptionItem {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId cannot be null");
        }
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder cannot be negative");
        }
        if (drugName == null || drugName.isBlank()) {
            throw new IllegalArgumentException("drugName cannot be null or blank");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("quantity cannot be null");
        }
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
    }
}
