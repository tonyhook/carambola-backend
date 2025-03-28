package cc.tonyhook.carambola.backend.dao.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.audit.Log;

public interface LogRepository extends JpaRepository<Log, Integer> {

}
