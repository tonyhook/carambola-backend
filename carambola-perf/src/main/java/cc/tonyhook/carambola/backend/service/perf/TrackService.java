package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.TrackRepository;
import cc.tonyhook.carambola.backend.entity.perf.Track;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class TrackService {

    private final AuthenticationService authenticationService;

    private final TrackRepository trackRepository;

    public TrackService(
            AuthenticationService authenticationService,
            TrackRepository trackRepository
    ) {
        this.authenticationService = authenticationService;
        this.trackRepository = trackRepository;
    }

    public List<Track> queryTrackList(Authentication authentication, Query query) {
        List<Track> qualifiedTrackList = getTrackList(authentication);

        qualifiedTrackList.removeIf(track -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("name")) {
                        value += track.getName().toLowerCase();
                    }
                    if (key.equals("code")) {
                        value += StringUtils.defaultString(track.getCode()).toLowerCase();
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

        return qualifiedTrackList;
    }

    public List<Track> getTrackList(Authentication authentication) {
        List<Track> trackList = trackRepository.findAll();

        return trackList;
    }

    public Track getTrack(Authentication authentication, Integer id) {
        Track track = trackRepository.findById(id).orElse(null);

        return track;
    }

    public Track addTrack(Authentication authentication, Track newTrack) {
        if (newTrack != null && authenticationService.isManagement(authentication)) {
            newTrack.setCreateTime(new Timestamp(System.currentTimeMillis()));
            newTrack.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Track updatedTrack = trackRepository.save(newTrack);

            return updatedTrack;
        } else {
            return null;
        }
    }

    public Track updateTrack(Authentication authentication, Track targetTrack, Track newTrack) {
        if (targetTrack != null && newTrack != null && authenticationService.isManagement(authentication)) {
            newTrack.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            Track updatedTrack = trackRepository.save(newTrack);

            return updatedTrack;
        } else {
            return null;
        }
    }

    @Transactional
    public Track removeTrack(Authentication authentication, Track targetTrack) {
        if (targetTrack != null && authenticationService.isManagement(authentication)) {
            targetTrack.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            targetTrack.setDeleted(true);

            return targetTrack;
        } else {
            return null;
        }
    }

}
