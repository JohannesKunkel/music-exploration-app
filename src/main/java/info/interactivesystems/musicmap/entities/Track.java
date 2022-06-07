package info.interactivesystems.musicmap.entities;

import java.io.Serializable;
import java.util.Set;

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
@Table(name = "tracks")
public class Track implements Serializable {
    private static final long serialVersionUID = -3494398062540232322L;

    @Id
    private long id;

    private String type;

    private String title;

    private String uri;

    private int popularity;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumns({ @JoinColumn(name = "owner_id", referencedColumnName = "id"), @JoinColumn(name = "owner_type", referencedColumnName = "type") })
    private Set<Image> images;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "artist_tracks", joinColumns = @JoinColumn(name = "track_uri", referencedColumnName = "uri"), inverseJoinColumns = @JoinColumn(name = "artist_uri", referencedColumnName = "uri"))
    private Set<Artist> allArtists;

    public Track() {
	super();
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + (int) (id ^ (id >>> 32));
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
	Track other = (Track) obj;
	if (id != other.id)
	    return false;
	return true;
    }

    public long getId() {
	return id;
    }

    public void setId(long id) {
	this.id = id;
    }

    public String getTitle() {
	return title;
    }

    public void setTitle(String title) {
	this.title = title;
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

    public Artist getFirstArtist() {
	return allArtists.iterator().next();
    }

    public Set<Artist> getAllArtists() {
	return allArtists;
    }

    public Artist[] getArtistsArray() {
	return allArtists.toArray(new Artist[allArtists.size()]);
    }

    public void setAllArtists(Set<Artist> allArtists) {
	this.allArtists = allArtists;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }
}
