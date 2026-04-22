package me.jianwen.mediask.api.dto;

import java.math.BigDecimal;
import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record CreatePrescriptionRequest(Long encounterId, List<PrescriptionItemRequest> items) {

    public CreatePrescriptionRequest {
        encounterId = ArgumentChecks.requireNonNull(encounterId, "encounterId");
        items = ArgumentChecks.requireNonNull(items, "items");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }

    public record PrescriptionItemRequest(
            Integer sortOrder,
            String drugName,
            String drugSpecification,
            String dosageText,
            String frequencyText,
            String durationText,
            BigDecimal quantity,
            String unit,
            String route) {

        public PrescriptionItemRequest {
            sortOrder = ArgumentChecks.requireNonNull(sortOrder, "sortOrder");
            if (sortOrder < 0) {
                throw new IllegalArgumentException("sortOrder cannot be negative");
            }
            drugName = ArgumentChecks.requireNonBlank(drugName, "drugName");
            quantity = ArgumentChecks.requireNonNull(quantity, "quantity");
            if (quantity.signum() < 0) {
                throw new IllegalArgumentException("quantity cannot be negative");
            }
            drugSpecification = ArgumentChecks.blankToNull(drugSpecification);
            dosageText = ArgumentChecks.blankToNull(dosageText);
            frequencyText = ArgumentChecks.blankToNull(frequencyText);
            durationText = ArgumentChecks.blankToNull(durationText);
            unit = ArgumentChecks.blankToNull(unit);
            route = ArgumentChecks.blankToNull(route);
        }
    }
}
