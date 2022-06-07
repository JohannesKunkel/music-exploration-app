package info.interactivesystems.musicmap.dao;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Genre;
import info.interactivesystems.musicmap.entities.Track;
import info.interactivesystems.musicmap.exceptions.NoSuchItemException;

@Named
@Stateless
public class MusicDAO {
    private static final Logger log = LoggerFactory.getLogger(MusicDAO.class);

    @PersistenceContext(unitName = "musicitem-pu")
    private EntityManager em;

    public Track getTrackByUri(String uri) throws NoSuchItemException {
	Query query = em.createQuery("from Track where uri = :uri").setParameter("uri", uri);
	try {
	    Track track = (Track) query.getSingleResult();
	    return track;
	} catch (NoResultException e) {
	    throw new NoSuchItemException(uri);
	}
    }

    public Collection<Genre> getAllGenres() {
	Collection<Genre> allGenres = new ArrayList<Genre>();
	Query query = em.createQuery("from Genre");
	for (Object singleResult : query.getResultList()) {
	    allGenres.add((Genre) singleResult);
	}

	return allGenres;
    }

    public Genre getGenre(int id) {
	Genre genre = em.find(Genre.class, id);
	return genre;
    }

    public Collection<Artist> getAllArtists() {
	Collection<Artist> allArtists = new ArrayList<Artist>();
	Query query = em.createQuery("from Artist");
	for (Object singleResult : query.getResultList()) {
	    allArtists.add((Artist) singleResult);
	}

	return allArtists;
    }

    public Artist getArtist(int id) {
	log.trace("Fetching artists with id {}", id);
	Artist artist = em.find(Artist.class, id);
	log.trace("Got artist.");
	return artist;
    }

    public Artist getArtistByUri(String uri) {
	Artist artist = em.createQuery("SELECT a FROM Artist a WHERE a.uri = :uri", Artist.class).setParameter("uri", uri).getSingleResult();
	return artist;
    }
}
