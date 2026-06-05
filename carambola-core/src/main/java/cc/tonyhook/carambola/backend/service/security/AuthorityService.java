package cc.tonyhook.carambola.backend.service.security;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.security.AuthorityRepository;
import cc.tonyhook.carambola.backend.dao.security.RoleRepository;
import cc.tonyhook.carambola.backend.entity.security.Authority;
import cc.tonyhook.carambola.backend.entity.security.Role;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;

    public AuthorityService(AuthorityRepository authorityRepository, RoleRepository roleRepository) {
        this.authorityRepository = authorityRepository;
        this.roleRepository = roleRepository;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public List<Authority> queryAuthorityList(Query query) {
        List<Authority> authorityList = authorityRepository.findAll();

        authorityList.removeIf(authority -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value = authority.getName().toLowerCase();
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

        return authorityList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public List<Authority> getAuthorityList() {
        List<Authority> authorityList = authorityRepository.findAll();

        return authorityList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Authority getAuthority(Integer id) {
        Authority authority = authorityRepository.findById(id).orElse(null);

        return authority;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Authority addAuthority(Authority newAuthority) {
        Authority updatedAuthority = authorityRepository.save(newAuthority);

        return updatedAuthority;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void updateAuthority(Integer id, Authority newAuthority) {
        authorityRepository.save(newAuthority);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void removeAuthority(Integer id) {
        Authority deletedAuthority = authorityRepository.findById(id).orElse(null);

        List<Role> roleList = roleRepository.findAll();
        for (Role role : roleList) {
            if (role.getAuthorities().contains(deletedAuthority)) {
                role.getAuthorities().remove(deletedAuthority);
                roleRepository.save(role);
            }
        }

        authorityRepository.delete(deletedAuthority);
    }

}
