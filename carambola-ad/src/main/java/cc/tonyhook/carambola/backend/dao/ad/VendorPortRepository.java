package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;

public interface VendorPortRepository extends JpaRepository<VendorPort, Integer> {

    @EntityGraph(attributePaths = {"vendor", "vendorMedia"})
    List<VendorPort> findByVendorInOrderByUpdateTimeDesc(List<Vendor> vendorList);

    @EntityGraph(attributePaths = {"vendor", "vendorMedia"})
    List<VendorPort> findByVendorMediaInOrderByUpdateTimeDesc(List<VendorMedia> vendorMediaList);

    @Query("SELECT MAX(p.updateTime), COUNT(p), MAX(v.updateTime), MAX(m.updateTime) FROM VendorPort p JOIN p.vendor v JOIN p.vendorMedia m WHERE v.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(p.updateTime), COUNT(p), MAX(v.updateTime), MAX(m.updateTime) FROM VendorPort p JOIN p.vendor v JOIN p.vendorMedia m")
    List<Object[]> getStamp();

}
