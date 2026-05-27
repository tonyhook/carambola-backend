package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;

public interface ClientChannelRepository extends JpaRepository<ClientChannel, Integer> {

    ClientChannel findFirstByMediaNameAndMediaCode(String mediaName, String mediaCode);

}
