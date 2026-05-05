package me.jianwen.mediask.domain.clinical.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrescriptionOrderTest {

    @Test
    void updateItems_WhenDraft_ReturnsNewInstanceWithUpdatedItems() {
        PrescriptionOrder draft = createDraftPrescription();

        List<PrescriptionItem> newItems = List.of(
                new PrescriptionItem(100L, 0, "新药品", "规格", "用量", "频率", "天数",
                        new BigDecimal("10"), "粒", "口服"));

        PrescriptionOrder updated = draft.updateItems(newItems);

        assertEquals(PrescriptionStatus.DRAFT, updated.prescriptionStatus());
        assertEquals(1, updated.items().size());
        assertEquals("新药品", updated.items().get(0).drugName());
        assertEquals(draft.version(), updated.version());
        assertTrue(updated.updatedAt().isAfter(draft.updatedAt()));
        assertEquals(draft.prescriptionOrderId(), updated.prescriptionOrderId());
        assertEquals(draft.prescriptionNo(), updated.prescriptionNo());
    }

    @Test
    void updateItems_WhenIssued_ThrowsIllegalState() {
        PrescriptionOrder issued = createDraftPrescription().issue();

        List<PrescriptionItem> newItems = List.of(
                new PrescriptionItem(100L, 0, "新药品", null, null, null, null,
                        new BigDecimal("10"), "粒", "口服"));

        assertThrows(IllegalStateException.class, () -> issued.updateItems(newItems));
    }

    @Test
    void updateItems_WhenCancelled_ThrowsIllegalState() {
        PrescriptionOrder cancelled = createDraftPrescription().cancel();

        List<PrescriptionItem> newItems = List.of(
                new PrescriptionItem(100L, 0, "新药品", null, null, null, null,
                        new BigDecimal("10"), "粒", "口服"));

        assertThrows(IllegalStateException.class, () -> cancelled.updateItems(newItems));
    }

    @Test
    void updateItems_WhenEmptyItems_ThrowsIllegalArgument() {
        PrescriptionOrder draft = createDraftPrescription();

        assertThrows(IllegalArgumentException.class, () -> draft.updateItems(List.of()));
    }

    @Test
    void issue_WhenDraft_ReturnsNewInstanceWithIssuedStatus() {
        PrescriptionOrder draft = createDraftPrescription();

        PrescriptionOrder issued = draft.issue();

        assertEquals(PrescriptionStatus.ISSUED, issued.prescriptionStatus());
        assertEquals(draft.version(), issued.version());
        assertTrue(issued.updatedAt().isAfter(draft.updatedAt()));
        assertEquals(draft.prescriptionOrderId(), issued.prescriptionOrderId());
        assertEquals(draft.prescriptionNo(), issued.prescriptionNo());
        assertEquals(draft.items().size(), issued.items().size());
    }

    @Test
    void issue_WhenIssued_ThrowsIllegalState() {
        PrescriptionOrder issued = createDraftPrescription().issue();

        assertThrows(IllegalStateException.class, issued::issue);
    }

    @Test
    void issue_WhenCancelled_ThrowsIllegalState() {
        PrescriptionOrder cancelled = createDraftPrescription().cancel();

        assertThrows(IllegalStateException.class, cancelled::issue);
    }

    @Test
    void cancel_WhenDraft_ReturnsNewInstanceWithCancelledStatus() {
        PrescriptionOrder draft = createDraftPrescription();

        PrescriptionOrder cancelled = draft.cancel();

        assertEquals(PrescriptionStatus.CANCELLED, cancelled.prescriptionStatus());
        assertEquals(draft.version(), cancelled.version());
        assertTrue(cancelled.updatedAt().isAfter(draft.updatedAt()));
    }

    @Test
    void cancel_WhenIssued_ReturnsNewInstanceWithCancelledStatus() {
        PrescriptionOrder issued = createDraftPrescription().issue();

        PrescriptionOrder cancelled = issued.cancel();

        assertEquals(PrescriptionStatus.CANCELLED, cancelled.prescriptionStatus());
        assertEquals(issued.version(), cancelled.version());
    }

    @Test
    void cancel_WhenCancelled_ThrowsIllegalState() {
        PrescriptionOrder cancelled = createDraftPrescription().cancel();

        assertThrows(IllegalStateException.class, cancelled::cancel);
    }

    @Test
    void issue_PreservesItemsAndMetadata() {
        PrescriptionOrder draft = createDraftPrescription();

        PrescriptionOrder issued = draft.issue();

        assertEquals(draft.items(), issued.items());
        assertEquals(draft.prescriptionNo(), issued.prescriptionNo());
        assertEquals(draft.recordId(), issued.recordId());
        assertEquals(draft.encounterId(), issued.encounterId());
        assertEquals(draft.patientId(), issued.patientId());
        assertEquals(draft.doctorId(), issued.doctorId());
        assertEquals(draft.createdAt(), issued.createdAt());
    }

    private PrescriptionOrder createDraftPrescription() {
        List<PrescriptionItem> items = List.of(
                new PrescriptionItem(1L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天",
                        new BigDecimal("30"), "粒", "口服"),
                new PrescriptionItem(2L, 1, "对乙酰氨基酚片", null, "每次1片", "必要时", "3天",
                        new BigDecimal("10"), "片", "口服"));
        return PrescriptionOrder.createDraft("RX123456", 100L, 200L, 300L, 400L, items);
    }
}
