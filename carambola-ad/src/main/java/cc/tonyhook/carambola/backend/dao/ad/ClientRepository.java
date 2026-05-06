package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.Tenant;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    List<Client> findByTenantOrderByUpdateTimeDesc(Tenant tenant);
    Client findTopByCode(String code);

    @Query("SELECT MAX(c.updateTime), COUNT(c) FROM Client c WHERE c.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(c.updateTime), COUNT(c) FROM Client c")
    List<Object[]> getStamp();

}
