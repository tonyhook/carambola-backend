package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.MediaRepository;
import cc.tonyhook.carambola.backend.entity.perf.Media;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class MediaService {

    private final AuthenticationService authenticationService;

    private final MediaRepository mediaRepository;

    public MediaService(
            AuthenticationService authenticationService,
            MediaRepository mediaRepository
    ) {
        this.authenticationService = authenticationService;
        this.mediaRepository = mediaRepository;
    }

    public List<Media> queryMediaList(Authentication authentication, Query query) {
        List<Media> qualifiedMediaList = getMediaList(authentication);

        qualifiedMediaList.removeIf(media -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += media.getName().toLowerCase();
                    }
                    if (key.equals("code")) {
                        value += StringUtils.defaultString(media.getCode()).toLowerCase();
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

        return qualifiedMediaList;
    }

    public List<Media> getMediaList(Authentication authentication) {
        List<Media> mediaList = mediaRepository.findAll();

        return mediaList;
    }

    public Media getMedia(Authentication authentication, Integer id) {
        Media media = mediaRepository.findById(id).orElse(null);

        return media;
    }

    public Media addMedia(Authentication authentication, Media newMedia) {
        if (newMedia != null && authenticationService.isManagement(authentication)) {
            newMedia.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Media updatedMedia = mediaRepository.save(newMedia);

            return updatedMedia;
        } else {
            return null;
        }
    }

    public Media updateMedia(Authentication authentication, Media targetMedia, Media newMedia) {
        if (targetMedia != null && newMedia != null && authenticationService.isManagement(authentication)) {
            newMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Media updatedMedia = mediaRepository.save(newMedia);

            return updatedMedia;
        } else {
            return null;
        }
    }

    @Transactional
    public Media removeMedia(Authentication authentication, Media targetMedia) {
        if (targetMedia != null && authenticationService.isManagement(authentication)) {
            targetMedia.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            targetMedia.setDeleted(true);

            return targetMedia;
        } else {
            return null;
        }
    }

}
