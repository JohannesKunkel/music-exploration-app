var INTSYS = INTSYS || {};

var currentState = null;

INTSYS.SpotifyPlayer = function(accessToken) {
	player = new Spotify.Player({
		name: 'MusicRadioPlayer',
    	getOAuthToken: cb => { cb(accessToken); }
  	});
 

	// Error handling
	player.addListener('initialization_error', ({ message }) => { console.log("initialization_error"); console.error(message); });
	player.addListener('authentication_error', ({ message }) => { console.log("authentication_error"); console.error(message); });
	player.addListener('account_error', ({ message }) => { 
		console.log("account_error"); 
		console.error(message); 
		alert("To use this app, you need to be registered with a Spotify Primium (or family) account."); 
	});
	player.addListener('playback_error', ({ message }) => { console.log("playback_error"); console.error(message); });

	// Playback status updates
	player.addListener('player_state_changed', state => { 
		if((!currentState || currentState.paused) && (state && !state.paused)){
			startPositionInterval();
			 $('#playSongOverlay').show();
		}
		currentState = state; 
	});

	// Ready
	player.addListener('ready', ({ device_id }) => {
		console.log('Ready with Device ID', device_id);
		registerWebPlayer([ {
			name : "deviceId",
			value : "" + device_id
		} ]);
	});

	// Not Ready
	player.addListener('not_ready', ({ device_id }) => {
		console.log('Device ID has gone offline', device_id);
		unregisterWebPlayer();
	});
	
	// Connect to the player!
	player.connect();
};
