package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    List<Tenant> findAllByOrderByUpdateTimeDesc();

}
