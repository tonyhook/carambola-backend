package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientRepository;
import cc.tonyhook.carambola.backend.entity.perf.Client;
import jakarta.transaction.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> getClientList() {
        List<Client> clientList = clientRepository.findAll();

        return clientList;
    }

    public Client getClient(Integer id) {
        Client client = clientRepository.findById(id).orElse(null);

        return client;
    }

    public Client addClient(Client newClient) {
        newClient.setCreateTime(new Timestamp(System.currentTimeMillis()));
        newClient.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        Client updatedClient = clientRepository.save(newClient);

        return updatedClient;
    }

    public void updateClient(Integer id, Client newClient) {
        newClient.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        clientRepository.save(newClient);
    }

    @Transactional
    public void removeClient(Integer id) {
        Client deletedClient = clientRepository.findById(id).orElse(null);

        clientRepository.delete(deletedClient);
    }

}
