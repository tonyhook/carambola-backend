package cc.tonyhook.carambola.backend.dao.perf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.Client;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;

public interface ClientProjectRepository extends JpaRepository<ClientProject, Integer> {

    List<ClientProject> findByClientOrderByUpdateTimeDesc(Client client);

}
