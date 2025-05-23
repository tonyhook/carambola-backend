package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cc.tonyhook.carambola.backend.entity.ad.Medium;

public interface MediumRepository extends JpaRepository<Medium, Integer> {

    List<Medium> findByClientPortInAndDateBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    List<Medium> findByVendorPortInAndDateBetween(List<Integer> vendorPortIdList, Timestamp start, Timestamp end);
    @Query(value = "SELECT * FROM ad_medium WHERE (client_port IN :clientPortIdList OR vendor_port IN :vendorPortIdList) AND date BETWEEN :start AND :end", nativeQuery = true)
    List<Medium> findByClientPortInOrVendorPortInAndDateBetween(List<Integer> clientPortIdList, List<Integer> vendorPortIdList, Timestamp start, Timestamp end);

}
