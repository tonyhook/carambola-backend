package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cc.tonyhook.carambola.backend.entity.ad.Tenant;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {

    List<Vendor> findByTenantOrderByUpdateTimeDesc(Tenant tenant);

    @Query("SELECT MAX(v.updateTime), COUNT(v) FROM Vendor v WHERE v.tenant.id = :tenantId")
    List<Object[]> getStampByTenant(@Param("tenantId") Integer tenantId);

    @Query("SELECT MAX(v.updateTime), COUNT(v) FROM Vendor v")
    List<Object[]> getStamp();

}
