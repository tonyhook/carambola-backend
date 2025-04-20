package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.VendorRepository;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class VendorService {

    private final AuthenticationService authenticationService;
    private final KeyService keyService;
    private final TenantDefaultService tenantDefaultService;
    private final VendorMediaService vendorMediaService;

    private final VendorRepository vendorRepository;

    public VendorService(
            AuthenticationService authenticationService,
            KeyService keyService,
            TenantDefaultService tenantDefaultService,
            VendorMediaService vendorMediaService,
            VendorRepository vendorRepository
    ) {
        this.authenticationService = authenticationService;
        this.keyService = keyService;
        this.tenantDefaultService = tenantDefaultService;
        this.vendorMediaService = vendorMediaService;
        this.vendorRepository = vendorRepository;
    }

    public List<Vendor> queryVendorList(Authentication authentication, Query query) {
        List<Vendor> qualifiedVendorList = new ArrayList<Vendor>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Vendor> vendorList;
        if (tenantDefault == null) {
            vendorList = vendorRepository.findAll();
        } else {
            vendorList = vendorRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
        }

        for (Vendor vendor : vendorList) {
            if (authenticationService.hasAccess(authentication, vendor)) {
                qualifiedVendorList.add(vendor);
            }
        }

        qualifiedVendorList.removeIf(vendor -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("mode")) {
                    List<String> modeList = query.filter.get(key);
                    Boolean matched = false;
                    for (String mode : modeList) {
                        if (vendor.getMode().toString().equals(mode.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
            }

            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += vendor.getName().toLowerCase();
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

        return qualifiedVendorList;
    }

    public List<Vendor> getVendorList(Authentication authentication) {
        List<Vendor> qualifiedVendorList = new ArrayList<Vendor>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Vendor> vendorList;
        if (tenantDefault == null) {
            vendorList = vendorRepository.findAll();
        } else {
            vendorList = vendorRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
        }

        for (Vendor vendor : vendorList) {
            if (authenticationService.hasAccess(authentication, vendor)) {
                qualifiedVendorList.add(vendor);
            }
        }

        return qualifiedVendorList;
    }

    public Vendor getVendor(Authentication authentication, Integer id) {
        Vendor vendor = vendorRepository.findById(id).orElse(null);

        if (vendor != null && authenticationService.hasAccess(authentication, vendor)) {
            return vendor;
        } else {
            return null;
        }
    }

    public Vendor addVendor(Authentication authentication, Vendor newVendor) {
        if (newVendor != null
            && authenticationService.hasAccess(authentication, newVendor.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            newVendor.setEkey(keyService.generateKey());
            newVendor.setIkey(keyService.generateKey());

            newVendor.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newVendor.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Vendor updatedVendor = vendorRepository.save(newVendor);

            return updatedVendor;
        } else {
            return null;
        }
    }

    public Vendor updateVendor(Authentication authentication, Vendor targetVendor, Vendor newVendor) {
        if (targetVendor != null
            && newVendor != null
            && authenticationService.hasAccess(authentication, targetVendor.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            if (targetVendor.getEkey().isEmpty()) {
                targetVendor.setEkey(keyService.generateKey());
            }
            if (targetVendor.getIkey().isEmpty()) {
                targetVendor.setIkey(keyService.generateKey());
            }
            newVendor.setEkey(targetVendor.getEkey());
            newVendor.setIkey(targetVendor.getIkey());

            newVendor.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            Vendor updatedVendor = vendorRepository.save(newVendor);

            return updatedVendor;
        } else {
            return null;
        }
    }

    @Transactional
    public Vendor removeVendor(Authentication authentication, Vendor targetVendor) {
        if (targetVendor != null
            && authenticationService.hasAccess(authentication, targetVendor.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)) {
            targetVendor.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            targetVendor.setDeleted(true);

            for (VendorMedia vendorMedia : targetVendor.getVendorMedia()) {
                vendorMediaService.removeVendorMedia(authentication, vendorMedia);
            }

            return targetVendor;
        } else {
            return null;
        }
    }

}
