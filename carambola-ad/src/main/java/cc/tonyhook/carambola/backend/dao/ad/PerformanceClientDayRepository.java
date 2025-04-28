package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientDay;
import jakarta.transaction.Transactional;

public interface PerformanceClientDayRepository extends JpaRepository<PerformanceClientDay, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_client_day WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_day WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceClientDay> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_day WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port = 0", nativeQuery = true)
    List<PerformanceClientDay> findSummaryByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_day WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientDay> findDetailByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_day WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceClientDay> findDetailByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_day WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0 AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientDay> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
