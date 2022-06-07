package info.interactivesystems.musicmap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.exceptions.NoSuchItemException;

@Named
@ApplicationScoped
public class SearchArtistBean {
    private static final Logger logger = LoggerFactory.getLogger(SearchArtistBean.class);
    private boolean searchBeanReady = false;
    private Map<String, ArtistMapItem> searchStringToArtist;

    @Inject
    private GenreHierarchyBean genreHierarchyBean;

    @PostConstruct
    private void init() {
	searchStringToArtist = new HashMap<String, ArtistMapItem>();

	logger.debug("Fetching artist strings for auto completion...");
	for (ArtistMapItem artistMapItem : genreHierarchyBean.getAllArtists()) {
	    searchStringToArtist.put(((Artist) artistMapItem.getUserData()).getName(), artistMapItem);
	}
	logger.debug("Done fetching artist strings for auto completion.");
	searchBeanReady = true;

    }

    public Collection<String> getArtistsForSearch() {
	return searchStringToArtist.keySet();
    }

    public Artist getArtistBySearchString(String searchString) throws NoSuchItemException {
	if (!searchStringToArtist.containsKey(searchString)) {
	    throw new NoSuchItemException(searchString);
	}
	return (Artist) searchStringToArtist.get(searchString).getUserData();
    }

    public boolean isSearchBeanReady() {
	return searchBeanReady;
    }

    public void setSearchBeanReady(boolean searchBeanReady) {
	this.searchBeanReady = searchBeanReady;
    }

}
