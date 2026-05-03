package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminPatientsQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.audit.model.AuditEventRecord;
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
        CreateAdminPatientUseCase useCase = new CreateAdminPatientUseCase(
                writeRepository,
                rawPassword -> "hash<" + rawPassword + ">",
                TestAuditSupport.auditTrailService());

        AdminPatientDetail result = useCase.handle(
                new CreateAdminPatientCommand(
                        "patient_new", "patient123", "李新患者", "137****1234", "female", LocalDate.of(1995, 6, 1), "A", "Peanut"),
                TestAuditSupport.auditContext());

        assertEquals("patient_new", writeRepository.lastCreateDraft.username());
        assertEquals("hash<patient123>", writeRepository.lastCreateDraft.passwordHash());
        assertEquals("female", writeRepository.lastCreateDraft.gender());
        assertEquals(2208L, result.patientId());
    }

    @Test
    void create_WhenPasswordHasEdgeSpaces_PreserveRawPasswordBeforeHashing() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        CapturingPasswordHasher passwordHasher = new CapturingPasswordHasher();
        CreateAdminPatientUseCase useCase =
                new CreateAdminPatientUseCase(writeRepository, passwordHasher, TestAuditSupport.auditTrailService());

        useCase.handle(
                new CreateAdminPatientCommand(
                        "patient_new", "  patient123  ", "李新患者", "137****1234", "female", LocalDate.of(1995, 6, 1), "A", "Peanut"),
                TestAuditSupport.auditContext());

        assertEquals("  patient123  ", passwordHasher.lastRawPassword);
        assertEquals("hash<  patient123  >", writeRepository.lastCreateDraft.passwordHash());
    }

    @Test
    void update_WhenCommandValid_PersistUpdateDraft() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        UpdateAdminPatientUseCase useCase =
                new UpdateAdminPatientUseCase(writeRepository, TestAuditSupport.auditTrailService());

        AdminPatientDetail result = useCase.handle(
                new UpdateAdminPatientCommand(2208L, "李修改", "137****9999", "MALE", LocalDate.of(1990, 1, 2), "B", "Dust"),
                TestAuditSupport.auditContext());

        assertEquals(2208L, writeRepository.lastUpdatedPatientId);
        assertEquals("李修改", writeRepository.lastUpdateDraft.displayName());
        assertEquals("B", writeRepository.lastUpdateDraft.bloodType());
        assertEquals("李修改", result.displayName());
    }

    @Test
    void detail_WhenPatientMissing_ThrowNotFound() {
        GetAdminPatientDetailUseCase useCase = new GetAdminPatientDetailUseCase(
                new StubAdminPatientQueryRepository(Optional.empty(), List.of()), TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new GetAdminPatientDetailQuery(2208L), TestAuditSupport.auditContext()));

        assertEquals(UserErrorCode.ADMIN_PATIENT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void list_WhenKeywordProvided_DelegateToRepository() {
        StubAdminPatientQueryRepository queryRepository = new StubAdminPatientQueryRepository(
                Optional.of(detail()),
                List.of(new AdminPatientListItem(
                        2208L, 2008L, "P20260008", "patient_new", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "ACTIVE")));
        ListAdminPatientsUseCase useCase = new ListAdminPatientsUseCase(queryRepository);

        PageData<AdminPatientListItem> result = useCase.handle(ListAdminPatientsQuery.page("patient", 1L, 20L));

        assertEquals(1, result.items().size());
        assertEquals("patient_new", result.items().getFirst().username());
        assertEquals("patient", queryRepository.lastKeyword);
        assertEquals(1L, queryRepository.lastPageQuery.pageNum());
        assertEquals(20L, queryRepository.lastPageQuery.pageSize());
    }

    @Test
    void listQueryPage_WhenPagingMissing_UseDefaults() {
        ListAdminPatientsQuery query = ListAdminPatientsQuery.page("patient", null, null);

        assertEquals("patient", query.keyword());
        assertEquals(PageQuery.DEFAULT_PAGE_NUM, query.pageQuery().pageNum());
        assertEquals(PageQuery.DEFAULT_PAGE_SIZE, query.pageQuery().pageSize());
    }

    @Test
    void listQueryPage_WhenPageNumInvalid_ThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ListAdminPatientsQuery.page("patient", 0L, 20L));
        assertThrows(
                IllegalArgumentException.class,
                () -> ListAdminPatientsQuery.page("patient", PageQuery.MAX_PAGE_NUM + 1L, 20L));
    }

    @Test
    void listQueryPage_WhenPageSizeInvalid_ThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ListAdminPatientsQuery.page("patient", 1L, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> ListAdminPatientsQuery.page("patient", 1L, PageQuery.MAX_PAGE_SIZE + 1L));
    }

    @Test
    void delete_WhenCommandValid_DeleteByPatientId() {
        StubAdminPatientWriteRepository writeRepository = new StubAdminPatientWriteRepository();
        StubAdminPatientQueryRepository queryRepository =
                new StubAdminPatientQueryRepository(Optional.of(detail()), List.of());
        List<AuditEventRecord> auditEvents = new ArrayList<>();
        AuditTrailService auditTrailService = new AuditTrailService(
                new RecordAuditEventUseCase(auditEvents::add),
                new RecordDataAccessLogUseCase(record -> {}));
        DeleteAdminPatientUseCase useCase =
                new DeleteAdminPatientUseCase(queryRepository, writeRepository, auditTrailService);

        useCase.handle(new DeleteAdminPatientCommand(2208L), TestAuditSupport.auditContext());

        assertEquals(2208L, writeRepository.lastDeletedPatientId);
        assertEquals(2008L, auditEvents.getLast().patientUserId());
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

    private static final class StubAdminPatientQueryRepository implements AdminPatientQueryRepository {

        private final Optional<AdminPatientDetail> detail;
        private final List<AdminPatientListItem> items;
        private String lastKeyword;
        private PageQuery lastPageQuery;

        private StubAdminPatientQueryRepository(Optional<AdminPatientDetail> detail, List<AdminPatientListItem> items) {
            this.detail = detail;
            this.items = items;
        }

        @Override
        public PageData<AdminPatientListItem> pageByKeyword(String keyword, PageQuery pageQuery) {
            this.lastKeyword = keyword;
            this.lastPageQuery = pageQuery;
            return new PageData<>(items, pageQuery.pageNum(), pageQuery.pageSize(), items.size(), items.isEmpty() ? 0 : 1, false);
        }

        @Override
        public Optional<AdminPatientDetail> findDetailByPatientId(Long patientId) {
            return detail;
        }
    }
}
