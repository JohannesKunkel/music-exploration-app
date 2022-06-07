package info.interactivesystems.musicmap.dao;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;

@Named
@Stateless
public class MapDAO {
    private static final Logger log = LoggerFactory.getLogger(MapDAO.class);

    @PersistenceContext(unitName = "mapitem-pu")
    private EntityManager em;

    public Collection<ArtistMapItem> getAllMapItems() {
	log.debug("Fetching all map items...");
	Collection<ArtistMapItem> allMapItems = new ArrayList<>();
	Query query = em.createQuery("from ArtistMapItem");
	for (Object singleResult : query.getResultList()) {
	    allMapItems.add((ArtistMapItem) singleResult);
	}
	log.debug("Done fetching map items. Got " + allMapItems.size() + ".");
	em.clear(); // detach mapitems from persistency context so no changes will be persisted
	return allMapItems;
    }

    public Collection<GenreMapItem> getAllGenres() {
	log.debug("Fetching all genres...");
	Collection<GenreMapItem> genres = new ArrayList<GenreMapItem>();
	Query query = em.createQuery("from GenreMapItem");
	for (Object genreMapItem : query.getResultList()) {
	    genres.add((GenreMapItem) genreMapItem);
	}
	log.debug("Done fetching genres.");
	return genres;
    }

}
