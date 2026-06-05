package cc.tonyhook.carambola.backend.dao.backend;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.backend.Menu;

public interface MenuRepository extends JpaRepository<Menu, Integer> {

}
