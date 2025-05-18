package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Sign;

public interface SignRepository extends JpaRepository<Sign, Integer> {

    List<Sign> findByVendorPortInAndDateBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    List<Sign> findByVendorPortInAndStatusAndDateBetween(List<Integer> vendorPortIdList, Integer status, Timestamp start, Timestamp end);

}
