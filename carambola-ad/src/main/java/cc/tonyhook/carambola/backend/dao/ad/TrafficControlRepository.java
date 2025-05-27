package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;

public interface TrafficControlRepository extends JpaRepository<TrafficControl, Integer> {

    List<TrafficControl> findByClientPortAndVendorPort(Integer clientPortId, Integer vendorPortId);
    List<TrafficControl> findByClientPortInAndVendorPortIn(List<Integer> clientPortIdList, List<Integer> vendorPortIdList);

}
