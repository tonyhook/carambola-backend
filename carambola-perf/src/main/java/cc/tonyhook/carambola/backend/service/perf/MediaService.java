package cc.tonyhook.carambola.backend.service.perf;

import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.MediaRepository;
import cc.tonyhook.carambola.backend.entity.perf.Media;
import jakarta.transaction.Transactional;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;

    public MediaService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public List<Media> getMediaList() {
        List<Media> mediaList = mediaRepository.findAll();

        return mediaList;
    }

    public Media getMedia(Integer id) {
        Media media = mediaRepository.findById(id).orElse(null);

        return media;
    }

    public Media addMedia(Media newMedia) {
        Media updatedMedia = mediaRepository.save(newMedia);

        return updatedMedia;
    }

    public void updateMedia(Integer id, Media newMedia) {
        mediaRepository.save(newMedia);
    }

    @Transactional
    public void removeMedia(Integer id) {
        Media deletedMedia = mediaRepository.findById(id).orElse(null);

        mediaRepository.delete(deletedMedia);
    }

}
