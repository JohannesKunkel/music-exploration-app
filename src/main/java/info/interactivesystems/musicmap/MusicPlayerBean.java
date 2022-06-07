package info.interactivesystems.musicmap;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.SlideEndEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wrapper.spotify.model_objects.specification.Track;

import info.interactivesystems.musicmap.utils.SpotifyUtils;

@Named
@ViewScoped
public class MusicPlayerBean implements Serializable {
    private static final long serialVersionUID = 6465469890384351695L;
    private static final Logger logger = LoggerFactory.getLogger(MusicPlayerBean.class);

    private boolean deviceReady;
    private boolean isPlaying;
    private int currentPosition;
    private Track currentSpotifyTrack;

    @Inject
    private DetailsBean detailsBean;

    @Inject
    private SpotifyUtils spotifyUtils;

    @PostConstruct
    public void init() {
	isPlaying = false;
	deviceReady = false;
	currentPosition = 0;
	if (detailsBean.getCurrentTrack() != null) {
	    currentSpotifyTrack = spotifyUtils.getSpotifyTrack(detailsBean.getCurrentTrack().getUri());
	}
    }

    public void handleNotPlayable(String spotifyUri) {
	logger.warn("SpotifyUri not playable: {}", spotifyUri);
	FacesContext.getCurrentInstance().addMessage(null,
		new FacesMessage(FacesMessage.SEVERITY_WARN, "Cannot play song.", "Unfortunately, this song is not available in your country/region."));
	stopPlayback();
    }

    public void handleDeviceNotFound() {
	logger.warn("Device not found: {}.", spotifyUtils.getDeviceId());
	FacesContext.getCurrentInstance().addMessage(null,
		new FacesMessage(FacesMessage.SEVERITY_WARN, "Web player not found.", "There seems to be a problem with the Spotify Web Player in your browser. Please try "
			+ "reloading the page and contact the supervisor of this study in case the problem remains."));
    }

    public void unregisterWebPlayer() {
	logger.debug("Unregister web player");
	spotifyUtils.setDeviceId("");
	this.deviceReady = false;
    }

    public void registerWebPlayer() {
	FacesContext context = FacesContext.getCurrentInstance();
	Map<String, String> map = context.getExternalContext().getRequestParameterMap();
	String deviceId = map.get("deviceId");
	logger.debug("Register device with id: {}", deviceId);
	spotifyUtils.setDeviceId(deviceId);
	this.deviceReady = true;
    }

    public void changeTrack(String uri) {
	logger.trace("Stop playback");
	stopPlayback();
	logger.trace("Fetch spotify track.");
	currentSpotifyTrack = spotifyUtils.getSpotifyTrack(uri);
	logger.trace("Resume playback.");
	resumePlayback();
    }

    public void stopPlayback() {
	pausePlayback();
	currentPosition = 0;
    }

    public void pausePlayback() {
	if (isPlaying) {
	    spotifyUtils.pausePlayback();
	    isPlaying = false;
	}
    }

    public void resumePlayback() {
	if (currentSpotifyTrack != null && deviceReady) {
	    logger.trace("Log playing song.");
	    logger.trace("Send command to resume play to spotify.");
	    spotifyUtils.resumeTrack(currentSpotifyTrack.getUri(), currentPosition);
	    logger.trace("Everything done. Song should be playing by now.");
	    isPlaying = true;
	}
    }

    public void changeCurrentTrackPositionBySlider(SlideEndEvent event) {
	logger.debug("Slide Ended. Changed position to: {}", event.getValue());
	int milisToMoveTo = percentToMilis((int) Math.round(event.getValue()));
	logger.debug("Move to song position: {}", milisToTime(milisToMoveTo));
	currentPosition = milisToMoveTo;
	if (isPlaying) {
	    spotifyUtils.moveToPosition(milisToMoveTo);
	}
    }

    public int getCurrentPositionPercent() {
	return milisToPercent(currentPosition);
    }

    public String getFormattedCurrentTrackPosition() {
	return milisToTime(currentPosition);
    }

    public String getTotalTrackDuration() {
	return milisToTime(currentSpotifyTrack.getDurationMs());
    }

    private String milisToTime(int milis) {
	Date date = new Date(milis);
	DateFormat formatter = new SimpleDateFormat("mm:ss");
	return formatter.format(date);
    }

    public void setCurrentPositionPercent(int trackPercentToMoveTo) {
	logger.debug("Setter for current position was called ({}).", trackPercentToMoveTo);
    }

    private int percentToMilis(int percent) {
	int milis = (percent * currentSpotifyTrack.getDurationMs()) / 100;
	return milis;
    }

    private int milisToPercent(int milis) {
	return (int) Math.round((((double) currentPosition) / currentSpotifyTrack.getDurationMs()) * 100);
    }

    public void updateTimeMilies() {
	currentPosition += 1000;
	if (currentPosition >= currentSpotifyTrack.getDurationMs()) {
	    stopPlayback();
	    return;
	}
	if (currentPosition % 3000 == 0) {
	    currentPosition = spotifyUtils.getCurrentSongPosition();
	}
    }

    public com.wrapper.spotify.model_objects.specification.Track getCurrentSpotifyTrack() {
	return currentSpotifyTrack;
    }

    public boolean isPlaying() {
	return isPlaying;
    }

    public boolean isDeviceReady() {
	return deviceReady;
    }

    public void setDeviceReady(boolean deviceReady) {
	this.deviceReady = deviceReady;
    }
}
