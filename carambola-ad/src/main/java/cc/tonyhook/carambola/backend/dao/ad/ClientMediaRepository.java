package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;

public interface ClientMediaRepository extends JpaRepository<ClientMedia, Integer> {

    List<ClientMedia> findByClientInOrderByUpdateTimeDesc(List<Client> clientList);

}
