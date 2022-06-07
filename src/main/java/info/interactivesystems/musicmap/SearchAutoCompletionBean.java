package info.interactivesystems.musicmap;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.exceptions.NoSuchItemException;

@Named
@RequestScoped
public class SearchAutoCompletionBean {
    private static final Logger log = LoggerFactory.getLogger(SearchAutoCompletionBean.class);

    private String searchText;

    @Inject
    private SearchArtistBean searchArtistBean;

    @Inject
    private DetailsBean detailsBean;

    @Inject
    private UserBean userBean;

    public List<String> completeTrack(String query) {
	String lowerCaseQuery = query.toLowerCase();
	Collection<String> tracks = searchArtistBean.getArtistsForSearch();

	return tracks.stream().filter(trackString -> trackString.toLowerCase().contains(lowerCaseQuery)).collect(Collectors.toList());
    }

    public void onItemSelect(SelectEvent<String> event) {
	String selectedItem = event.getObject();
	try {
	    Artist selectedArtist = searchArtistBean.getArtistBySearchString(selectedItem);
	    detailsBean.changeCurrentArtist(selectedArtist);
	    if (userBean.getCondition() == 1) {
		userBean.openArtistTabs(selectedArtist);
	    }
	} catch (NoSuchItemException e) {
	    log.warn("Cannot find artist for search string.", e);
	}

    }

    public String getSearchText() {
	return searchText;
    }

    public void setSearchText(String searchText) {
	this.searchText = searchText;
    }

}
