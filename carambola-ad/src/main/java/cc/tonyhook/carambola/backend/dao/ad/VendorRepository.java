package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Tenant;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {

    List<Vendor> findByTenantOrderByUpdateTimeDesc(Tenant tenant);

}
