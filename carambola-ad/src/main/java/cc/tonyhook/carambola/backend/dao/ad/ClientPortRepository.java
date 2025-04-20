package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;

public interface ClientPortRepository extends JpaRepository<ClientPort, Integer> {

    List<ClientPort> findByClientInOrderByUpdateTimeDesc(List<Client> clientList);
    List<ClientPort> findByClientMediaInOrderByUpdateTimeDesc(List<ClientMedia> clientMediaList);

}
