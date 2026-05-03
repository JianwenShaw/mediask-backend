package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.user.command.UpdateCurrentPatientProfileCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.PatientProfileDraft;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.PatientProfileWriteRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.Test;

class UpdateCurrentPatientProfileUseCaseTest {

    @Test
    void handle_WhenAuthenticatedUserIsPatient_UpdateProfile() {
        StubPatientProfileWriteRepository writeRepository = new StubPatientProfileWriteRepository();
        UpdateCurrentPatientProfileUseCase useCase = new UpdateCurrentPatientProfileUseCase(
                new StubUserAuthenticationRepository(authenticatedUser(RoleCode.PATIENT, UserType.PATIENT)),
                writeRepository,
                TestAuditSupport.auditTrailService());

        useCase.handle(
                new UpdateCurrentPatientProfileCommand(2003L, "  FEMALE  ", LocalDate.of(1992, 5, 1), "  A  ", "  Peanut  "),
                TestAuditSupport.auditContext());

        assertEquals(2003L, writeRepository.lastUserId);
        assertEquals("FEMALE", writeRepository.lastDraft.gender());
        assertEquals("A", writeRepository.lastDraft.bloodType());
        assertEquals("Peanut", writeRepository.lastDraft.allergySummary());
    }

    @Test
    void handle_WhenAuthenticatedUserIsNotPatient_ThrowRoleMismatch() {
        StubPatientProfileWriteRepository writeRepository = new StubPatientProfileWriteRepository();
        UpdateCurrentPatientProfileUseCase useCase = new UpdateCurrentPatientProfileUseCase(
                new StubUserAuthenticationRepository(authenticatedUser(RoleCode.DOCTOR, UserType.DOCTOR)),
                writeRepository,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(
                        new UpdateCurrentPatientProfileCommand(2003L, "FEMALE", null, "A", null),
                        TestAuditSupport.auditContext()));

        assertEquals(UserErrorCode.ROLE_MISMATCH.getCode(), exception.getCode());
        assertNull(writeRepository.lastDraft);
    }

    @Test
    void handle_WhenGenderIsInvalid_ThrowIllegalArgumentExceptionAndDoNotWrite() {
        StubPatientProfileWriteRepository writeRepository = new StubPatientProfileWriteRepository();
        UpdateCurrentPatientProfileUseCase useCase = new UpdateCurrentPatientProfileUseCase(
                new StubUserAuthenticationRepository(authenticatedUser(RoleCode.PATIENT, UserType.PATIENT)),
                writeRepository,
                TestAuditSupport.auditTrailService());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.handle(
                        new UpdateCurrentPatientProfileCommand(2003L, "UNKNOWN", null, null, null),
                        TestAuditSupport.auditContext()));

        assertEquals("gender must be one of MALE, FEMALE, OTHER", exception.getMessage());
        assertNull(writeRepository.lastDraft);
    }

    private static AuthenticatedUser authenticatedUser(RoleCode roleCode, UserType userType) {
        return new AuthenticatedUser(
                2003L,
                "user_2003",
                "User 2003",
                userType,
                new LinkedHashSet<>(Set.of(roleCode)),
                Set.of(),
                Set.<DataScopeRule>of(),
                roleCode == RoleCode.PATIENT ? 2201L : null,
                roleCode == RoleCode.DOCTOR ? 3301L : null,
                null);
    }

    private static final class StubPatientProfileWriteRepository implements PatientProfileWriteRepository {

        private Long lastUserId;
        private PatientProfileDraft lastDraft;

        @Override
        public void updateByUserId(Long userId, PatientProfileDraft draft) {
            this.lastUserId = userId;
            this.lastDraft = draft;
        }
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser authenticatedUser;

        private StubUserAuthenticationRepository(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public Optional<LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException("not needed for update profile tests");
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            return Optional.ofNullable(authenticatedUser);
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException("not needed for update profile tests");
        }
    }
}
