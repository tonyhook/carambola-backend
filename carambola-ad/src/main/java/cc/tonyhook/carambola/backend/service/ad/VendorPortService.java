package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.VendorMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorRepository;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class VendorPortService {

    private final AuthenticationService authenticationService;
    private final ConnectionService connectionService;
    private final TenantDefaultService tenantDefaultService;

    private final VendorRepository vendorRepository;
    private final VendorMediaRepository vendorMediaRepository;
    private final VendorPortRepository vendorPortRepository;

    public VendorPortService(
            AuthenticationService authenticationService,
            ConnectionService connectionService,
            TenantDefaultService tenantDefaultService,
            VendorRepository vendorRepository,
            VendorMediaRepository vendorMediaRepository,
            VendorPortRepository vendorPortRepository
    ) {
        this.authenticationService = authenticationService;
        this.connectionService = connectionService;
        this.tenantDefaultService = tenantDefaultService;
        this.vendorRepository = vendorRepository;
        this.vendorMediaRepository = vendorMediaRepository;
        this.vendorPortRepository = vendorPortRepository;
    }

    public List<VendorPort> queryVendorPortList(Authentication authentication, Query query) {
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

        List<VendorPort> vendorPortList = vendorPortRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        vendorPortList.removeIf(vendorPort -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("vendor")) {
                    List<String> vendorIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorId : vendorIdList) {
                        if (vendorPort.getVendor().getId().toString().equals(vendorId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("vendorMedia")) {
                    List<String> vendorMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorMediaId : vendorMediaIdList) {
                        if (vendorPort.getVendorMedia().getId().toString().equals(vendorMediaId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("mode")) {
                    List<String> modeList = query.filter.get(key);
                    Boolean matched = false;
                    for (String mode : modeList) {
                        if (vendorPort.getMode().toString().equals(mode.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("format")) {
                    List<String> formatList = query.filter.get(key);
                    Boolean matched = false;
                    for (String format : formatList) {
                        if (vendorPort.getFormat().toString().equals(format.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("budget")) {
                    List<String> budgetList = query.filter.get(key);
                    Boolean matched = false;
                    for (String budget : budgetList) {
                        if (vendorPort.getBudget().toString().equals(budget.toLowerCase())) {
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
                        if (vendorPort.getVendorMedia().getPlatform().equals(platform)) {
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
                        value += vendorPort.getName().toLowerCase();
                    }
                    if (key.equals("tagId")) {
                        if (vendorPort.getTagId() != null) {
                            value += vendorPort.getTagId().toLowerCase();
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

        return vendorPortList;
    }

    public List<VendorPort> getVendorPortList(Authentication authentication) {
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

        List<VendorPort> vendorPortList = vendorPortRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        return vendorPortList;
    }

    public List<VendorPort> getVendorPortList(Authentication authentication, Vendor vendor) {
        List<VendorPort> qualifiedVendorPortList = new ArrayList<VendorPort>();

        if (vendor != null && authenticationService.hasAccess(authentication, vendor)) {
            List<VendorPort> vendorPortList = vendorPortRepository.findByVendorInOrderByUpdateTimeDesc(Arrays.asList(vendor));

            for (VendorPort vendorPort : vendorPortList) {
                qualifiedVendorPortList.add(vendorPort);
            }
        }

        return qualifiedVendorPortList;
    }

    public List<VendorPort> getVendorPortList(Authentication authentication, List<Vendor> vendorList) {
        List<Vendor> qualifiedVendorList = new ArrayList<Vendor>();

        if (vendorList != null) {
            for (Vendor vendor : vendorList) {
                if (authenticationService.hasAccess(authentication, vendor)) {
                    qualifiedVendorList.add(vendor);
                }
            }
        }

        List<VendorPort> vendorPortList = vendorPortRepository.findByVendorInOrderByUpdateTimeDesc(qualifiedVendorList);

        return vendorPortList;
    }

    public List<VendorPort> getVendorPortList(Authentication authentication, VendorMedia vendorMedia) {
        List<VendorPort> qualifiedVendorPortList = new ArrayList<VendorPort>();

        if (vendorMedia != null && authenticationService.hasAccess(authentication, vendorMedia.getVendor())) {
            List<VendorPort> vendorPortList = vendorPortRepository.findByVendorMediaInOrderByUpdateTimeDesc(Arrays.asList(vendorMedia));

            for (VendorPort vendorPort : vendorPortList) {
                qualifiedVendorPortList.add(vendorPort);
            }
        }

        return qualifiedVendorPortList;
    }

    public VendorPort getVendorPort(Authentication authentication, Integer id) {
        VendorPort vendorPort = vendorPortRepository.findById(id).orElse(null);

        if (vendorPort != null && authenticationService.hasAccess(authentication, vendorPort.getVendor())) {
            return vendorPort;
        } else {
            return null;
        }
    }

    public VendorPort addVendorPort(Authentication authentication, VendorPort newVendorPort) {
        if (newVendorPort != null && newVendorPort.getVendor() != null && newVendorPort.getVendorMedia() != null) {
            Vendor vendor = vendorRepository.findById(newVendorPort.getVendor().getId()).orElse(null);
            VendorMedia vendorMedia = vendorMediaRepository.findById(newVendorPort.getVendorMedia().getId()).orElse(null);

            if (vendor != null
                && vendorMedia != null
                && (authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                    || vendor.getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendor.getId())
                    || vendor.getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendor.getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendor.getId()))) {
                newVendorPort.setVendor(vendor);
                newVendorPort.setVendorMedia(vendorMedia);
                newVendorPort.setCreateTime(new Timestamp(System.currentTimeMillis()));
                newVendorPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                VendorPort updatedVendorPort = vendorPortRepository.save(newVendorPort);

                return updatedVendorPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public VendorPort updateVendorPort(Authentication authentication, VendorPort targetVendorPort, VendorPort newVendorPort) {
        if (targetVendorPort != null && targetVendorPort.getId() != null
            && newVendorPort != null && newVendorPort.getVendor() != null && newVendorPort.getVendorMedia() != null) {
            VendorPort vendorPort = vendorPortRepository.findById(targetVendorPort.getId()).orElse(null);
            Vendor vendor = vendorRepository.findById(newVendorPort.getVendor().getId()).orElse(null);
            VendorMedia vendorMedia = vendorMediaRepository.findById(newVendorPort.getVendorMedia().getId()).orElse(null);

            if (vendorPort != null
                && vendor != null
                && vendorMedia != null
                && (authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                    || vendor.getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendor.getId())
                    || vendor.getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendor.getId()))) {
                newVendorPort.setVendor(vendor);
                newVendorPort.setVendorMedia(vendorMedia);
                newVendorPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                VendorPort updatedVendorPort = vendorPortRepository.save(newVendorPort);

                return updatedVendorPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Transactional
    public VendorPort removeVendorPort(Authentication authentication, VendorPort targetVendorPort) {
        if (targetVendorPort != null && targetVendorPort.getId() != null) {
            VendorPort vendorPort = vendorPortRepository.findById(targetVendorPort.getId()).orElse(null);

            if (vendorPort != null
                && (authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                    || vendorPort.getVendor().getMode() == Vendor.PARTNER_TYPE_DIRECT && authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_DIRECT, vendorPort.getVendor().getId())
                    || vendorPort.getVendor().getMode() == Vendor.PARTNER_TYPE_PROGRAMMATIC && authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_DOWNSTREAM_MANAGER_PROGRAMMATIC, vendorPort.getVendor().getId()))) {
                vendorPort.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                vendorPort.setDeleted(true);

                for (Connection connection : vendorPort.getConnection()) {
                    connectionService.removeConnection(connection.getId());
                }

                return vendorPort;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
