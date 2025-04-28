package cc.tonyhook.carambola.backend.dao.ad;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad. Server;

public interface ServerRepository extends JpaRepository< Server, Integer> {

    Server findTopByAddress(String address);

}
