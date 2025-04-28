package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleQuarter;
import jakarta.transaction.Transactional;

public interface PerformanceVendorBundleQuarterRepository extends JpaRepository<PerformanceVendorBundleQuarter, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_vendor_bundle_quarter WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_quarter WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceVendorBundleQuarter> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_quarter WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port = 0", nativeQuery = true)
    List<PerformanceVendorBundleQuarter> findSummaryByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_quarter WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceVendorBundleQuarter> findDetailByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_quarter WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceVendorBundleQuarter> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
