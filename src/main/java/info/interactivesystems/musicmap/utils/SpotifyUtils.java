package info.interactivesystems.musicmap.utils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.enums.Action;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.NotFoundException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.special.Actions;
import com.wrapper.spotify.model_objects.specification.Recommendations;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import com.wrapper.spotify.model_objects.specification.User;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.player.SeekToPositionInCurrentlyPlayingTrackRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;

import info.interactivesystems.musicmap.MusicPlayerBean;
import info.interactivesystems.musicmap.entities.Track;
import info.interactivesystems.musicmap.utils.json.JsonUtils;

@Named
@SessionScoped
public class SpotifyUtils implements Serializable {
    private static final long serialVersionUID = 5625000633374845051L;
    private static final Logger logger = LoggerFactory.getLogger(SpotifyUtils.class);

    private static final String clientId = ResourceBundle.getBundle("privateKeys").getString("spotify_client_id");
    private static final String clientSecret = ResourceBundle.getBundle("privateKeys").getString("spotify_client_secret");
    private static final URI redirectUri = SpotifyHttpManager.makeUri(ResourceBundle.getBundle("privateKeys").getString("spotify_redirect_uri"));

    private String deviceId;

    private Calendar accessTokenTimeout;

    @Inject
    private MusicPlayerBean musicPlayerBean;

    private SpotifyApi spotifyApi;

