package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;

public interface TrafficControlRepository extends JpaRepository<TrafficControl, Integer> {

    @Query(value = "SELECT * FROM ad_traffic_control WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList", nativeQuery = true)
    List<TrafficControl> findByClientPortInAndVendorPortIn(List<Integer> clientPortIdList, List<Integer> vendorPortIdList);

}
