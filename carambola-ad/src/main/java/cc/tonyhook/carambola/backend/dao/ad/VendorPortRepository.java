package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;

public interface VendorPortRepository extends JpaRepository<VendorPort, Integer> {

    List<VendorPort> findByVendorInOrderByUpdateTimeDesc(List<Vendor> vendorList);
    List<VendorPort> findByVendorMediaInOrderByUpdateTimeDesc(List<VendorMedia> vendorMediaList);

}
