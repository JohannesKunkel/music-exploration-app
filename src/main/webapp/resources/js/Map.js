var INTSYS = INTSYS || {};
CameraControls.install( { THREE: THREE } );

INTSYS.MAP_CONS = {
	MAP_COLOR: '#3e3e3e',
	MAX_CAMERA_DIST: 3075,
	MIN_CAMERA_DIST: 30,
	ZOOM_MIN: 0.5, // Min zoom level to zoom out to
	ZOOM_LVL_1: 1.4, // Zoom level from which lvl 1 labels start being visible
	ZOOM_LVL_2: 2.7, // Zoom level in which lvl 1 labels are fully visible
	ZOOM_LVL_3: 4.0, // Zoom level in which all items start being shown
	ZOOM_LVL_4: 8, // Zoom level in which all items are fully visible
	ZOOM_MAX: 50
};

INTSYS.Map = function(genreHierarchy, componentId) {
	var htmlComponent = document.getElementById(componentId);
	this._htmlComponent = htmlComponent;
	this._height = htmlComponent.clientHeight;
	this._width = htmlComponent.clientWidth;
	
	this._genreHierarchy = genreHierarchy;
	this._genreRepresentatives = [[],[]];
	this._allItems = [];
    
    this._currentlySelectedCircle = null;
    
    this._preferences = [];
    this._recommendations = [];
    
    this._labels = [[],[]];
    
    this._needsUpdate = false;
    this._setupAllItems(this._genreHierarchy.children);
    this._colorRepresentatives();
    
    // setup map
    this._setupScaleFunctions();
    this._setupScene(htmlComponent);
    
    this.hasFovRectangleUpdate = true;
    this._startAnimationLoop();
};

INTSYS.Map.prototype._setupAllItems = function(genres){
	genres.forEach(genre => {
		this._genreRepresentatives[genre.level].push(...genre.representatives.map(rep => rep.id));
		if(genre.level === 1){
			genre.items.forEach(item => {
				item.userData = {"color": genre.color};
				if(!this._allItems.map(it => it.id).includes(item.id)){
					this._allItems.push(item);
				}
			});
		} else {
			this._setupAllItems(genre.children);
		}
	});
};

INTSYS.Map.prototype._colorRepresentatives = function(){
	[...this._genreHierarchy.children, ...this._genreHierarchy.children.flatMap(child => child.children)].forEach(genre => {
		genre.representatives.forEach(representative => {
			this._allItems.find(item => item.id===representative.id).userData.color = genre.color;
		});
	});
}

INTSYS.Map.prototype._setupScaleFunctions = function(){
    // scale functions for embedding
    const minX = d3.min(this._allItems, d => d.x);
    const minY = d3.min(this._allItems, d => d.y);
    const maxX = d3.max(this._allItems, d => d.x);
    const maxY = d3.max(this._allItems, d => d.y);
    
    this._xScale = d3.scaleLinear().domain([minX, maxX]).range([0, this._width]);
    this._yScale = d3.scaleLinear().domain([minY, maxY]).range([0, this._height]);
    
    // scale functions for zooming
    this._lvl1Scaler = d3.scaleLinear().domain([INTSYS.MAP_CONS.ZOOM_LVL_1, INTSYS.MAP_CONS.ZOOM_LVL_2]).range([0, 1]);
    this._lvl2Scaler = d3.scaleLinear().domain([INTSYS.MAP_CONS.ZOOM_LVL_3, INTSYS.MAP_CONS.ZOOM_LVL_4]).range([0, 1]);
}

