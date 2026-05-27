package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.Event;

public interface EventRepository extends JpaRepository<Event, Integer> {

    public Event findFirstBySerialno(String serialno);

}
