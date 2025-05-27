package cc.tonyhook.carambola.backend.service.ad;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import cc.tonyhook.carambola.backend.dao.ad.TrafficControlRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class TrafficControlService {

    private final TrafficControlRepository trafficControlRepository;

    private final PartnerService partnerService;

    public TrafficControlService(TrafficControlRepository trafficControlRepository, PartnerService partnerService) {
        this.trafficControlRepository = trafficControlRepository;
        this.partnerService = partnerService;
    }

    public List<TrafficControl> getTrafficControlList(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());

        if (query.filter.get("clientPort") != null && query.filter.get("clientPort").contains("-1")) {
            clientPortIdList.add(-1);
        }
        if (query.filter.get("vendorPort") != null && query.filter.get("vendorPort").contains("-1")) {
            vendorPortIdList.add(-1);
        }

        List<TrafficControl> trafficControlList = trafficControlRepository.findByClientPortInAndVendorPortIn(clientPortIdList, vendorPortIdList);

        return trafficControlList;
    }

    public List<TrafficControl> getTrafficControlList(Authentication authentication, Integer clientPortId, Integer vendorPortId) {
        List<TrafficControl> trafficControlList = trafficControlRepository.findByClientPortAndVendorPort(clientPortId, vendorPortId);

        return trafficControlList;
    }

    public TrafficControl getTrafficControl(Integer id) {
        TrafficControl trafficControl = trafficControlRepository.findById(id).orElse(null);

        if (trafficControl != null) {
            return trafficControl;
        } else {
            return null;
        }
    }

    public TrafficControl addTrafficControl(TrafficControl newTrafficControl) {
        if (newTrafficControl != null) {
            TrafficControl updatedTrafficControl = trafficControlRepository.save(newTrafficControl);

            return updatedTrafficControl;
        } else {
            return null;
        }
    }

    public void updateTrafficControl(Integer id, TrafficControl newTrafficControl) {
        if (newTrafficControl != null) {
            trafficControlRepository.save(newTrafficControl);
        }
    }

    @Transactional
    public void removeTrafficControl(TrafficControl targetTrafficControl) {
        if (targetTrafficControl != null) {
            trafficControlRepository.delete(targetTrafficControl);
        }
    }

}
