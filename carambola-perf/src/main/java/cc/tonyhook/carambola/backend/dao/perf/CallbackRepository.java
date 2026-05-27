package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.Callback;

public interface CallbackRepository extends JpaRepository<Callback, Integer> {

}
