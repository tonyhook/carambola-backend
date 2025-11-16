package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleMinute;
import jakarta.transaction.Transactional;

public interface PerformanceVendorBundleMinuteRepository extends JpaRepository<PerformanceVendorBundleMinute, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_vendor_bundle_minute WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_minute WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceVendorBundleMinute> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_minute WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port = 0", nativeQuery = true)
    List<PerformanceVendorBundleMinute> findSummaryByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_minute WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceVendorBundleMinute> findDetailByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_vendor_bundle_minute WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceVendorBundleMinute> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
