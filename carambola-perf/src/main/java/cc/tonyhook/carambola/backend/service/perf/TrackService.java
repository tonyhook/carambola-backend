package cc.tonyhook.carambola.backend.service.perf;

import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.TrackRepository;
import cc.tonyhook.carambola.backend.entity.perf.Track;
import jakarta.transaction.Transactional;

@Service
public class TrackService {

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public List<Track> getTrackList() {
        List<Track> trackList = trackRepository.findAll();

        return trackList;
    }

    public Track getTrack(Integer id) {
        Track track = trackRepository.findById(id).orElse(null);

        return track;
    }

    public Track addTrack(Track newTrack) {
        Track updatedTrack = trackRepository.save(newTrack);

        return updatedTrack;
    }

    public void updateTrack(Integer id, Track newTrack) {
        trackRepository.save(newTrack);
    }

    @Transactional
    public void removeTrack(Integer id) {
        Track deletedTrack = trackRepository.findById(id).orElse(null);

        trackRepository.delete(deletedTrack);
    }

}