    @PostConstruct
    private void init() {
	spotifyApi = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret).setRedirectUri(redirectUri).build();
    }

    public void authorize(String code) {
	AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
	AuthorizationCodeCredentials authorizationCodeCredentials;
	try {
	    authorizationCodeCredentials = authorizationCodeRequest.execute();
	    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
	    spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

	    int tokenExpiresIn = authorizationCodeCredentials.getExpiresIn();
	    updateAccessTokenTimeout(tokenExpiresIn);

	    logger.debug("User authorized. Access token expires in {}.", tokenExpiresIn);
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    throw new RuntimeException("Cannot authorize user", e);
	}
    }

    private void updateAccessTokenTimeout(int timeSecs) {
	accessTokenTimeout = Calendar.getInstance();
	accessTokenTimeout.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + timeSecs * 1000);
    }

    public User requestUser() throws ParseException, SpotifyWebApiException, IOException {
	checkAccessToken();
	return spotifyApi.getCurrentUsersProfile().build().execute();
    }

    public static String getAuthorizationCodeUriRequest() {
	SpotifyApi spotifyAuthorizationApi = new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret).setRedirectUri(redirectUri).build();
	AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyAuthorizationApi.authorizationCodeUri().scope(
		"streaming, user-read-email, user-read-private, user-read-playback-state, user-modify-playback-state, user-library-read, user-top-read, user-read-recently-played")
		.build();
	return authorizationCodeUriRequest.execute().toString();
    }

    private void checkAccessToken() {
	if (Calendar.getInstance().after(accessTokenTimeout)) {
	    logger.info("Access token expired... requesting new.");
	    refreshAccessToken();
	}
    }

    private void refreshAccessToken() {
	AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
	try {
	    AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
	    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

	    int tokenExpiresIn = authorizationCodeCredentials.getExpiresIn();
	    updateAccessTokenTimeout(tokenExpiresIn);

	    logger.debug("User re-authorized. New access token expires in {}.", tokenExpiresIn);
	} catch (IOException | SpotifyWebApiException | ParseException e) {
	    logger.error("Cannot refresh session.", e);
	}
    }

    public String getAccessToken() {
	checkAccessToken();
	return spotifyApi.getAccessToken();
    }

    public String getDeviceId() {
	return deviceId;
    }

    public void setDeviceId(String deviceId) {
	checkAccessToken();
	this.deviceId = deviceId;
    }

    public int getCurrentSongPosition() {
	checkAccessToken();
	int currentSongPosition = 0;
	try {
	    CurrentlyPlayingContext currentlyPlayingContext = spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
	    if (currentlyPlayingContext != null) {
		currentSongPosition = currentlyPlayingContext.getProgress_ms();
	    }
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    logger.error("Cannot retreive info about current track.", e);
	}
	return currentSongPosition;
    }

    public void startTrack(Track track) {
	checkAccessToken();
	try {
	    spotifyApi.startResumeUsersPlayback().uris(JsonUtils.toJsonArray(track.getUri())).device_id(deviceId).build().execute();
	} catch (NotFoundException nfe) {
	    logger.error("Device not found", nfe);
	    musicPlayerBean.handleDeviceNotFound();
	} catch (ParseException | JsonSyntaxException | SpotifyWebApiException | IOException e) {
	    logger.error("Cannot play track.", e);
	}
    }

    public void resumeTrack(String uri, int offset) {
	checkAccessToken();
	try {
	    logger.trace("Send command to start playback to spotify");
	    spotifyApi.startResumeUsersPlayback().uris(JsonUtils.toJsonArray(uri)).device_id(deviceId).position_ms(offset).build().execute();
	    logger.trace("Spotify start playback request successful.");
	} catch (NotFoundException nfe) {
	    logger.error("Device not found", nfe);
	    musicPlayerBean.handleDeviceNotFound();
	} catch (ParseException | JsonSyntaxException | SpotifyWebApiException | IOException e) {
	    logger.error("Cannot play track.", e.getMessage());
	    musicPlayerBean.handleNotPlayable(uri);
	}
    }

    public void pausePlayback() {
	checkAccessToken();
	try {
	    CurrentlyPlayingContext currentlyPlayingContext = spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
	    if (currentlyPlayingContext != null) {
		logger.trace("Is playing: {}", currentlyPlayingContext.getIs_playing());
		Actions actions = currentlyPlayingContext.getActions();
		EnumSet<Action> disallowedActions = actions.getDisallows().getDisallowedActions();
		logger.trace("Disallowed actions: {}", disallowedActions);
		logger.trace("Playing type: {}", currentlyPlayingContext.getCurrentlyPlayingType());
		logger.trace("Device: {}", currentlyPlayingContext.getDevice());
		logger.trace("Playing item: {}", currentlyPlayingContext.getItem());
		logger.trace("Current progress (ms): {}", currentlyPlayingContext.getProgress_ms());
		if (!disallowedActions.contains(Action.SEEKING)) { // Workaround for when several songs are being called (pause is not allowed without permission to seek)
		    spotifyApi.pauseUsersPlayback().device_id(deviceId).build().execute();
		}
	    }
	} catch (NotFoundException nfe) {
	    logger.error("Device not found", nfe);
	    musicPlayerBean.handleDeviceNotFound();
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    logger.error("Cannot pause.", e);
	}
    }

    public void moveToPosition(int milis) {
	checkAccessToken();
	SeekToPositionInCurrentlyPlayingTrackRequest seekToPositionInCurrentlyPlayingTrackRequest = spotifyApi.seekToPositionInCurrentlyPlayingTrack(milis).device_id(deviceId)
		.build();
	try {
	    CurrentlyPlayingContext currentlyPlayingContext = spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
	    if (currentlyPlayingContext != null) {
		Actions actions = currentlyPlayingContext.getActions();
		EnumSet<Action> disallowedActions = actions.getDisallows().getDisallowedActions();
		if (!disallowedActions.contains(Action.SEEKING)) { // Workaround for when several songs are being called (pause is not allowed without permission to seek)
		    seekToPositionInCurrentlyPlayingTrackRequest.execute();
		}
	    }
	} catch (NotFoundException nfe) {
	    logger.error("Device not found", nfe);
	    musicPlayerBean.handleDeviceNotFound();
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    logger.error("Connot move to song position.", e);
	}
    }

    public com.wrapper.spotify.model_objects.specification.Track getSpotifyTrack(String uri) {
	checkAccessToken();
	com.wrapper.spotify.model_objects.specification.Track track = null;
	GetTrackRequest getTrackRequest = spotifyApi.getTrack(uriToId(uri)).market(CountryCode.US).build();
	try {
	    track = getTrackRequest.execute();
	} catch (IOException | SpotifyWebApiException | ParseException e) {
	    logger.error("No track received.", e);
	}

	return track;
    }

    public List<String> getRecommendationUris(List<Track> seedTracks, int howMany) {
	checkAccessToken();
	Collections.shuffle(seedTracks);
	List<String> recommendationUris = new ArrayList<String>();
	if (seedTracks.size() > 5) {
	    for (int i = 0; i < seedTracks.size(); i += 5) {
		recommendationUris.addAll(getRecommendationUris(seedTracks.subList(i, i + 5 < seedTracks.size() ? i + 5 : seedTracks.size()), howMany));

	    }
	    Collections.shuffle(recommendationUris);
	    return recommendationUris.subList(0, howMany);
	}
	try {
	    com.wrapper.spotify.requests.data.browse.GetRecommendationsRequest.Builder requestBuilder = spotifyApi.getRecommendations().limit(howMany).market(CountryCode.US);
	    StringBuilder stringBuilder = new StringBuilder();
	    String prefix = "";
	    for (Track seedTrack : seedTracks) {
		stringBuilder.append(prefix);
		stringBuilder.append(uriToId(seedTrack.getUri()));
		prefix = ",";
	    }
	    requestBuilder = requestBuilder.seed_tracks(stringBuilder.toString());
	    Recommendations recommendations = requestBuilder.build().execute();

	    for (TrackSimplified recommendation : recommendations.getTracks()) {
		recommendationUris.add(recommendation.getUri());
	    }
	} catch (ParseException | SpotifyWebApiException | IOException e) {
	    logger.error("Cannot receive recommendations.", e);
	}
	return recommendationUris;
    }

    public static String uriToId(String uri) {
	String id = uri.substring(uri.lastIndexOf(":") + 1);
	return id;
    }

}
