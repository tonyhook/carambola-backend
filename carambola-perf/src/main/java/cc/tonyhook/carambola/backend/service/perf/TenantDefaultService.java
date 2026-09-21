package cc.tonyhook.carambola.backend.service.perf;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.TenantDefaultRepository;
import cc.tonyhook.carambola.backend.entity.perf.Tenant;
import cc.tonyhook.carambola.backend.entity.perf.TenantDefault;

@Service
public class TenantDefaultService {

    private final TenantService tenantService;

    private final TenantDefaultRepository tenantDefaultRepository;

    public TenantDefaultService(TenantService tenantService, TenantDefaultRepository tenantDefaultRepository) {
        this.tenantService = tenantService;
        this.tenantDefaultRepository = tenantDefaultRepository;
    }

    public TenantDefault getTenantDefault(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        TenantDefault tenantDefault = tenantDefaultRepository.findByUsername(username);

        if (tenantDefault == null) {
            List<Tenant> tenantList = tenantService.getTenantList(authentication);
            if (tenantList == null || tenantList.isEmpty()) {
                return null;
            }
            tenantDefault = new TenantDefault();
            tenantDefault.setUsername(username);
            tenantDefault.setTenant(tenantList.get(0));
            tenantDefaultRepository.save(tenantDefault);
        }

        return tenantDefault;
    }

    public void updateTenantDefault(Authentication authentication, Tenant tenant) {
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        TenantDefault tenantDefault = tenantDefaultRepository.findByUsername(username);
        if (tenantDefault == null) {
            tenantDefault = new TenantDefault();
            tenantDefault.setUsername(username);
        }
        tenantDefault.setTenant(tenant);
        tenantDefaultRepository.save(tenantDefault);
    }

}
