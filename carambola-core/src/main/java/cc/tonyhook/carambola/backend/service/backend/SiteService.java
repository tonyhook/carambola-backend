package cc.tonyhook.carambola.backend.service.backend;

import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.backend.SiteRepository;
import cc.tonyhook.carambola.backend.entity.backend.Site;
import jakarta.transaction.Transactional;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public List<Site> getSiteList() {
        List<Site> siteList = siteRepository.findAll();

        return siteList;
    }

    public Site getSite(Integer id) {
        Site site = siteRepository.findById(id).orElse(null);

        return site;
    }

    public Site addSite(Site newSite) {
        Site updatedSite = siteRepository.save(newSite);

        return updatedSite;
    }

    public void updateSite(Integer id, Site newSite) {
        siteRepository.save(newSite);
    }

    @Transactional
    public void removeSite(Integer id) {
        Site deletedSite = siteRepository.findById(id).orElse(null);

        siteRepository.delete(deletedSite);
    }

}
