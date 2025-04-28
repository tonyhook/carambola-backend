package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientHour;
import jakarta.transaction.Transactional;

public interface PerformanceClientHourRepository extends JpaRepository<PerformanceClientHour, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ad_performance_client_hour WHERE time BETWEEN :start AND :end", nativeQuery = true)
    void deleteByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_hour WHERE time BETWEEN :start AND :end", nativeQuery = true)
    List<PerformanceClientHour> findByTimeBetween(Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_hour WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port = 0", nativeQuery = true)
    List<PerformanceClientHour> findSummaryByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_hour WHERE client_port IN :clientPortIdList AND time BETWEEN :start AND :end AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientHour> findDetailByClientPortInAndTimeBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_hour WHERE vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0", nativeQuery = true)
    List<PerformanceClientHour> findDetailByVendorPortInAndTimeBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_performance_client_hour WHERE client_port IN :clientPortIdList AND vendor_port IN :vendorPortIdList AND time BETWEEN :start AND :end AND client_port <> 0 AND vendor_port <> 0", nativeQuery = true)
    List<PerformanceClientHour> findDetailByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
