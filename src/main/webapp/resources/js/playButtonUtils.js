var trackPositionInterval;

function startPositionInterval(){
	if(!trackPositionInterval){
		trackPositionInterval = setInterval(updatePosition, 1000);
	}
}

function updatePosition(){
	if(currentState && !currentState.loading && currentState.position > 0){
		updateTrackPosition();
		$('#playSongOverlay').hide();
	}
	if(currentState && currentState.paused){
		stopPositionInterval();
	}
}

function stopPositionInterval(){
	clearInterval(trackPositionInterval);
	trackPositionInterval = null;
}

var wasRunning = false;

function onSlideStart(){
	if(trackPositionInterval){
		wasRunning = true;
		stopPositionInterval();
	} else {
		wasRunning = false;
	}
}

function onSlideEnd(){
	if(wasRunning){
		startPositionInterval();
	}
}