package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.EventDelivery;

public interface EventDeliveryRepository extends JpaRepository<EventDelivery, Long> {

    EventDelivery findFirstByToken(String token);

}
