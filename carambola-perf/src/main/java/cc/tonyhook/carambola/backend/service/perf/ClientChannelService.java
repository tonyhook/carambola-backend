package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.ClientChannelRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import jakarta.transaction.Transactional;

@Service
public class ClientChannelService {

    private final ClientChannelRepository clientChannelRepository;

    public ClientChannelService(ClientChannelRepository clientChannelRepository) {
        this.clientChannelRepository = clientChannelRepository;
    }

    public List<ClientChannel> getClientChannelList() {
        List<ClientChannel> clientChannelList = clientChannelRepository.findAll();

        return clientChannelList;
    }

    public ClientChannel getClientChannel(Integer id) {
        ClientChannel clientChannel = clientChannelRepository.findById(id).orElse(null);

        return clientChannel;
    }

    public ClientChannel getClientChannelByCode(String media, String mediaCode) {
        if (media == null || mediaCode == null) {
            return null;
        }

        ClientChannel clientChannel = clientChannelRepository.findFirstByMediaNameAndMediaCode(media, mediaCode);

        return clientChannel;
    }

    public ClientChannel addClientChannel(ClientChannel newClientChannel) {
        newClientChannel.setCreateTime(new Timestamp(System.currentTimeMillis()));
        newClientChannel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        ClientChannel updatedClientChannel = clientChannelRepository.save(newClientChannel);

        return updatedClientChannel;
    }

    public void updateClientChannel(Integer id, ClientChannel newClientChannel) {
        newClientChannel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        clientChannelRepository.save(newClientChannel);
    }

    @Transactional
    public void removeClientChannel(Integer id) {
        ClientChannel deletedClientChannel = clientChannelRepository.findById(id).orElse(null);

        clientChannelRepository.delete(deletedClientChannel);
    }

}
