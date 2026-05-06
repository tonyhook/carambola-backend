package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;

public interface ClientMediaRepository extends JpaRepository<ClientMedia, Integer> {

    @EntityGraph(attributePaths = {"client"})
    List<ClientMedia> findByClientInOrderByUpdateTimeDesc(List<Client> clientList);

    @Query("SELECT MAX(m.updateTime), COUNT(m), MAX(c.updateTime) FROM ClientMedia m JOIN m.client c WHERE c.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(m.updateTime), COUNT(m), MAX(c.updateTime) FROM ClientMedia m JOIN m.client c")
    List<Object[]> getStamp();

}
