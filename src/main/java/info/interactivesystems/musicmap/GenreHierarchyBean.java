package info.interactivesystems.musicmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.dao.MapDAO;
import info.interactivesystems.musicmap.dao.MusicDAO;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Genre;
import info.interactivesystems.musicmap.utils.Comparators;
import info.interactivesystems.musicmap.utils.GenreHierarchy;
import info.interactivesystems.musicmap.utils.json.JsonUtils;

@Named
@ApplicationScoped
public class GenreHierarchyBean {
    private static final Logger logger = LoggerFactory.getLogger(GenreHierarchyBean.class);

    private GenreHierarchy genreHierarchy;

    @Inject
    private MusicDAO musicDao;

    @Inject
    private MapDAO mapDao;

    @PostConstruct
    private void init() {
	Collection<GenreMapItem> allGenres = mapDao.getAllGenres();
	logger.debug("Start populating GenreMapItems...");
	allGenres.stream().forEach(genreMapItem -> populateGenreMapItem(genreMapItem));
	logger.debug("Done populating GenreMapItems.");
	Collection<ArtistMapItem> allArtists = mapDao.getAllMapItems();
	logger.debug("Start populating ArtistMapItems...");
	allArtists.stream().forEach(artistMapItem -> artistMapItem.setUserData(musicDao.getArtistByUri(artistMapItem.getSpotifyUri())));
	logger.debug("Done populating ArtistMapItems.");
	genreHierarchy = GenreHierarchy.create(allArtists, allGenres);
    }

    private void populateGenreMapItem(GenreMapItem genreMapItem) {
	genreMapItem.setUserData(musicDao.getGenre(genreMapItem.getGenreId()));
	genreMapItem.getRepresentatives().stream().forEach(mapItem -> mapItem.setUserData(musicDao.getArtistByUri(mapItem.getSpotifyUri())));
    }

    public List<GenreMapItem> getSortedSuperGenres() {
	List<GenreMapItem> superGenres = new ArrayList<GenreMapItem>(genreHierarchy.getSuperGenres());
	Collections.sort(superGenres, Comparators.GENRE_MAP_ITEM_TITLE_COMPARATOR);
	return superGenres;
    }

    public List<GenreMapItem> getSortedSubGenres(GenreMapItem superGenre) {
	List<GenreMapItem> subGenres = new ArrayList<GenreMapItem>(genreHierarchy.getSubGenres(superGenre));
	Collections.sort(subGenres, Comparators.GENRE_MAP_ITEM_TITLE_COMPARATOR);
	return subGenres;
    }

    public List<ArtistMapItem> getSortedArtistsForGenre(GenreMapItem genre) {
	return getSortedArtistsForGenre(genre, Comparators.ARTIST_NAME_COMPARATOR);
    }

    public List<ArtistMapItem> getSortedArtistsForGenre(GenreMapItem genre, Comparator<ArtistMapItem> comparator) {
	List<ArtistMapItem> artistsForGenre = new ArrayList<ArtistMapItem>(genreHierarchy.getArtistsForGenre(genre));
	Collections.sort(artistsForGenre, comparator);
	return artistsForGenre;
    }

    public String getGenreHierarchyAsJson() {
	return JsonUtils.genreHierarchyToJson(genreHierarchy);
    }

    public Collection<ArtistMapItem> getAllArtists() {
	Collection<ArtistMapItem> allArtists = new HashSet<ArtistMapItem>();
	for (GenreMapItem superGenre : genreHierarchy.getSuperGenres()) {
	    for (GenreMapItem subGenre : genreHierarchy.getSubGenres(superGenre)) {
		allArtists.addAll(genreHierarchy.getArtistsForGenre(subGenre));
	    }
	}
	return allArtists;
    }

    public Genre getGenreFromMapItem(GenreMapItem genreMapItem) {
	return (Genre) genreMapItem.getUserData();
    }

    public Artist getArtistFromMapItem(ArtistMapItem artistMapItem) {
	return (Artist) artistMapItem.getUserData();
    }

}