INTSYS.Map.prototype._setupScene = function(htmlElement){
    // setup renderer
	this._renderer = new THREE.WebGLRenderer({
    	antialias: true, 
    	powerPreference: "high-performance"
    });
	this._renderer.setSize(this._width, this._height);
	this._renderer.autoClear = false;
	htmlElement.appendChild(this._renderer.domElement);
    
	// setup scene
	this._scene = new THREE.Scene();
	this._scene.background = new THREE.Color(INTSYS.CONS.BACKGROUND_COLOR);
    
	// setup cameras
	this._camera = new THREE.OrthographicCamera(0, this._width, this._height, 0, 1, 30);
	this._camera.position.set(0, 0, 20);
	this._pCamera = new THREE.PerspectiveCamera(45, this._width / this._height, 1, 2);
	this._pCamera.position.set(0, 0, 1500);
   
	// Enable dom events
	this._domEvents = new THREEx.DomEvents(this._camera, this._renderer.domElement);
    
	// setup controls
	this._cameraControls = new CameraControls(this._camera, this._renderer.domElement);
	this._cameraControls.mouseButtons.left = CameraControls.ACTION.NONE;
	this._cameraControls.mouseButtons.right = CameraControls.ACTION.NONE;
	this._cameraControls.mouseButtons.middle = CameraControls.ACTION.NONE;
	this._cameraControls.touches.one = CameraControls.ACTION.NONE;
	this._cameraControls.touches.two = CameraControls.ACTION.NONE;
	this._cameraControls.touches.three = CameraControls.ACTION.NONE;
	this._cameraControls.minZoom = INTSYS.MAP_CONS.ZOOM_MIN;
	this._cameraControls.maxZoom = INTSYS.MAP_CONS.ZOOM_MAX;
    
	this._pCameraControls = new CameraControls(this._pCamera, this._renderer.domElement);
	this._pCameraControls.mouseButtons.left = CameraControls.ACTION.TRUCK;
	this._pCameraControls.mouseButtons.right = CameraControls.ACTION.NONE;
	this._pCameraControls.mouseButtons.middle = CameraControls.ACTION.NONE;
	this._pCameraControls.touches.one = CameraControls.ACTION.TOUCH_TRUCK;
	this._pCameraControls.touches.two = CameraControls.ACTION.NONE;
	this._pCameraControls.touches.three = CameraControls.ACTION.NONE;
	this._pCameraControls.dollyToCursor = true;
	this._pCameraControls.minDistance = INTSYS.MAP_CONS.MIN_CAMERA_DIST;
	this._pCameraControls.maxDistance = INTSYS.MAP_CONS.MAX_CAMERA_DIST;
	this._pCameraControls.setBoundary(new THREE.Box3({"x": -this._width/2, "y": -this._height/2, "z": -Infinity}, {"x": this._width/2, "y": this._height/2, "z": Infinity}))
   
	this._clock = new THREE.Clock();
   
  	this._createGroundPlaneMesh();
	this._createLabels(this._genreHierarchy.children);
	this._createCircleGeometries();
	this._createCircles();
}

INTSYS.Map.prototype._createGroundPlaneMesh = function(){
	const planeMaterial = new THREE.MeshBasicMaterial({
		color: INTSYS.MAP_CONS.MAP_COLOR
	});
   	const planeWidth = this._width;
   	const planeHeight = this._height;
   	const planeGeom = new THREE.PlaneGeometry(planeWidth, planeHeight, 1, 1);
   	const planeMesh = new THREE.Mesh(planeGeom, planeMaterial);
   	planeMesh.position.x = (planeWidth / 2);
   	planeMesh.position.y = (planeHeight / 2);
   	planeMesh.position.z = -9;
	
    const outlines = [[0,0], [0, this._height], [this._width, this._height], [this._width, 0]];
    const maskShape = new THREE.Shape();

    let first = true;
    outlines.forEach(d => {
        if(first){
            first = false;
            maskShape.moveTo(d[0], d[1]);
        } else {
            maskShape.lineTo(d[0], d[1]);
        }
    });

    const hPoints = this._allItems.map(point => {
        return [point.x, point.y];
    });
   
    const hullPoints = hull(hPoints, 26)
    const hole = new THREE.Shape();
    first = true
    hullPoints.forEach(point => {
        if(first){
            first = false;
            hole.moveTo(this._xScale(point[0]), this._yScale(point[1]));
        }
        else {
            hole.lineTo(this._xScale(point[0]), this._yScale(point[1]));
        }
    });

    maskShape.holes.push(hole);

    const maskGeometry = new THREE.ShapeBufferGeometry( maskShape );
    const maskMaterial = new THREE.MeshBasicMaterial( { color: INTSYS.CONS.BACKGROUND_COLOR} );
    const maskMesh = new THREE.Mesh( maskGeometry, maskMaterial );
    maskMesh.position.z = -8;
    
    this.maskMesh = maskMesh;
    
    this._scene.add(planeMesh);
    this._scene.add(maskMesh);
    
    this._createMinimapComponents(planeMesh, maskMesh);
    
}

