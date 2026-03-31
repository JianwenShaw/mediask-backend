package me.jianwen.mediask.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import me.jianwen.mediask.infra.persistence.mapper.AdminPatientRow;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminPatientQueryRepositoryAdapter implements AdminPatientQueryRepository {

    private final PatientProfileMapper patientProfileMapper;

    public AdminPatientQueryRepositoryAdapter(PatientProfileMapper patientProfileMapper) {
        this.patientProfileMapper = patientProfileMapper;
    }

    @Override
    public List<AdminPatientListItem> listByKeyword(String keyword) {
        return patientProfileMapper.selectAdminPatientsByKeyword(keyword).stream()
                .map(this::toListItem)
                .toList();
    }

    @Override
    public Optional<AdminPatientDetail> findDetailByPatientId(Long patientId) {
        return Optional.ofNullable(patientProfileMapper.selectAdminPatientByPatientId(patientId)).map(this::toDetail);
    }

    private AdminPatientListItem toListItem(AdminPatientRow row) {
        return new AdminPatientListItem(
                row.getPatientId(),
                row.getUserId(),
                row.getPatientNo(),
                row.getUsername(),
                row.getDisplayName(),
                row.getMobileMasked(),
                row.getGender(),
                row.getBirthDate(),
                row.getBloodType(),
                row.getAccountStatus());
    }

    private AdminPatientDetail toDetail(AdminPatientRow row) {
        return new AdminPatientDetail(
                row.getPatientId(),
                row.getUserId(),
                row.getPatientNo(),
                row.getUsername(),
                row.getDisplayName(),
                row.getMobileMasked(),
                row.getGender(),
                row.getBirthDate(),
                row.getBloodType(),
                row.getAllergySummary(),
                row.getAccountStatus());
    }
}
