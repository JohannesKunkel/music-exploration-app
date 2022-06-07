package info.interactivesystems.musicmap.utils.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Genre;
import info.interactivesystems.musicmap.utils.GenreHierarchy;

public class JsonUtils {
    public static String genreHierarchyToJson(GenreHierarchy genreHierarchy) {

	GenreHierarchyForJson genreHierarchyJson = getJsonHierarchyFromGenreHierarchy(genreHierarchy);
	return new Gson().toJson(genreHierarchyJson);
    }

    private static GenreHierarchyForJson getJsonHierarchyFromGenreHierarchy(GenreHierarchy genreHierarchy) {

	List<Lvl0GenreForJson> lvl0Children = new ArrayList<Lvl0GenreForJson>();
	List<GenreMapItem> superGenres = new ArrayList<>(genreHierarchy.getSuperGenres());
	Collections.sort(superGenres, new Comparator<GenreMapItem>() {

	    @Override
	    public int compare(GenreMapItem o1, GenreMapItem o2) {
		return ((Genre) o1.getUserData()).getTitle().compareTo(((Genre) o2.getUserData()).getTitle());
	    }
	});
	for (GenreMapItem superGenre : superGenres) {
	    List<Lvl1GenreForJson> lvl1Children = new ArrayList<Lvl1GenreForJson>();
	    List<GenreMapItem> subGenres = new ArrayList<GenreMapItem>(genreHierarchy.getSubGenres(superGenre));
	    // Collections.sort(subGenres, Comparators.GENRE_MAP_ITEM_POSITION_COMPARATOR);
	    for (GenreMapItem subGenre : subGenres) {
		List<ArtistMapItem> artistsForGenre = genreHierarchy.getArtistsForGenre(subGenre);
		// Collections.sort(artistsForGenre, new Comparators.ArtistMapItemPositionComparator(subGenre));
		lvl1Children.add(new Lvl1GenreForJson(subGenre, artistsForGenre));
	    }
	    lvl0Children.add(new Lvl0GenreForJson(superGenre, lvl1Children));
	}
	return new GenreHierarchyForJson(lvl0Children);
    }

    public static String artistIdsToJson(Collection<Artist> artists) {
	StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append("[");
	String prefix = "";
	for (Artist artist : artists) {
	    stringBuilder.append(prefix);
	    prefix = ", ";
	    stringBuilder.append(artist.getId());
	}
	stringBuilder.append("]");
	return stringBuilder.toString();
    }

    public static JsonArray toJsonArray(String uri) {
	return JsonParser.parseString("[\"" + uri + "\"]").getAsJsonArray();
    }

    public static Map<String, Collection<String>> getGenreHierarchyFromJson(JsonObject jsonHierarchyObject) {
	HashMap<String, Collection<String>> genreHierarchy = new HashMap<String, Collection<String>>();

	for (Entry<String, JsonElement> superGenre : jsonHierarchyObject.entrySet()) {
	    Type collectionType = new TypeToken<Collection<String>>() {
	    }.getType();
	    Collection<String> subGenres = new Gson().fromJson(superGenre.getValue(), collectionType);
	    genreHierarchy.put(superGenre.getKey(), subGenres);
	}

	return genreHierarchy;
    }

}
