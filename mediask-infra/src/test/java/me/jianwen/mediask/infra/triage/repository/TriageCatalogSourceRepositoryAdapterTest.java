package me.jianwen.mediask.infra.triage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import org.junit.jupiter.api.Test;

class TriageCatalogSourceRepositoryAdapterTest {

    @Test
    void loadCandidates_WhenDepartmentsExist_ReturnsMinimalClinicalDepartmentProjection() {
        DepartmentDO department = new DepartmentDO();
        department.setId(3101L);
        department.setName("神经内科");
        department.setSortOrder(10);
        FakeDepartmentMapper mapper = new FakeDepartmentMapper(List.of(department));
        TriageCatalogSourceRepositoryAdapter adapter = new TriageCatalogSourceRepositoryAdapter(mapper.proxy());

        List<DepartmentCandidate> candidates = adapter.loadCandidates("default");

        assertEquals(1, candidates.size());
        DepartmentCandidate candidate = candidates.get(0);
        assertEquals(3101L, candidate.departmentId());
        assertEquals("神经内科", candidate.departmentName());
        assertEquals("神经内科相关问题优先考虑", candidate.routingHint());
        assertTrue(candidate.aliases().isEmpty());
        assertEquals(10, candidate.sortOrder());

        assertTrue(mapper.selectListCalled);
    }

    private static final class FakeDepartmentMapper {

        private final List<DepartmentDO> rows;
        private boolean selectListCalled;

        private FakeDepartmentMapper(List<DepartmentDO> rows) {
            this.rows = rows;
        }

        private DepartmentMapper proxy() {
            Object proxy = Proxy.newProxyInstance(
                    DepartmentMapper.class.getClassLoader(),
                    new Class<?>[] {DepartmentMapper.class},
                    (instance, method, args) -> switch (method.getName()) {
                        case "selectList" -> {
                            selectListCalled = true;
                            yield rows;
                        }
                        case "equals" -> instance == args[0];
                        case "hashCode" -> System.identityHashCode(instance);
                        case "toString" -> "FakeDepartmentMapper";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            return DepartmentMapper.class.cast(proxy);
        }
    }
}
