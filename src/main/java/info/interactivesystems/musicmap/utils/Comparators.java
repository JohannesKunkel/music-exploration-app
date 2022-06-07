package info.interactivesystems.musicmap.utils;

import java.util.Comparator;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.mapviews.mapitems.MapItem3D;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Genre;
import info.interactivesystems.musicmap.entities.Track;

public final class Comparators {

    public static final GenreMapItemTitleComparator GENRE_MAP_ITEM_TITLE_COMPARATOR = new GenreMapItemTitleComparator();
    public static final GenreMapItemPositionComparator GENRE_MAP_ITEM_POSITION_COMPARATOR = new GenreMapItemPositionComparator();
    public static final ArtistNameComparator ARTIST_NAME_COMPARATOR = new ArtistNameComparator();
    public static final MapItemZComparator<ArtistMapItem> ARTIST_Z_COMPARATOR = new MapItemZComparator<>();
    public static final MapItemZComparator<GenreMapItem> GENRE_Z_COMPARATOR = new MapItemZComparator<>();
    public static final TrackPopularityComparator TRACK_POPULARITY_COMPARATOR = new TrackPopularityComparator();

    private static class GenreMapItemTitleComparator implements Comparator<GenreMapItem> {
	@Override
	public int compare(GenreMapItem mapItem1, GenreMapItem mapItem2) {
	    if (mapItem1.getUserData() == null || mapItem2.getUserData() == null) {
		throw new RuntimeException("Cannot compare GenreMapItems because userData is null");
	    }
	    return ((Genre) mapItem1.getUserData()).getTitle().compareTo(((Genre) mapItem2.getUserData()).getTitle());
	}

    }

    private static class GenreMapItemPositionComparator implements Comparator<GenreMapItem> {
	@Override
	public int compare(GenreMapItem gmi1, GenreMapItem gmi2) {
	    return Integer.compare(gmi1.getPosition(), gmi2.getPosition());
	}
    }

    private static class ArtistNameComparator implements Comparator<ArtistMapItem> {
	@Override
	public int compare(ArtistMapItem a1, ArtistMapItem a2) {
	    if (a1.getUserData() == null || a2.getUserData() == null) {
		throw new RuntimeException("Cannot compare MusicMapItems because userData is null");
	    }
	    return ((Artist) a1.getUserData()).getName().compareTo(((Artist) a2.getUserData()).getName());
	}

    }

    private static class MapItemZComparator<M extends MapItem3D> implements Comparator<M> {

	@Override
	public int compare(M mapItem1, M mapItem2) {
	    return Double.compare(mapItem1.getZ(), mapItem2.getZ());
	}

    }

    private static class TrackPopularityComparator implements Comparator<Track> {
	@Override
	public int compare(Track track1, Track track2) {
	    return Integer.compare(track1.getPopularity(), track2.getPopularity());
	}

    }

}
