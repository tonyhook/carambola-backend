package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;

public interface VendorMediaRepository extends JpaRepository<VendorMedia, Integer> {

    List<VendorMedia> findByVendorInOrderByUpdateTimeDesc(List<Vendor> vendorList);

}
