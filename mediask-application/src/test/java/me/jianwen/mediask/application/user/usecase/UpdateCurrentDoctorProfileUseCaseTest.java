package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.application.user.command.UpdateCurrentDoctorProfileCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DoctorProfileDraft;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.DoctorProfileWriteRepository;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.Test;

class UpdateCurrentDoctorProfileUseCaseTest {

    @Test
    void handle_WhenAuthenticatedUserIsDoctor_UpdateProfile() {
        StubDoctorProfileWriteRepository writeRepository = new StubDoctorProfileWriteRepository();
        UpdateCurrentDoctorProfileUseCase useCase = new UpdateCurrentDoctorProfileUseCase(
                new StubUserAuthenticationRepository(authenticatedUser(RoleCode.DOCTOR, UserType.DOCTOR)),
                writeRepository);

        useCase.handle(new UpdateCurrentDoctorProfileCommand(2003L, "  Chief Physician  ", "  Experienced  "));

        assertEquals(2003L, writeRepository.lastUserId);
        assertEquals("Chief Physician", writeRepository.lastDraft.professionalTitle());
        assertEquals("Experienced", writeRepository.lastDraft.introductionMasked());
    }

    @Test
    void handle_WhenAuthenticatedUserIsNotDoctor_ThrowRoleMismatch() {
        StubDoctorProfileWriteRepository writeRepository = new StubDoctorProfileWriteRepository();
        UpdateCurrentDoctorProfileUseCase useCase = new UpdateCurrentDoctorProfileUseCase(
                new StubUserAuthenticationRepository(authenticatedUser(RoleCode.PATIENT, UserType.PATIENT)),
                writeRepository);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new UpdateCurrentDoctorProfileCommand(2003L, "Chief Physician", null)));

        assertEquals(UserErrorCode.ROLE_MISMATCH.getCode(), exception.getCode());
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

    private static final class StubDoctorProfileWriteRepository implements DoctorProfileWriteRepository {

        private Long lastUserId;
        private DoctorProfileDraft lastDraft;

        @Override
        public void updateByUserId(Long userId, DoctorProfileDraft draft) {
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
