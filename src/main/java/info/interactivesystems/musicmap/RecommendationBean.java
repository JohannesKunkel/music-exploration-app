package info.interactivesystems.musicmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.dao.MusicDAO;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Track;
import info.interactivesystems.musicmap.exceptions.NoSuchItemException;
import info.interactivesystems.musicmap.utils.Comparators;
import info.interactivesystems.musicmap.utils.SpotifyUtils;
import info.interactivesystems.musicmap.utils.json.JsonUtils;

@Named
@SessionScoped
public class RecommendationBean implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(RecommendationBean.class);
    private static final long serialVersionUID = 10L;

    private static final int HOW_MANY_RECOMMENDATIONS = 10;

    private List<Track> preferences;
    private List<Track> recommendations;

    @Inject
    private SpotifyUtils spotifyUtils;

    @Inject
    private MusicDAO musicDao;

    @Inject
    private GenreHierarchyBean genreHierarchyBean;

    @PostConstruct
    private void init() {
	recommendations = new ArrayList<Track>();
	preferences = new ArrayList<Track>();
    }

    public void refreshRecommendations() {
	recommendations.clear();
	if (preferences.isEmpty()) {
	    return;
	}
	int multiplier = 1;
	while (recommendations.size() < HOW_MANY_RECOMMENDATIONS && multiplier <= 5) {
	    List<String> recommendationUris = spotifyUtils.getRecommendationUris(preferences, HOW_MANY_RECOMMENDATIONS * multiplier);
	    for (String recommendationUri : recommendationUris) {
		if (!uriInRecommendations(recommendationUri)) {
		    fetchTrackAndAddToRecommendations(recommendationUri);
		}

	    }
	    multiplier++;
	}
	if (recommendations.size() > HOW_MANY_RECOMMENDATIONS) {
	    recommendations.subList(HOW_MANY_RECOMMENDATIONS, recommendations.size()).clear();
	}
    }

    private boolean uriInRecommendations(String recommendationUri) {
	return recommendations.stream().filter(r -> r.getUri().equals(recommendationUri)).count() > 0;
    }

    private void fetchTrackAndAddToRecommendations(String recommendationUri) {
	try {
	    Track recommendation = musicDao.getTrackByUri(recommendationUri);
	    recommendations.add(recommendation);
	} catch (NoSuchItemException e) {
	    log.warn("Item not found: " + recommendationUri);
	}
    }

    public Collection<ArtistMapItem> getArtistsWithPreferencesInGenre(GenreMapItem genre) {
	Collection<ArtistMapItem> artistsWithPreferencesInGenre = getTrackArtistsInGenre(genre, preferences);
	return new ArrayList<ArtistMapItem>(artistsWithPreferencesInGenre);
    }

    public Collection<ArtistMapItem> getArtistsWithRecommendationInGenre(GenreMapItem genre) {
	return new ArrayList<ArtistMapItem>(getTrackArtistsInGenre(genre, recommendations));
    }

    private Collection<ArtistMapItem> getTrackArtistsInGenre(GenreMapItem genre, Collection<Track> tracks) {
	Collection<ArtistMapItem> artistsWithGenre = genreHierarchyBean.getSortedArtistsForGenre(genre, Comparators.ARTIST_NAME_COMPARATOR);
	Collection<Artist> artistsInTracks = tracksToArtists(tracks);

	return artistsWithGenre.stream().filter(artistMapItem -> artistsInTracks.contains(artistMapItem.getUserData())).collect(Collectors.toSet());
    }

    private Set<Artist> tracksToArtists(Collection<Track> tracks) {
	return tracks.stream().flatMap(track -> track.getAllArtists().stream()).collect(Collectors.toSet());
    }

    public void addPreference(Track track) {
	this.preferences.add(track);
	refreshRecommendations();
    }

    public void removePreferences(Track track) {
	this.preferences.remove(track);
	refreshRecommendations();
    }

    public void reset() {
	this.preferences.clear();
	this.recommendations.clear();
    }

    public boolean isPreference(Track track) {
	return preferences.contains(track);
    }

    public boolean isPreference(Artist artist) {
	boolean isPreference = false;
	for (Track track : artist.getTracks()) {
	    if (preferences.contains(track)) {
		isPreference = true;
	    }
	}
	return isPreference;
    }

    public List<Track> getPreferences() {
	return this.preferences;
    }

    public void setPreferences(List<Track> preferences) {
	this.preferences = preferences;
    }

    public boolean isRecommendation(Track track) {
	return recommendations.contains(track);
    }

    public boolean isRecommendation(Artist artist) {
	boolean isRecommendation = false;
	for (Track track : artist.getTracks()) {
	    if (recommendations.contains(track)) {
		isRecommendation = true;
	    }
	}
	return isRecommendation;
    }

    public String getRecommendedArtistsAsJson() {
	return JsonUtils.artistIdsToJson(tracksToArtists(recommendations));
    }

    public String getPreferencedArtistsAsJson() {
	return JsonUtils.artistIdsToJson(tracksToArtists(preferences));
    }

    public List<Track> getRecommendations() {
	return recommendations;
    }

    public void setRecommendations(List<Track> recommendations) {
	this.recommendations = recommendations;
    }
}
