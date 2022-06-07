package info.interactivesystems.musicmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.dao.MusicDAO;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Track;
import info.interactivesystems.musicmap.utils.Comparators;

@Named
@SessionScoped
public class DetailsBean implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(DetailsBean.class);
    private static final long serialVersionUID = 12L;

    private Artist currentArtist;

    private Track currentTrack;

    @Inject
    private MusicDAO musicDao;

    @Inject
    private RecommendationBean recommendationBean;

    @Inject
    private MusicPlayerBean musicPlayerBean;

    @Inject
    private UserBean userBean;

    public void changeCurrentTrack(Track track) {
	log.trace("Start changing current track...");
	this.currentTrack = track;
	musicPlayerBean.changeTrack(track.getUri());
	log.trace("Start logging track change to db...");
	log.trace("Done logging track change to db.");
	log.debug("Changed current item to: {} ({})", track.getId(), track.getTitle());
    }

    public List<Artist> getCurrentTracksArtistsAsList() {
	return new ArrayList<Artist>(currentTrack.getAllArtists());
    }

    public void changeCurrentArtistFromTrack(Artist artist) {
	if (userBean.getCondition() == 1) {
	    userBean.openArtistTabs(artist);
	}
	changeCurrentArtist(artist);
    }

    public void changeCurrentArtist(Artist artist) {
	this.currentArtist = artist;
	log.debug("Changed current artist to: {} ({})", this.currentArtist.getId(), this.currentArtist.getName());
    }

    public void changeCurrentArtist() {
	FacesContext context = FacesContext.getCurrentInstance();
	Map<String, String> map = context.getExternalContext().getRequestParameterMap();
	int artistId = Integer.parseInt(map.get("artistId"));
	if (currentArtist == null || currentArtist.getId() != artistId) {
	    Artist artist = musicDao.getArtist(artistId);
	    changeCurrentArtist(artist);
	}
    }

    public boolean isCurrentArtistInGenre(GenreMapItem genre) {
	if (currentArtist == null) {
	    return false;
	}
	return currentArtist.getGenres().stream().map(artistGenre -> artistGenre.getId()).collect(Collectors.toSet()).contains(genre.getGenreId());
    }

    public List<Track> getCurrentArtistsTracks() {
	List<Track> artistTracks = new ArrayList<Track>();
	for (Track preference : recommendationBean.getPreferences()) {
	    if (preference.getAllArtists().contains(currentArtist)) {
		artistTracks.add(preference);
	    }
	}
	for (Track recommendation : recommendationBean.getRecommendations()) {
	    if (recommendation.getAllArtists().contains(currentArtist)) {
		artistTracks.add(recommendation);
	    }
	}
	List<Track> filteredArtistTracks = currentArtist.getTracks().stream().filter(t -> !artistTracks.contains(t)).collect(Collectors.toList());
	Collections.sort(filteredArtistTracks, Comparators.TRACK_POPULARITY_COMPARATOR);
	Collections.reverse(filteredArtistTracks);
	artistTracks.addAll(filteredArtistTracks.subList(0, Math.min(10, filteredArtistTracks.size())));
	return artistTracks;
    }

    public Track getCurrentTrack() {
	return currentTrack;
    }

    public List<String> getCurrentArtistsGenres() {
	List<String> genres = currentArtist.getGenres().stream().map(g -> g.getTitle()).collect(Collectors.toList());
	Collections.sort(genres);
	return genres;
    }

    public void toggleLiked() {
	if (isPreference()) {
	    recommendationBean.removePreferences(currentTrack);
	} else {
	    recommendationBean.addPreference(currentTrack);
	}
    }

    public void reset() {
	musicPlayerBean.stopPlayback();
	currentArtist = null;
	currentTrack = null;
    }

    public boolean isPreference() {
	return recommendationBean.isPreference(currentTrack);
    }

    public Artist getCurrentArtist() {
	return currentArtist;
    }

    public void setCurrentArtist(Artist currentArtist) {
	this.currentArtist = currentArtist;
    }

}