INTSYS.Map.prototype._createMinimapComponents = function(planeMesh, maskMesh){
	this._minimapScene = new THREE.Scene();
    this._minimapScene.background = new THREE.Color(INTSYS.CONS.BACKGROUND_COLOR);
    
    this._minimapCamera = new THREE.OrthographicCamera(0, this._width, this._height, 0, 1, 30);
	this._minimapCamera.position.set(0, 0, 20);
	
	this._minimapScene.add(planeMesh.clone());
    this._minimapScene.add(maskMesh.clone());
    
    const geo = new THREE.PlaneBufferGeometry( 1, 1 );
    
    var canvas = document.createElement('canvas');
	canvas.width  = this._width;
	canvas.height = this._height;
	
	var context = canvas.getContext("2d");
	context.globalAlpha = 0.5;
	context.rect(0, 0, canvas.width, canvas.height);
	context.fillStyle = "black";
	context.fill();
	
	
    var rectangleTexture = new THREE.Texture( canvas );
	rectangleTexture.needsUpdate = true;
    
	const mat = new THREE.MeshBasicMaterial( {transparent: true, map: rectangleTexture } );
	this._rectangleMesh = new THREE.Mesh( geo, mat );
	this._rectangleMesh.position.x = (this._width / 2);
   	this._rectangleMesh.position.y = (this._height / 2);
	this._minimapScene.add(this._rectangleMesh)
	
	this._minimapRenderer = new THREE.WebGLRenderer({
        antialias: true
    });
    this._minimapRenderer.setSize(this._width, this._height);
    this._minimapRenderer.setPixelRatio(window.devicePixelRatio);
}

INTSYS.Map.prototype._createLabel = function(labelText, fontSize, visibility, color = INTSYS.CONS.LABEL_COLORS[0]){
	let label = new SpriteText(labelText, fontSize, color);
   	label.fontWeight = "bold";
    label.material.opacity =  visibility ? 1 : 0;
    label.material.transparent = true;
    return label;
}

INTSYS.Map.prototype._addLabel = function(label, x, y, scene){
	const labelGroup = new THREE.Group();
    labelGroup.add(label);
	labelGroup.position.x = this._xScale(x);
    labelGroup.position.y = this._yScale(y);
    labelGroup.position.z = 0.1
    scene.add(labelGroup); 
    return labelGroup;
}

INTSYS.Map.prototype._createLabels = function(genres){
    genres.forEach(genre => {
    	let label = this._createLabel(genre.name, 50 - genre.level * 32, genre.level === 0, genre.color); 
    	let labelGroup = this._addLabel(label, genre.x, genre.y, this._scene);
    	this._labels[genre.level].push(labelGroup);
    	if(genre.level === 0){
    		let largerLabel = this._createLabel(genre.name, 80, true, genre.color);
    		this._addLabel(largerLabel, genre.x, genre.y, this._minimapScene);
	    	this._createLabels(genre.children);
    	}
    });
}

INTSYS.Map.prototype._createCircleGeometries = function(){
    this._ringMaterial = new THREE.MeshBasicMaterial({ color: "black" });
    this._circleMaterial = new THREE.MeshBasicMaterial({color: "white"});
    this._preferenceCircleMaterial = new THREE.MeshBasicMaterial({color: INTSYS.CONS.PREFERENCE_COLOR});
    this._recommendedCircleMaterial = new THREE.MeshBasicMaterial({color: INTSYS.CONS.RECOMMENDATION_COLOR});
    this._selectedCircleMaterial = new THREE.MeshBasicMaterial({color: INTSYS.CONS.SELECTED_COLOR});
    this._circleGeometry = new THREE.CircleBufferGeometry( 8, 30 );
}

