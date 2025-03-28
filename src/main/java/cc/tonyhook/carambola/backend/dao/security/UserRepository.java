package cc.tonyhook.carambola.backend.dao.security;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.security.User;

public interface UserRepository extends JpaRepository<User, Integer> {

}
