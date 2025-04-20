package cc.tonyhook.carambola.backend.dao.backend;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.backend.Site;

public interface SiteRepository extends JpaRepository<Site, Integer> {

}