INTSYS.Map.prototype._createCircles = function(){
    this._circles = this._allItems.map(item => {
    	const ring = new THREE.Mesh( this._circleGeometry, this._ringMaterial);
        ring.scale.multiplyScalar(1.3);
        ring.position.z = 0.2;
        const circle = new THREE.Mesh( this._circleGeometry, new THREE.MeshBasicMaterial({color: item.userData.color}));
        circle.position.z = 0.3;
        const label = this._createLabel(item.name, 16, true);
        label.position.z = 0.3;
        label.position.y = -20;
        const group = new THREE.Group();
        group.userData.id = item.id;
        group.userData.uri = item.uri;
        group.add(ring);
        group.add(circle);
        group.add(label);
        this._scene.add(group);
		group.visible = this._genreRepresentatives[0].includes(item.id);
        group.position.x = this._xScale(item.x);
        group.position.y = this._yScale(item.y);
        
        if(group.visible){
        	this._addEventListenerForCircleGroup(group);
        }
        
        return group;
    });
}

INTSYS.Map.prototype._addEventListenerForCircleGroup = function(group){
	let circle = group.children[1];
	let label = group.children[2];
	var self = this;
	group.userData.onClick = function(e){
		self._onCircleClick(e);
	}
	group.userData.onHover = function(e){
		self._onHover(group, label)
	}
	group.userData.onHoverOut = function(e){
		self._onHoverOut(group, label)
	}
	this._domEvents.addEventListener(group, "click", group.userData.onClick, false);
    this._domEvents.addEventListener(circle, "mouseover", group.userData.onHover , false);
    this._domEvents.addEventListener(label, "mouseover", group.userData.onHover, false);
    this._domEvents.addEventListener(circle, "mouseout", group.userData.onHoverOut , false);
    this._domEvents.addEventListener(label, "mouseout", group.userData.onHoverOut, false);
}

INTSYS.Map.prototype._removeEventListenerForCircleGroup = function(group){
	let circle = group.children[1];
	let label = group.children[2];
	this._domEvents.removeEventListener(group, "click", group.userData.onClick, false);
    this._domEvents.removeEventListener(circle, "mouseover", group.userData.onHover, false);
    this._domEvents.removeEventListener(label, "mouseover", group.userData.onHover, false);
    this._domEvents.removeEventListener(circle, "mouseout", group.userData.onHoverOut, false);
    this._domEvents.removeEventListener(label, "mouseout", group.userData.onHoverOut, false);
}

INTSYS.Map.prototype._onCircleClick = function(e){
	let selectedGroup = e.target;
	//console.log("Switch to: ", selectedGroup.userData.id);
    this._selectCircleFromMap(selectedGroup);
}

INTSYS.Map.prototype._onHover = function(group, label){
	this._highlightGroup(group, label);
}

INTSYS.Map.prototype._highlightGroup = function(group, label){
	group.scale.multiplyScalar(2);
    group.position.z = 3;
    group.userData.needsRescaling = true;
    if(label){
	    label.userData.formerOpacity = label.material.opacity;
	    label.material.opacity = 1;
    }
}

INTSYS.Map.prototype._onHoverOut = function(group, label){
	this._unhighlightGroup(group, label);
}

INTSYS.Map.prototype._unhighlightGroup = function(group, label){
	if(group.userData.needsRescaling){
		group.scale.multiplyScalar(0.5);
	}
    group.position.z = 0.3;
    if(label){
    	label.material.opacity = label.userData.formerOpacity;
    }
}

INTSYS.Map.prototype._selectCircleFromMap = function(circle){
	this.selectCircle(circle.userData.id, false);
    showArtist([ {
		name : "artistId",
		value : "" + circle.userData.id
	} ]);
}

