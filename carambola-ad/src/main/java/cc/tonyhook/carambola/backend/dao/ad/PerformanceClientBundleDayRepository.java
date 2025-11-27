package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleDay;
import jakarta.transaction.Transactional;

public interface PerformanceClientBundleDayRepository extends JpaRepository<PerformanceClientBundleDay, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_client_bundle_day WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_day WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceClientBundleDay> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_day WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port = 0", nativeQuery = true)
    List<PerformanceClientBundleDay> findSummaryByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_day WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleDay> findDetailByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_day WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleDay> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_day WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND bundle IN :bundleList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleDay> findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, List<String> bundleList, Timestamp start, Timestamp end);

}
