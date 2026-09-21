package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.Track;

public interface TrackRepository extends JpaRepository<Track, Integer> {

}
