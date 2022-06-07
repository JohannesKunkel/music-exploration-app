package info.interactivesystems.musicmap.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.entities.Artist;

public class GenreHierarchy {
    private static final Logger logger = LoggerFactory.getLogger(GenreHierarchy.class);

    private Map<GenreMapItem, Collection<GenreMapItem>> genres;

    private Map<GenreMapItem, Collection<ArtistMapItem>> genreArtists;

    private GenreHierarchy() {
    }

    public Collection<GenreMapItem> getSuperGenres() {
	return genres.keySet();
    }

    public Collection<GenreMapItem> getSubGenres(GenreMapItem superGenre) {
	return genres.get(superGenre);
    }

    public List<ArtistMapItem> getArtistsForGenre(GenreMapItem genre) {
	List<ArtistMapItem> artistsForGenre = new ArrayList<ArtistMapItem>();
	if (getSuperGenres().contains(genre)) {
	    for (GenreMapItem subGenre : getSubGenres(genre)) {
		artistsForGenre.addAll(genreArtists.get(subGenre));
	    }
	} else {
	    artistsForGenre.addAll(genreArtists.get(genre));
	}
	return artistsForGenre;
    }

    public boolean isSuperGenre(GenreMapItem genre) {
	return this.getSuperGenres().contains(genre);
    }

    public static GenreHierarchy create(Collection<ArtistMapItem> allArtists, Collection<GenreMapItem> allGenres) {
	logger.debug("Start creating GenreHierarchy...");
	GenreHierarchy genreHierarchy = new GenreHierarchy();
	genreHierarchy.genres = new HashMap<GenreMapItem, Collection<GenreMapItem>>();
	for (GenreMapItem lvl0Genre : allGenres.stream().filter(genre -> genre.getLevel() == 0).collect(Collectors.toList())) {
	    genreHierarchy.genres.put(lvl0Genre, new ArrayList<GenreMapItem>());
	}
	for (GenreMapItem lvl1Genre : allGenres.stream().filter(genre -> genre.getLevel() == 1).collect(Collectors.toList())) {
	    genreHierarchy.genres.get(lvl1Genre.getParent()).add(lvl1Genre);
	}
	genreHierarchy.genreArtists = createGenreArtists(allArtists, genreHierarchy.genres);
	logger.debug("Done creating GenreHierarchy.");
	return genreHierarchy;
    }

    private static Map<GenreMapItem, Collection<ArtistMapItem>> createGenreArtists(Collection<ArtistMapItem> allArtists, Map<GenreMapItem, Collection<GenreMapItem>> genres) {
	Map<GenreMapItem, Collection<ArtistMapItem>> genreArtists = new HashMap<GenreMapItem, Collection<ArtistMapItem>>();
	for (GenreMapItem superGenre : genres.keySet()) {
	    for (GenreMapItem subGenre : genres.get(superGenre)) {
		Collection<ArtistMapItem> subGenreArtists = new ArrayList<ArtistMapItem>();
		for (ArtistMapItem artistMapItem : allArtists) {
		    Artist artist = (Artist) artistMapItem.getUserData();
		    if (artist.getGenres().stream().map(g -> g.getId()).collect(Collectors.toList()).contains(subGenre.getGenreId())) {
			subGenreArtists.add(artistMapItem);
		    }
		}
		genreArtists.put(subGenre, subGenreArtists);
	    }
	}
	return genreArtists;
    }
}
