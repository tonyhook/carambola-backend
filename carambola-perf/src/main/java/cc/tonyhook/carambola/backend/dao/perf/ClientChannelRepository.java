package cc.tonyhook.carambola.backend.dao.perf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;

public interface ClientChannelRepository extends JpaRepository<ClientChannel, Integer> {

    List<ClientChannel> findByClientOrderByUpdateTimeDesc(Client client);

    List<ClientChannel> findByClientProjectOrderByUpdateTimeDesc(ClientProject clientProject);

    ClientChannel findFirstByMediaNameAndMediaCode(String mediaName, String mediaCode);

    @Query("""
        SELECT cc FROM ClientChannel cc
        WHERE cc.mediaName = :mediaName
            AND cc.mediaCode = :mediaCode
            AND (cc.deleted IS NULL OR cc.deleted = false)
        """)
    ClientChannel findFirstActiveByMediaNameAndMediaCode(
            @Param("mediaName") String mediaName,
            @Param("mediaCode") String mediaCode);

}
