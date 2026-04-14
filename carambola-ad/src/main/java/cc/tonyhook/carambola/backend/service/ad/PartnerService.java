package cc.tonyhook.carambola.backend.service.ad;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.ClientPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.ClientRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorMediaRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;

@Service
public class PartnerService {

    private final AuthenticationService authenticationService;
    private final TenantDefaultService tenantDefaultService;
    private final ClientRepository clientRepository;
    private final ClientMediaRepository clientMediaRepository;
    private final ClientPortRepository clientPortRepository;
    private final VendorRepository vendorRepository;
    private final VendorMediaRepository vendorMediaRepository;
    private final VendorPortRepository vendorPortRepository;

    public PartnerService(
            AuthenticationService authenticationService,
            TenantDefaultService tenantDefaultService,
            ClientRepository clientRepository,
            ClientMediaRepository clientMediaRepository,
            ClientPortRepository clientPortRepository,
            VendorRepository vendorRepository,
            VendorMediaRepository vendorMediaRepository,
            VendorPortRepository vendorPortRepository
    ) {
        this.authenticationService = authenticationService;
        this.tenantDefaultService = tenantDefaultService;
        this.clientRepository = clientRepository;
        this.clientMediaRepository = clientMediaRepository;
        this.clientPortRepository = clientPortRepository;
        this.vendorRepository = vendorRepository;
        this.vendorMediaRepository = vendorMediaRepository;
        this.vendorPortRepository = vendorPortRepository;
    }

    public List<Client> getQualifiedClientListWithoutFilterAndSearch(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = new ArrayList<Client>();

        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);
        List<Client> clientList;
        if (tenantDefault == null) {
            clientList = clientRepository.findAll();
        } else {
            clientList = clientRepository.findByTenantOrderByUpdateTimeDesc(tenantDefault.getTenant());
        }

        for (Client client : clientList) {
            if (authenticationService.hasAccess(authentication, client)) {
                qualifiedClientList.add(client);
            }
        }
        qualifiedClientList.removeIf(client -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("clientMode")) {
                    List<String> modeList = query.filter.get(key);
                    Boolean matched = false;
                    for (String mode : modeList) {
                        if (client.getMode().toString().equals(mode.toLowerCase())) {
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

        return qualifiedClientList;
    }

    public List<Vendor> getQualifiedVendorListWithoutFilterAndSearch(Authentication authentication, Query query) {
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

        return qualifiedVendorList;
    }

    public List<ClientMedia> getQualifiedClientMediaListWithoutFilterAndSearch(List<Client> qualifiedClientList, Query query) {
        List<ClientMedia> clientMediaList = clientMediaRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        clientMediaList.removeIf(clientMedia -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("client")) {
                    List<String> clientIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientId : clientIdList) {
                        if (clientMedia.getClient().getId().toString().equals(clientId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientMedia")) {
                    List<String> clientMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientMediaId : clientMediaIdList) {
                        if (clientMedia.getId().toString().equals(clientMediaId.toLowerCase())) {
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

        return clientMediaList;
    }

    public List<VendorMedia> getQualifiedVendorMediaListWithoutFilterAndSearch(List<Vendor> qualifiedVendorList, Query query) {
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
                if (key.equals("vendorMedia")) {
                    List<String> vendorMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorMediaId : vendorMediaIdList) {
                        if (vendorMedia.getId().toString().equals(vendorMediaId.toLowerCase())) {
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

        return vendorMediaList;
    }

    public List<ClientPort> getQualifiedClientPortList(List<Client> qualifiedClientList, Query query) {
        List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        clientPortList.removeIf(clientPort -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("client")) {
                    List<String> clientIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientId : clientIdList) {
                        if (clientPort.getClient().getId().toString().equals(clientId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientMedia")) {
                    List<String> clientMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientMediaId : clientMediaIdList) {
                        if (clientPort.getClientMedia().getId().toString().equals(clientMediaId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientPort")) {
                    List<String> clientPortIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientPortId : clientPortIdList) {
                        if (clientPort.getId().toString().equals(clientPortId.toLowerCase())) {
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
                        if (clientPort.getMode().toString().equals(mode.toLowerCase())) {
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
                        if (clientPort.getFormat().toString().equals(format.toLowerCase())) {
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
                        if (clientPort.getBudget().toString().equals(budget.toLowerCase())) {
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
                        value += clientPort.getName().toLowerCase();
                    }
                    if (key.equals("tagId")) {
                        if (clientPort.getTagId() != null) {
                            value += clientPort.getTagId().toLowerCase();
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

        return clientPortList;
    }

    public List<ClientPort> getQualifiedClientPortListWithoutFilterAndSearch(List<Client> qualifiedClientList, Query query) {
        List<ClientPort> clientPortList = clientPortRepository.findByClientInOrderByUpdateTimeDesc(qualifiedClientList);

        clientPortList.removeIf(clientPort -> {
            for (String key : query.filter.keySet()) {
                if (query.filter.get(key).isEmpty()) {
                    continue;
                }
                if (key.equals("client")) {
                    List<String> clientIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientId : clientIdList) {
                        if (clientPort.getClient().getId().toString().equals(clientId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientMedia")) {
                    List<String> clientMediaIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientMediaId : clientMediaIdList) {
                        if (clientPort.getClientMedia().getId().toString().equals(clientMediaId.toLowerCase())) {
                            matched = true;
                        }
                    }
                    if (!matched) {
                        return true;
                    }
                }
                if (key.equals("clientPort")) {
                    List<String> clientPortIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String clientPortId : clientPortIdList) {
                        if (clientPort.getId().toString().equals(clientPortId.toLowerCase())) {
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

        return clientPortList;
    }

    public List<VendorPort> getQualifiedVendorPortList(List<Vendor> qualifiedVendorList, Query query) {
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
                if (key.equals("vendorPort")) {
                    List<String> vendorPortIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorPortId : vendorPortIdList) {
                        if (vendorPort.getId().toString().equals(vendorPortId.toLowerCase())) {
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

    public List<VendorPort> getQualifiedVendorPortListWithoutFilterAndSearch(List<Vendor> qualifiedVendorList, Query query) {
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
                if (key.equals("vendorPort")) {
                    List<String> vendorPortIdList = query.filter.get(key);
                    Boolean matched = false;
                    for (String vendorPortId : vendorPortIdList) {
                        if (vendorPort.getId().toString().equals(vendorPortId.toLowerCase())) {
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

        return vendorPortList;
    }

}
