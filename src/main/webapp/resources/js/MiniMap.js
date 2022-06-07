var INTSYS = INTSYS || {};

INTSYS.Minimap = function(componentId, mainMap) {
	this._htmlComponent = document.getElementById(componentId);
	this._height = this._htmlComponent.clientHeight;
	this._width = this._htmlComponent.clientWidth;
	
	this._mainMap = mainMap;
	
    this._setupScene();
    
    this._startAnimationLoop();
};

INTSYS.Minimap.prototype._setupScene = function() {
	// setup renderer
	this._renderer = new THREE.WebGLRenderer({
    	antialias: true, 
    	powerPreference: "high-performance"
    });
	this._renderer.setSize(this._width, this._height);
	this._renderer.autoClear = false;
	this._htmlComponent.appendChild(this._renderer.domElement);
    
	// setup scene
	this._scene = new THREE.Scene();
	this._scene.background = new THREE.Color(INTSYS.CONS.BACKGROUND_COLOR);
    
	// setup camera
	this._camera = new THREE.OrthographicCamera(0, this._width, this._height, 0, 1, 30);
	this._camera.position.set(0, 0, 20);
   
	// Enable dom events
	this._domEvents = new THREEx.DomEvents(this._camera, this._renderer.domElement);
    
	this._clock = new THREE.Clock();
   
  	this._createGroundPlaneMesh();
};

INTSYS.Minimap.prototype._createGroundPlaneMesh = function(){
	let canvasTexture = this._mainMap.getFovRectangleTexture();
   	this._planeMaterial = new THREE.MeshBasicMaterial({
		map: canvasTexture
	});

   	const planeWidth = this._width;
   	const planeHeight = this._height;
   	const planeGeom = new THREE.PlaneGeometry(planeWidth, planeHeight, 1, 1);
   	const planeMesh = new THREE.Mesh(planeGeom, this._planeMaterial);
   	planeMesh.position.x = (planeWidth / 2);
   	planeMesh.position.y = (planeHeight / 2);
   	planeMesh.position.z = -9;
   	
   	this._domEvents.addEventListener(planeMesh, 'click', (event) => {
		var normalizedX = event.intersect.point.x / this._width;
		var normalizedY = event.intersect.point.y / this._height;
		this._mainMap.moveToNormalizedPosition(normalizedX, normalizedY);
	}, false)
	
    this._scene.add(planeMesh);
}

INTSYS.Minimap.prototype.updateFovRectangle = function(){
    this._planeMaterial.map = this._mainMap.getFovRectangleTexture();
    this._mainMap.hasFovRectangleUpdate = false;
}

INTSYS.Minimap.prototype._startAnimationLoop = function() {
	var self = this;
	(function loop() {
		self._render();
		requestAnimationFrame(loop);
	})();
};

INTSYS.Minimap.prototype._render = function() {
	var dt = this._clock.getDelta();
	this._renderer.autoClear = false;
	this._renderer.clear();
	if(this._mainMap.hasFovRectangleUpdate){
		this.updateFovRectangle();
	}
	this._renderer.render(this._scene, this._camera);
};
