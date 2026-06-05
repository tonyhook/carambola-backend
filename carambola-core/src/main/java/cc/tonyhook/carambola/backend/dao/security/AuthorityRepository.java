package cc.tonyhook.carambola.backend.dao.security;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.security.Authority;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {

}
