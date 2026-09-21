package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.EventCallbackUrl;

public interface EventCallbackUrlRepository extends JpaRepository<EventCallbackUrl, Integer> {

}
