package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.VendorMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorRepository;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class VendorMediaService {

    private final AuthenticationService authenticationService;
    private final TenantDefaultService tenantDefaultService;
    private final VendorPortService vendorPortService;

    private final VendorRepository vendorRepository;
    private final VendorMediaRepository vendorMediaRepository;

    public VendorMediaService(
            AuthenticationService authenticationService,
            TenantDefaultService tenantDefaultService,
            VendorPortService vendorPortService,
            VendorRepository vendorRepository,
            VendorMediaRepository vendorMediaRepository
    ) {
        this.authenticationService = authenticationService;
        this.tenantDefaultService = tenantDefaultService;
        this.vendorPortService = vendorPortService;
        this.vendorRepository = vendorRepository;
        this.vendorMediaRepository = vendorMediaRepository;
    }

    public List<VendorMedia> queryVendorMediaList(Authentication authentication, Query query) {
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
                if (key.equals("vendorMode")) {
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

            return false;
        });

        List<VendorMedia> vendorMediaList = vendorMediaRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        vendorMediaList.removeIf(vendorMedia -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("vendor")) {
                    List<String> vendorIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorId : vendorIdList) {
                        if (vendorMedia.getVendor().getId().toString().equals(vendorId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("platform")) {
                    List<String> platformList = query.filter.get(key);
                    Boolean matched = false;
                    for (String platform : platformList) {
                        if (vendorMedia.getPlatform().toLowerCase().equals(platform.toLowerCase())) {
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
                        value += vendorMedia.getName().toLowerCase();
                    }
                    if (key.equals("apppackage")) {
                        if (vendorMedia.getApppackage() != null) {
                            value += vendorMedia.getApppackage().toLowerCase();
                        }
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

        return vendorMediaList;
    }

    public List<VendorMedia> getVendorMediaList(Authentication authentication) {
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

        List<VendorMedia> vendorMediaList = vendorMediaRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        return vendorMediaList;
    }

    public List<VendorMedia> getVendorMediaList(Authentication authentication, Vendor vendor) {
        List<VendorMedia> qualifiedVendorMediaList = new ArrayList<VendorMedia>();

        if (vendor != null && authenticationService.hasAccess(authentication, vendor)) {
            List<VendorMedia> vendorMediaList = vendorMediaRepository.findByVendorInOrderByUpdateTimeDesc(Arrays.asList(vendor));

            for (VendorMedia vendorMedia : vendorMediaList) {
                qualifiedVendorMediaList.add(vendorMedia);
            }
        }

        return qualifiedVendorMediaList;
    }

    public List<VendorMedia> getVendorMediaList(Authentication authentication, List<Vendor> vendorList) {
        List<Vendor> qualifiedVendorList = new ArrayList<Vendor>();

        if (vendorList != null) {
            for (Vendor vendor : vendorList) {
                if (authenticationService.hasAccess(authentication, vendor)) {
                    qualifiedVendorList.add(vendor);
                }
            }
        }

        List<VendorMedia> vendorMediaList = vendorMediaRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        return vendorMediaList;
    }

    public VendorMedia getVendorMedia(Authentication authentication, Integer id) {
        VendorMedia vendorMedia = vendorMediaRepository.findById(id).orElse(null);

        if (vendorMedia != null && authenticationService.hasAccess(authentication, vendorMedia.getVendor())) {
            return vendorMedia;
        } else {
            return null;
        }
    }

    public VendorMedia addVendorMedia(Authentication authentication, VendorMedia newVendorMedia) {
        if (newVendorMedia != null && newVendorMedia.getVendor() != null) {
            Vendor vendor = vendorRepository.findById(newVendorMedia.getVendor().getId()).orElse(null);

            if (vendor != null && (authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                || vendor.getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendor.getId())
                || vendor.getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendor.getId()))) {
                newVendorMedia.setVendor(vendor);
                newVendorMedia.setCreateTime(new Timestamp(System.currentTimeMillis()));
                newVendorMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                VendorMedia updatedVendorMedia = vendorMediaRepository.save(newVendorMedia);

                return updatedVendorMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public VendorMedia updateVendorMedia(Authentication authentication, VendorMedia targetVendorMedia, VendorMedia newVendorMedia) {
        if (targetVendorMedia != null && targetVendorMedia.getId() != null && newVendorMedia != null && newVendorMedia.getVendor() != null) {
            VendorMedia vendorMedia = vendorMediaRepository.findById(targetVendorMedia.getId()).orElse(null);
            Vendor vendor = vendorRepository.findById(newVendorMedia.getVendor().getId()).orElse(null);

            if (vendorMedia != null
                && vendor != null
                && (authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                    || vendorMedia.getVendor().getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendorMedia.getVendor().getId())
                    || vendorMedia.getVendor().getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendorMedia.getVendor().getId()))) {
                newVendorMedia.setVendor(vendor);
                newVendorMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                VendorMedia updatedVendorMedia = vendorMediaRepository.save(newVendorMedia);

                return updatedVendorMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional
    public VendorMedia removeVendorMedia(Authentication authentication, VendorMedia targetVendorMedia) {
        if (targetVendorMedia != null && targetVendorMedia.getId() != null) {
            VendorMedia vendorMedia = vendorMediaRepository.findById(targetVendorMedia.getId()).orElse(null);

            if (vendorMedia != null
                && (authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                    || vendorMedia.getVendor().getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendorMedia.getVendor().getId())
                    || vendorMedia.getVendor().getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendorMedia.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendorMedia.getVendor().getId()))) {
                vendorMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                vendorMedia.setDeleted(true);

                for (VendorPort vendorPort : vendorMedia.getVendorPort()) {
                    vendorPortService.removeVendorPort(authentication, vendorPort);
                }

                return vendorMedia;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