INTSYS.Map.prototype.selectCircle = function(artistId, moveToPosition){
	if(this._currentlySelectedCircle){
		this._currentlySelectedCircle.children[2].text = this._currentlySelectedCircle.children[2].text.replaceAll(" (selected)", "");
		this._currentlySelectedCircle.children[2].color = INTSYS.CONS.LABEL_COLORS[0];
		this._currentlySelectedCircle.children[2].strokeWidth = 0;
	}
	this._currentlySelectedCircle = this._getCircleByArtistId(artistId);
	if(this._currentlySelectedCircle){
		this._currentlySelectedCircle.children[2].color = INTSYS.CONS.SELECTED_COLOR;
		this._currentlySelectedCircle.children[2].strokeWidth = 1;
		this._currentlySelectedCircle.children[2].strokeColor = "black";
		this._currentlySelectedCircle.children[2].text = this._currentlySelectedCircle.children[2].text + " (selected)";
		this._needsUpdate = true;
		if(moveToPosition){
			let x = this._currentlySelectedCircle.position.x - (this._width/2);
			let y = this._currentlySelectedCircle.position.y - (this._height/2);
			this._pCameraControls.moveTo(x, y, this._pCamera.position.z, true);
		}
	}
}

INTSYS.Map.prototype._getCircleByArtistId = function(artistId){
	return this._circles.find(c => c.userData.id === artistId);
}

INTSYS.Map.prototype.refreshPreferences = function(preferenceIds){
	let oldPreferences = this._preferences;
	this._preferences = [];
	oldPreferences.forEach(artistId => {
		var circle = this._getCircleByArtistId(artistId);
		circle.children[2].text = circle.children[2].text.replaceAll(" (liked)", "");
		if(this._recommendations.includes(circle.userData.id)){
			circle.children[2].color = INTSYS.CONS.RECOMMENDATION_COLOR;
		} else {
			circle.children[2].color = INTSYS.CONS.LABEL_COLORS[0];
			circle.children[2].strokeWidth = 0;
		}
	});
	this._preferences = preferenceIds;
	this._circles.forEach(circle => {
		if(preferenceIds.includes(circle.userData.id)){
			circle.children[2].color = INTSYS.CONS.PREFERENCE_COLOR;
			circle.children[2].strokeWidth = 1;
			circle.children[2].strokeColor = "black";
			circle.children[2].text = circle.children[2].text + " (liked)";
		}
	});
	this._needsUpdate = true;
}

INTSYS.Map.prototype.refreshRecommendations = function(recommendationIds){
	let oldRecommendations = this._recommendations;
	this._recommendations = [];
	oldRecommendations.forEach(trackId => {
		var circle = this._getCircleByArtistId(trackId);
		circle.children[2].text = circle.children[2].text.replaceAll(" (recommended)", "");
		if(this._preferences.includes(circle.userData.id)){
			circle.children[2].color = INTSYS.CONS.PREFERENCE_COLOR;
		} else {
			circle.children[2].color = INTSYS.CONS.LABEL_COLORS[0];
			circle.children[2].strokeWidth = 0;
		}
	});
	this._recommendations = recommendationIds;
	this._circles.forEach(circle => {
		if(recommendationIds.includes(circle.userData.id)){
			circle.children[2].color = INTSYS.CONS.RECOMMENDATION_COLOR;
			circle.children[2].strokeWidth = 1;
			circle.children[2].strokeColor = "black";
			circle.children[2].text = circle.children[2].text + " (recommended)"
		}
	});
	this._needsUpdate = true;
}

INTSYS.Map.prototype.resetMap = function(){
    this._cameraControls.reset(true);
    this._pCameraControls.reset(true);
}

INTSYS.Map.prototype._startAnimationLoop = function() {
	var self = this;
	(function loop() {
		self._render();
		requestAnimationFrame(loop);
	})();
};

