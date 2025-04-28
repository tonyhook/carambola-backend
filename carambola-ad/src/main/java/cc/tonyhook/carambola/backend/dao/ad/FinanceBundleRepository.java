package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.FinanceBundle;

public interface FinanceBundleRepository extends JpaRepository<FinanceBundle, Integer> {

    List<FinanceBundle> findByTimeBetween(Timestamp start, Timestamp end);
    List<FinanceBundle> findByClientPortInAndVendorPortInAndTimeBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
