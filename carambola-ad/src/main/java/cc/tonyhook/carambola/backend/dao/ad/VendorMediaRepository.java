package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;

public interface VendorMediaRepository extends JpaRepository<VendorMedia, Integer> {

    @EntityGraph(attributePaths = {"vendor"})
    List<VendorMedia> findByVendorInOrderByUpdateTimeDesc(List<Vendor> vendorList);

    @Query("SELECT MAX(m.updateTime), COUNT(m), MAX(v.updateTime) FROM VendorMedia m JOIN m.vendor v WHERE v.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(m.updateTime), COUNT(m), MAX(v.updateTime) FROM VendorMedia m JOIN m.vendor v")
    List<Object[]> getStamp();

}
