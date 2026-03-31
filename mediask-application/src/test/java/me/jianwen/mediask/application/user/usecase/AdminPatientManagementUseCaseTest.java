package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminPatientsQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
import me.jianwen.mediask.domain.user.port.PasswordHasher;
import org.junit.jupiter.api.Test;

class AdminPatientManagementUseCaseTest {

    @Test
    void create_WhenCommandValid_HashPasswordAndPersistDraft() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        CreateAdminPatientUseCase useCase = new CreateAdminPatientUseCase(writeRepository, rawPassword -> "hash<" + rawPassword + ">");

        AdminPatientDetail result = useCase.handle(new CreateAdminPatientCommand(
                "patient_new", "patient123", "李新患者", "137****1234", "female", LocalDate.of(1995, 6, 1), "A", "Peanut"));

        assertEquals("patient_new", writeRepository.lastCreateDraft.username());
        assertEquals("hash<patient123>", writeRepository.lastCreateDraft.passwordHash());
        assertEquals("female", writeRepository.lastCreateDraft.gender());
        assertEquals(2208L, result.patientId());
    }

    @Test
    void create_WhenPasswordHasEdgeSpaces_PreserveRawPasswordBeforeHashing() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        CapturingPasswordHasher passwordHasher = new CapturingPasswordHasher();
        CreateAdminPatientUseCase useCase = new CreateAdminPatientUseCase(writeRepository, passwordHasher);

        useCase.handle(new CreateAdminPatientCommand(
                "patient_new", "  patient123  ", "李新患者", "137****1234", "female", LocalDate.of(1995, 6, 1), "A", "Peanut"));

        assertEquals("  patient123  ", passwordHasher.lastRawPassword);
        assertEquals("hash<  patient123  >", writeRepository.lastCreateDraft.passwordHash());
    }

    @Test
    void update_WhenCommandValid_PersistUpdateDraft() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        UpdateAdminPatientUseCase useCase = new UpdateAdminPatientUseCase(writeRepository);

        AdminPatientDetail result = useCase.handle(new UpdateAdminPatientCommand(
                2208L, "李修改", "137****9999", "MALE", LocalDate.of(1990, 1, 2), "B", "Dust"));

        assertEquals(2208L, writeRepository.lastUpdatedPatientId);
        assertEquals("李修改", writeRepository.lastUpdateDraft.displayName());
        assertEquals("B", writeRepository.lastUpdateDraft.bloodType());
        assertEquals("李修改", result.displayName());
    }

    @Test
    void detail_WhenPatientMissing_ThrowNotFound() {
        GetAdminPatientDetailUseCase useCase = new GetAdminPatientDetailUseCase(new StubAdminPatientQueryRepository(Optional.empty(), List.of()));

        BizException exception = assertThrows(BizException.class, () -> useCase.handle(new GetAdminPatientDetailQuery(2208L)));

        assertEquals(UserErrorCode.ADMIN_PATIENT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void list_WhenKeywordProvided_DelegateToRepository() {
        ListAdminPatientsUseCase useCase = new ListAdminPatientsUseCase(new StubAdminPatientQueryRepository(
                Optional.of(detail()),
                List.of(new AdminPatientListItem(
                        2208L, 2008L, "P20260008", "patient_new", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "ACTIVE"))));

        List<AdminPatientListItem> result = useCase.handle(new ListAdminPatientsQuery("patient"));

        assertEquals(1, result.size());
        assertEquals("patient_new", result.getFirst().username());
    }

    @Test
    void delete_WhenCommandValid_DeleteByPatientId() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        DeleteAdminPatientUseCase useCase = new DeleteAdminPatientUseCase(writeRepository);

        useCase.handle(new DeleteAdminPatientCommand(2208L));

        assertEquals(2208L, writeRepository.lastDeletedPatientId);
    }

    private static AdminPatientDetail detail() {
        return new AdminPatientDetail(
                2208L,
                2008L,
                "P20260008",
                "patient_new",
                "李新患者",
                "137****1234",
                "FEMALE",
                LocalDate.of(1995, 6, 1),
                "A",
                "Peanut",
                "ACTIVE");
    }

    private static final class StubAdminPatientWriteRepository implements AdminPatientWriteRepository {

        private AdminPatientCreateDraft lastCreateDraft;
        private Long lastUpdatedPatientId;
        private AdminPatientUpdateDraft lastUpdateDraft;
        private Long lastDeletedPatientId;

        @Override
        public AdminPatientDetail create(AdminPatientCreateDraft draft) {
            this.lastCreateDraft = draft;
            return detail();
        }

        @Override
        public AdminPatientDetail update(Long patientId, AdminPatientUpdateDraft draft) {
            this.lastUpdatedPatientId = patientId;
            this.lastUpdateDraft = draft;
            return new AdminPatientDetail(
                    patientId,
                    2008L,
                    "P20260008",
                    "patient_new",
                    draft.displayName(),
                    draft.mobileMasked(),
                    draft.gender(),
                    draft.birthDate(),
                    draft.bloodType(),
                    draft.allergySummary(),
                    "ACTIVE");
        }

        @Override
        public void softDelete(Long patientId) {
            this.lastDeletedPatientId = patientId;
        }
    }

    private static final class CapturingPasswordHasher implements PasswordHasher {

        private String lastRawPassword;

        @Override
        public String hash(CharSequence rawPassword) {
            this.lastRawPassword = rawPassword.toString();
            return "hash<" + rawPassword + ">";
        }
    }

    private record StubAdminPatientQueryRepository(Optional<AdminPatientDetail> detail, List<AdminPatientListItem> items)
            implements AdminPatientQueryRepository {

        @Override
        public List<AdminPatientListItem> listByKeyword(String keyword) {
            return items;
        }

        @Override
        public Optional<AdminPatientDetail> findDetailByPatientId(Long patientId) {
            return detail;
        }
    }
}
