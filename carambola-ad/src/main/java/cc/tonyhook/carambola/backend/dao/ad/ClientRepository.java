package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.Tenant;

public interface ClientRepository extends JpaRepository<Client, Integer> {

    List<Client> findByTenantOrderByUpdateTimeDesc(Tenant tenant);

}
