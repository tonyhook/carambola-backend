package cc.tonyhook.carambola.backend.dao.security;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.security.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Role findByName(String name);

}
