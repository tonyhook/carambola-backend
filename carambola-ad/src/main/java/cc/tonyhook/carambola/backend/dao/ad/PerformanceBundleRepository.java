package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceBundle;

public interface PerformanceBundleRepository extends JpaRepository<PerformanceBundle, Integer> {

    List<PerformanceBundle> findByTimeBetween(Timestamp start, Timestamp end);
    List<PerformanceBundle> findByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