INTSYS.Map.prototype._render = function() {

    const delta = this._clock.getDelta();
    const hasUpdate = this._pCameraControls.update(delta);
    
    if(hasUpdate || this._needsUpdate){
	    const zoomBefore = this._camera.zoom;
	    const hasZoomUpdate = this._cameraControls.update(delta);
	    this._camera.position.x = this._pCamera.position.x;
	    this._camera.position.y = this._pCamera.position.y;
	    this._circleCulling();
	    this._updateFovRectangle();
	    if(hasZoomUpdate || this._needsUpdate){
	    	const zoomDelta = this._camera.zoom - zoomBefore;
		    this._zoomCircles();
		    this._zoomLabels();
		    this._checkZoomButtons();
	    	this._needsUpdate = false;
	    }
    }
    this._renderer.clear();
    this._renderer.render(this._scene, this._camera);
};

INTSYS.Map.prototype._circleCulling = function(){
	this._circles.forEach(c =>{
		let shouldBeVisible = this._shouldCircleBeVisible(c);
		let isVisible = c.visible;
	    if(shouldBeVisible && !isVisible){
		    this._addEventListenerForCircleGroup(c);
		    c.visible = true;
	    }
	    if (!shouldBeVisible && isVisible){
	    	this._removeEventListenerForCircleGroup(c);
	    	c.visible = false;
	    }
	});
}

INTSYS.Map.prototype._zoomCircles = function(){
    this._circles.forEach(c =>{
	    this._scaleCircleDependingOnZoom(c);       
    });
};

INTSYS.Map.prototype._shouldCircleBeVisible = function(circle){

	let trackId = circle.userData.id;

	if(!this._isCircleInCameraFrustum(circle)){
		return false; 
	} 
	
	if(this._isCirclePartOfUserProfile(circle)){
		return true;
	}
	
	if(circle.userData.shouldBeVisible){
		return true;
	}
	
	if(this._genreRepresentatives[0].includes(trackId)){
		return true;
	}
		
	if(this._genreRepresentatives[1].includes(trackId) && this._camera.zoom > INTSYS.MAP_CONS.ZOOM_LVL_1){
		return true;
	}	
	
	if(this._camera.zoom > INTSYS.MAP_CONS.ZOOM_LVL_3){
		return true;
	}	
	
	return false;
}

INTSYS.Map.prototype._isCircleInCameraFrustum = function(circle){
	this._camera.updateMatrix();
	this._camera.updateMatrixWorld();
	var frustum = new THREE.Frustum();
	frustum.setFromProjectionMatrix(new THREE.Matrix4().multiplyMatrices(this._camera.projectionMatrix, this._camera.matrixWorldInverse)); 
	
	return frustum.intersectsObject(circle.children[0]);
};


INTSYS.Map.prototype._scaleCircleDependingOnZoom = function(c){
	let scaler = 0;
	if(this._isCirclePartOfUserProfile(c) || this._genreRepresentatives[0].includes(c.userData.id)){
		scaler = 1;
	}
	else if(this._genreRepresentatives[1].includes(c.userData.id)){
    	scaler = this._lvl1Scaler(this._camera.zoom) < 1 ? this._lvl1Scaler(this._camera.zoom) : 1;
    }
    else {
    	scaler = this._lvl2Scaler(this._camera.zoom)  < 1 ? this._lvl2Scaler(this._camera.zoom) : 1;
    }
    
    c.scale.x = (1/this._camera.zoom) * scaler;
    c.scale.y = (1/this._camera.zoom) * scaler;
    c.userData.needsRescaling = false;
};

INTSYS.Map.prototype._isCirclePartOfUserProfile = function(circle){
	return this._preferences.includes(circle.userData.id) || this._recommendations.includes(circle.userData.id) || this._currentlySelectedCircle === circle;
};

INTSYS.Map.prototype._zoomLabels = function(){

	this._labels[0].forEach(labelGroup =>{
        this._zoomLabel(labelGroup, 1);
    });
    
    this._labels[1].forEach(labelGroup =>{
    	let opacity = 0;
    	let label = labelGroup.children[0];
    	let shouldBeVisible = this._camera.zoom > INTSYS.MAP_CONS.ZOOM_LVL_1;
    	if(shouldBeVisible){
	    	opacity = this._lvl1Scaler(this._camera.zoom) < 1 ? this._lvl1Scaler(this._camera.zoom) : 1;
        } else {
        }
        this._zoomLabel(labelGroup, opacity);
    });
}

