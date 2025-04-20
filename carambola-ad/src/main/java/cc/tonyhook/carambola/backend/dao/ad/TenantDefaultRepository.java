package cc.tonyhook.carambola.backend.dao.ad;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;

public interface TenantDefaultRepository extends JpaRepository<TenantDefault, Integer> {

    TenantDefault findByUsername(String username);

}
