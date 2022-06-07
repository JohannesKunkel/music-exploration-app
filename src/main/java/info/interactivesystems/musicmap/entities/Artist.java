package info.interactivesystems.musicmap.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Cacheable
@Table(name = "artists")
public class Artist implements Serializable {
    private static final long serialVersionUID = -7137386646204577469L;

    @Id
    private int id;

    private String type;

    private String name;

    private String uri;

    private int popularity;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumns({ @JoinColumn(name = "owner_id", referencedColumnName = "id"), @JoinColumn(name = "owner_type", referencedColumnName = "type") })
    private Set<Image> images;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "artist_genres", joinColumns = @JoinColumn(name = "artist_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "artist_tracks", joinColumns = @JoinColumn(name = "artist_uri", referencedColumnName = "uri"), inverseJoinColumns = @JoinColumn(name = "track_uri", referencedColumnName = "uri"))
    private Collection<Track> tracks;

    public Artist() {
	super();
    }

    @Override
    public String toString() {
	StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append("Artist ").append(name).append("(").append(uri).append(") with genres:");
	String comma = " ";
	for (Genre genre : genres) {
	    stringBuilder.append(comma).append(genre.getTitle());
	    comma = ", ";
	}
	return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Artist other = (Artist) obj;
	if (id != other.id)
	    return false;
	return true;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getUri() {
	return uri;
    }

    public void setUri(String uri) {
	this.uri = uri;
    }

    @Transient
    public String getSpotifyId() {
	return uri.substring(uri.lastIndexOf(":") + 1);
    }

    public Set<Genre> getGenres() {
	return genres;
    }

    public void setGenres(Set<Genre> genres) {
	this.genres = genres;
    }

    public int getPopularity() {
	return popularity;
    }

    public void setPopularity(int popularity) {
	this.popularity = popularity;
    }

    public Set<Image> getImages() {
	return images;
    }

    public void setImages(Set<Image> images) {
	this.images = images;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public Collection<Track> getTracks() {
	return tracks;
    }

    public void setTracks(Collection<Track> tracks) {
	this.tracks = tracks;
    }
}
