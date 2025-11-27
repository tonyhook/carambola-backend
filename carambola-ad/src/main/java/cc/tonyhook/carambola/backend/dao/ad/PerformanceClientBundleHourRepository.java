package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleHour;
import jakarta.transaction.Transactional;

public interface PerformanceClientBundleHourRepository extends JpaRepository<PerformanceClientBundleHour, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_client_bundle_hour WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_hour WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceClientBundleHour> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_hour WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port = 0", nativeQuery = true)
    List<PerformanceClientBundleHour> findSummaryByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_hour WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleHour> findDetailByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_hour WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleHour> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_bundle_hour WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND bundle IN :bundleList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientBundleHour> findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, List<String> bundleList, Timestamp start, Timestamp end);

}
