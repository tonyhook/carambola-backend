package cc.tonyhook.carambola.backend.dao.perf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    List<Tenant> findAllByOrderByUpdateTimeDesc();

}
