package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientProjectRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientProject;
import jakarta.transaction.Transactional;

@Service
public class ClientProjectService {

    private final ClientProjectRepository clientProjectRepository;

    public ClientProjectService(ClientProjectRepository clientProjectRepository) {
        this.clientProjectRepository = clientProjectRepository;
    }

    public List<ClientProject> getClientProjectList() {
        List<ClientProject> clientProjectList = clientProjectRepository.findAll();

        return clientProjectList;
    }

    public ClientProject getClientProject(Integer id) {
        ClientProject clientProject = clientProjectRepository.findById(id).orElse(null);

        return clientProject;
    }

    public ClientProject addClientProject(ClientProject newClientProject) {
        newClientProject.setCreateTime(new Timestamp(System.currentTimeMillis()));
        newClientProject.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        ClientProject updatedClientProject = clientProjectRepository.save(newClientProject);

        return updatedClientProject;
    }

    public void updateClientProject(Integer id, ClientProject newClientProject) {
        newClientProject.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        clientProjectRepository.save(newClientProject);
    }

    @Transactional
    public void removeClientProject(Integer id) {
        ClientProject deletedClientProject = clientProjectRepository.findById(id).orElse(null);

        clientProjectRepository.delete(deletedClientProject);
    }

}
