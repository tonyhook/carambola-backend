package cc.tonyhook.carambola.backend.controller.open;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.backend.Site;
import cc.tonyhook.carambola.backend.service.backend.SiteService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OpenApplicationController {

    private final SiteService siteService;

    public OpenApplicationController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping(value = "/api/open/application/site", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Site> getSite(
            HttpServletRequest request) {
        String host = request.getServerName();

        List<Site> siteList = siteService.getSiteList();
        for (Site site : siteList) {
            if (host.endsWith(site.getDomain())) {
                return ResponseEntity.ok().body(site);
            }
        }

        Site site = new Site();
        site.setDomain(host);
        site.setName("Carambola");
        site.setCompany("tonyhook.cc");
        site.setRegistration(null);

        return ResponseEntity.ok().body(site);
    }

}
