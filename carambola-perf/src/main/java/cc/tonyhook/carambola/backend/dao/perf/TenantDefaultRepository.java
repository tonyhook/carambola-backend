package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.TenantDefault;

public interface TenantDefaultRepository extends JpaRepository<TenantDefault, Integer> {

    TenantDefault findByUsername(String username);

}