INTSYS.Map.prototype._zoomLabel = function(labelGroup, opacity){
	if(this._labels[0].includes(labelGroup)){
		labelGroup.children[0].material.opacity = opacity * (1 / this._camera.zoom);
	} else {
		labelGroup.children[0].material.opacity = opacity * (1 / (this._camera.zoom - INTSYS.MAP_CONS.ZOOM_LVL_1));
	}
}

INTSYS.Map.prototype._updateFovRectangle = function(){

    this._rectangleMesh.scale.y = this._height / this._camera.zoom;
    this._rectangleMesh.scale.x = this._width / this._camera.zoom;
    
    this._rectangleMesh.position.x = this._camera.position.x + (this._width / 2)
    this._rectangleMesh.position.y = this._camera.position.y + (this._height / 2)
    
    this.hasFovRectangleUpdate = true;
}

INTSYS.Map.prototype.getFovRectangleTexture = function(){
	this._minimapRenderer.render(this._minimapScene, this._minimapCamera);
    var canvasTexture = new THREE.CanvasTexture(this._minimapRenderer.domElement)
    canvasTexture.anisotropy = this._minimapRenderer.capabilities.getMaxAnisotropy();
    canvasTexture.magFilter = THREE.LinearFilter;
    canvasTexture.needsUpdate = true;
    
    return canvasTexture;
}

INTSYS.Map.prototype.moveToNormalizedPosition = function(normX, normY){
	let x = normX * this._width - (this._width/2);
	let y = normY * this._height - (this._height/2);
	
	this._pCameraControls.moveTo(x, y, this._pCamera.position.z, true);
}

INTSYS.Map.prototype._checkZoomButtons = function(){
	let zoomInButton = document.getElementById("zoomInButton");
	let zoomOutButton = document.getElementById("zoomOutButton");
	if(zoomInButton && zoomOutButton){
		zoomInButton.disabled = this._camera.zoom >= INTSYS.MAP_CONS.ZOOM_MAX;
		zoomOutButton.disabled = this._camera.zoom <= INTSYS.MAP_CONS.ZOOM_MIN;
	}
}

INTSYS.Map.prototype.zoomIn = function(){
	let speed = 1;
	let delta = -3;
	let zoomScale = Math.pow(0.95, delta * speed);
	let distanceScale = Math.pow(0.95, -delta * speed);
	let zoomDelta = this._camera.zoom * zoomScale;
	let distanceDelta = this._pCameraControls.distance * distanceScale;

	if(this._camera.zoom + zoomDelta >= INTSYS.MAP_CONS.ZOOM_MAX){
		this._cameraControls.zoomTo(INTSYS.MAP_CONS.ZOOM_MAX, false);
		this._pCameraControls.dollyTo(INTSYS.MAP_CONS.MIN_CAMERA_DIST, false);
	} else {
		this._cameraControls.zoomTo(zoomDelta, false);
		this._pCameraControls.dollyTo(distanceDelta, 0, 0);
	}
	this._needsUpdate = true;
}

INTSYS.Map.prototype.zoomOut = function(){
	let speed = 1;
	let delta = 3;
	let zoomScale = Math.pow(0.95, delta * speed);
	let distanceScale = Math.pow(0.95, -delta * speed);
	let zoomDelta = this._camera.zoom * zoomScale;
	let distanceDelta = this._pCameraControls.distance * distanceScale;
	
	if(this._camera.zoom + zoomDelta <= INTSYS.MAP_CONS.ZOOM_MIN){
		this._cameraControls.zoomTo(INTSYS.MAP_CONS.ZOOM_MIN, false);
		this._pCameraControls.dollyTo(INTSYS.MAP_CONS.MAX_CAMERA_DIST, false);
	} else {
		this._cameraControls.zoomTo(zoomDelta, false);
		this._pCameraControls.dollyTo(distanceDelta, 0, 0);
	}
	
	this._needsUpdate = true;
}
