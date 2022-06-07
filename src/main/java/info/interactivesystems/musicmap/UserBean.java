package info.interactivesystems.musicmap;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.hc.core5.http.ParseException;
import org.primefaces.event.TabChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.User;

import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.utils.SpotifyUtils;

@Named
@SessionScoped
public class UserBean implements Serializable {
    private static final long serialVersionUID = 13L;
    private static final Logger logger = LoggerFactory.getLogger(UserBean.class);

    private User user;
    private String conditionUrl;
    private int condition;
    private int caseId;
    private String activeIndex;
    private String activeSubIndex;

    @Inject
    private SpotifyUtils spotifyUtils;

    @Inject
    private GenreHierarchyBean genreHierarchyBean;

    @PostConstruct
    private void init() {
	activeIndex = "";
	activeSubIndex = "";
    }

    public void authorizeUser(String code) {
	spotifyUtils.authorize(code);
	try {
	    user = spotifyUtils.requestUser();
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    logger.warn("Cannot authorize user.", e);
	}
    }

    public void openArtistTabs(Artist artist) {
	for (GenreMapItem superGenre : genreHierarchyBean.getSortedSuperGenres()) {
	    for (GenreMapItem subGenre : genreHierarchyBean.getSortedSubGenres(superGenre)) {
		if (genreHierarchyBean.getSortedArtistsForGenre(subGenre).stream().filter(artistMapItem -> artistMapItem.getSpotifyUri().equals(artist.getUri())).count() > 0) {
		    this.activeIndex = Integer.toString(getIndexOfGenreWithId(superGenre.getGenreId()));
		    this.activeSubIndex = "";
		}
	    }
	}
    }

    public void notifyTabChange(TabChangeEvent<GenreMapItem> event) {
	int genreId = event.getData().getGenreId();
	this.activeSubIndex = "";
	this.activeIndex = Integer.toString(getIndexOfGenreWithId(genreId));
    }

    private int getIndexOfGenreWithId(int genreId) {
	return genreHierarchyBean.getSortedSuperGenres()
		.indexOf(genreHierarchyBean.getSortedSuperGenres().stream().filter(genre -> genre.getGenreId() == genreId).findFirst().get());
    }

    public void notifySubTabChange(TabChangeEvent<GenreMapItem> event) {
	int genreId = event.getData().getGenreId();
	GenreMapItem superGenre = genreHierarchyBean.getSortedSuperGenres().get(Integer.parseInt(activeIndex));
	this.activeSubIndex = Integer.toString(getIndexOfSubGenreWithId(genreId, superGenre.getGenreId()));
    }

    private int getIndexOfSubGenreWithId(int genreId, int superGenreId) {
	int superGenreIndex = getIndexOfGenreWithId(superGenreId);
	GenreMapItem superGenre = genreHierarchyBean.getSortedSuperGenres().get(superGenreIndex);
	return genreHierarchyBean.getSortedSubGenres(superGenre)
		.indexOf(genreHierarchyBean.getSortedSubGenres(superGenre).stream().filter(genre -> genre.getGenreId() == genreId).findFirst().get());
    }

    public void notifyTabClose() {
	this.activeIndex = "";
    }

    public void notifySubTabClose() {
	this.activeSubIndex = "";
    }

    public User getUser() {
	return user;
    }

    public void setUser(User user) {
	this.user = user;
    }

    public String getConditionUrl() {
	return conditionUrl;
    }

    public void setConditionUrl(String conditionUrl) {
	this.conditionUrl = conditionUrl;
    }

    public int getCondition() {
	return condition;
    }

    public void setCondition(int condition) {
	this.condition = condition;
    }

    public int getCaseId() {
	return caseId;
    }

    public void setCaseId(int caseId) {
	this.caseId = caseId;
    }

    public String getActiveIndex() {
	return activeIndex;
    }

    public void setActiveIndex(String activeIndex) {
	// prevent jsf from changing activeIndex
    }

    public String getActiveSubIndex() {
	return activeSubIndex;
    }

    public void setActiveSubIndex(String activeSubIndex) {
    }

}
