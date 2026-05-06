package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;

public interface ClientPortRepository extends JpaRepository<ClientPort, Integer> {

    @EntityGraph(attributePaths = {"client", "clientMedia"})
    List<ClientPort> findByClientInOrderByUpdateTimeDesc(List<Client> clientList);

    @EntityGraph(attributePaths = {"client", "clientMedia"})
    List<ClientPort> findByClientMediaInOrderByUpdateTimeDesc(List<ClientMedia> clientMediaList);

    @Query("SELECT MAX(p.updateTime), COUNT(p), MAX(c.updateTime), MAX(m.updateTime) FROM ClientPort p JOIN p.client c JOIN p.clientMedia m WHERE c.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(p.updateTime), COUNT(p), MAX(c.updateTime), MAX(m.updateTime) FROM ClientPort p JOIN p.client c JOIN p.clientMedia m")
    List<Object[]> getStamp();

}
