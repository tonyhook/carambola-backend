package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;

public interface ConnectionRepository extends JpaRepository<Connection, Integer> {

    List<Connection> findByClientPort(ClientPort clientPort);
    List<Connection> findByVendorPort(VendorPort vendorPort);

    List<Connection> findByClientPortIn(List<ClientPort> clientPortList);
    List<Connection> findByVendorPortIn(List<VendorPort> vendorPortList);

}
