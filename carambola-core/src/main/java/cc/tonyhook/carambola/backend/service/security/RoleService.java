package cc.tonyhook.carambola.backend.service.security;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.security.PermissionRepository;
import cc.tonyhook.carambola.backend.dao.security.RoleRepository;
import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.security.Role;
import cc.tonyhook.carambola.backend.entity.security.User;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class RoleService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(PermissionRepository permissionRepository, RoleRepository roleRepository, UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public List<Role> queryRoleList(Query query) {
        List<Role> roleList = roleRepository.findAll();

        roleList.removeIf(role -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value = role.getName().toLowerCase();
                    }
                    for (String fragment : query.searchValue.split(" ")) {
                        if (value.contains(fragment.toLowerCase())) {
                            return false;
                        }
                    }
                }
                return true;
            }

            return false;
        });

        return roleList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public List<Role> getRoleList() {
        List<Role> roleList = roleRepository.findAll();

        return roleList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Role getRole(Integer id) {
        Role role = roleRepository.findById(id).orElse(null);

        return role;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Role addRole(Role newRole) {
        Role updatedRole = roleRepository.save(newRole);

        return updatedRole;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void updateRole(Integer id, Role newRole) {
        roleRepository.save(newRole);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void removeRole(Integer id) {
        Role deletedRole = roleRepository.findById(id).orElse(null);

        deletedRole.getAuthorities().clear();
        roleRepository.save(deletedRole);

        List<User> userList = userRepository.findAll();
        for (User user : userList) {
            if (user.getRoles().contains(deletedRole)) {
                user.getRoles().remove(deletedRole);
                userRepository.save(user);
            }
        }

        permissionRepository.deleteByRoleId(id);
        roleRepository.delete(deletedRole);
    }

}
