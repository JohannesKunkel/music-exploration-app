package info.interactivesystems.musicmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import info.interactivesystems.musicmap.entities.Track;

@Named
@SessionScoped
public class PlaylistBean implements Serializable {
    private static final long serialVersionUID = 2757074366501928580L;

    private List<Track> playlist;

    @Inject
    private DetailsBean detailsBean;

    @PostConstruct
    private void init() {
	this.playlist = new ArrayList<Track>();
    }

    public void addCurrentTrack() {
	if (!playlist.contains(detailsBean.getCurrentTrack())) {
	    addToPlaylist(detailsBean.getCurrentTrack());
	} else {
	    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Cannot add song to playlist.",
		    "Songs can only appear once on a playlist and this song is already part of this list."));
	}
    }

    public void addToPlaylist(Track track) {
	this.playlist.add(track);

    }

    public void removeFromPlaylist(Track track) {
	this.playlist.remove(track);
    }

    public void reset() {
	playlist.clear();
    }

    public boolean isTrackOnPlaylist(Track track) {
	return this.playlist.contains(track);
    }

    public List<Track> getPlaylist() {
	return playlist;
    }

    public void setPlaylist(List<Track> playlist) {
	this.playlist = playlist;
    }

}
