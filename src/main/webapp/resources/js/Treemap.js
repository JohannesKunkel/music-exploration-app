var INTSYS = INTSYS || {};

INTSYS.TRMAP_CONS = {
	LINE_HEIGHT : 1.6
};

INTSYS.Treemap = function(genreHierarchy, preferences, recommendations, currentlySelected, componentId) {
	this._svg = d3.select(componentId);
	this._height = parseInt(this._svg.style("height"));
	this._width = parseInt(this._svg.style("width"));
	
	this._svg.attr("viewBox", [0, -40, this._width, this._height + 40])
      .style("font", "10px sans-serif");
	
	this._genreHierarchy = genreHierarchy;
	
	this._allItems = [];
    this._setupAllItems(this._genreHierarchy.children);
    this.setupPrefsAndRecs(preferences, recommendations);
	
	if(currentlySelected != "0"){
    	this._currentlySelectedArtist = this._findArtistById(currentlySelected);
    	this._subGenresOfSelectedArtist = this._findSubGenresForArtist(currentlySelected);
    	this._superGenresOfSelectedArtist = this._findSuperGenresForArtist(currentlySelected);
    }
       
    this._currentNode = null;
    this._sortItems();
    this._setupScaleFunctions();
    this._setupTreemap();
    
}

INTSYS.Treemap.prototype._setupAllItems = function(genreHierarchyChildren){
	genreHierarchyChildren.forEach(genre => {
		if(genre.level === 1){
			genre.items.forEach(item => {
				if(!this._allItems.map(it => it.id).includes(item.id)){
					this._allItems.push(item);
				}
			});
		} else {
			this._setupAllItems(genre.children);
		}
	});
};

INTSYS.Treemap.prototype._findArtistById = function(artistId){
	for(const item of this._allItems){
		if(item.id === artistId){
			return item;
		}
	}
}

INTSYS.Treemap.prototype.setupPrefsAndRecs = function(preferences, recommendations){
	this._preferences = preferences;
	this._recommendations = recommendations;
	this._genreDict = {}
	for (const lvl0Genre of this._genreHierarchy.children){
		for(const lvl1Genre of lvl0Genre.children){
			for(const item of lvl1Genre.items){
				let artist = {"id" : item.id, "name" : item.name};
				if(preferences.includes(artist.id)){
					this._addGenreDictEntry(lvl0Genre.id, "preferences", artist);
					this._addGenreDictEntry(lvl1Genre.id, "preferences", artist);
				}
				if(recommendations.includes(artist.id)){
					this._addGenreDictEntry(lvl0Genre.id, "recommendations", artist);
					this._addGenreDictEntry(lvl1Genre.id, "recommendations", artist);
				}
			}
		}
	}
}

INTSYS.Treemap.prototype._addGenreDictEntry = function(genreId, key, artist){
	if(!this._genreDict[genreId]){
		this._genreDict[genreId] = {};
	}
	if(this._genreDict[genreId][key]){
		if(!this._genreDict[genreId][key].some(containedArtist => containedArtist.id === artist.id)){
			this._genreDict[genreId][key].push(artist);
		}
	} else {
		this._genreDict[genreId][key] = [artist];
	}
}

INTSYS.Treemap.prototype._sortItems = function(){
	for (const lvl0Genre of this._genreHierarchy.children){
		for(const lvl1Genre of lvl0Genre.children){
			let items = [...lvl1Genre.items];
			items.sort((a, b) => a.name.localeCompare(b.name));
			lvl1Genre.items = items;
		}
	}
}

INTSYS.Treemap.prototype._setupScaleFunctions = function(){    
    this._xScale = d3.scaleLinear().rangeRound([0, this._width]);
    this._yScale = d3.scaleLinear().rangeRound([0, this._height]);
}

INTSYS.Treemap.prototype._setupTreemap = function(){
	var self = this;
	this._tile = function(node, x0, y0, x1, y1) {
  		d3.treemapSquarify(node, 0, 0, self._width, self._height);

  		let paddingRight = 40;
  		let paddingBottom = 40;
  		
  		// scale to view dimensions since Squarify does not use the entire horizontal view
  		let minX0 = d3.min(node.children, d => d.x0);
  		let maxX1 = d3.max(node.children, d => d.x1);
  		let xScale = d3.scaleLinear().domain([minX0, maxX1]).range([0, self._width - paddingRight]);
  		let minY0 = d3.min(node.children, d => d.y0);
  		let maxY1 = d3.max(node.children, d => d.y1);
  		let yScale = d3.scaleLinear().domain([minY0, maxY1]).range([0, self._height - paddingBottom]);

  		for (const child of node.children) {
  			// enlarge most right and bottom cells so that artist can be displayed
  			let scaledX1 = Math.round(xScale(child.x1));
  			if (scaledX1 === self._width - paddingRight){
  				scaledX1 = self._width;
  			}
    		child.x0 = x0 + xScale(child.x0) / self._width * (x1 - x0);
    		child.x1 = x0 + scaledX1 / self._width * (x1 - x0);
    		
    		let scaledY1 = Math.round(yScale(child.y1));
  			if (scaledY1 === self._height - paddingBottom){
  				scaledY1 = self._height;
  			}
    		child.y0 = y0 + yScale(child.y0) / self._height * (y1 - y0);
    		child.y1 = y0 + scaledY1 / self._height * (y1 - y0);
  		}
	}
	this._treemap = d3.treemap()
		.tile(this._tile)(d3.hierarchy(this._genreHierarchy)
    	.sum(d => d.value)
    	.sort((a, b) => b.value - a.value));
    	
	this._group = this._svg.append("g");
	this._currentNode = this._treemap;
	this._render();
}

INTSYS.Treemap.prototype.moveToArtist = function(artistId){
	let superGenreId = this._findSuperGenresForArtist(artistId)[0];
	let d = this._treemap.find(node => node.data.id == superGenreId);
	this._currentNode = d;
	this.selectArtist(artistId);
}

INTSYS.Treemap.prototype.selectArtist = function(artistId){
	this._currentlySelectedArtist = this._findArtistById(artistId);
	this._subGenresOfSelectedArtist = this._findSubGenresForArtist(artistId);
	this._superGenresOfSelectedArtist = this._findSuperGenresForArtist(artistId);
	
	this._reload();
}

INTSYS.Treemap.prototype._findSuperGenresForArtist = function(artistId){
	let superGenres = [];
	for (const lvl0Genre of this._genreHierarchy.children){
		for(const lvl1Genre of lvl0Genre.children){
			for(const item of lvl1Genre.items){
				if(item.id === artistId){
					superGenres.push(lvl0Genre.id);
				}
			}
		}
	}
	return superGenres;
}

INTSYS.Treemap.prototype._findSubGenresForArtist = function(artistId){
	let subGenres = [];
	for (const lvl0Genre of this._genreHierarchy.children){
		for(const lvl1Genre of lvl0Genre.children){
			for(const item of lvl1Genre.items){
				if(item.id === artistId){
					subGenres.push(lvl1Genre.id);
				}
			}
		}
	}
	return subGenres;
}

INTSYS.Treemap.prototype._render = function(){
	let root = this._currentNode;
	const node = this._group
      .selectAll("g")
      .data(root.children.concat(root))
      .join("g");

    node.append("title")
        .text( d => d === root ? this._breadcrumps(d) : d.data.name.toUpperCase())
        .attr("fill", INTSYS.CONS.LABEL_COLORS[2])
        .attr("font-size", "1.6em");

    node.append("rect")
        .attr("stroke", "#fff")
        .on("wheel", (event, d) => this._onScroll(event, d, node, root))
        .attr("class", d => d.data.level === 1 ? "scrollCursor" : "")
        .filter(d => d === root ? d.parent : d.children)
        .attr("class", d => d === root ? "backCursor" : "downCursor")
        .on("click", (event, d) => d === root ? this._zoomout(root) : this._zoomin(d));

	node.append("text")
		.attr("font-weight", "bold")
		.attr("font-size", "1.3em")
		.attr("fill",  d => d.data.color ? d.data.color : INTSYS.CONS.LABEL_COLORS[2])
		.attr("x", 5)
		.attr("y", INTSYS.TRMAP_CONS.LINE_HEIGHT + "em")
		.text(d => { 
			if(d === root){
			 	return this._breadcrumps(d);
			} 
      		return d.data.name.toUpperCase();
      	})
        .filter(d => d === root ? d.parent : d.children)
        .attr("class", d => d === root ? "backCursor" : "downCursor")
        .on("click", (event, d) => d === root ? this._zoomout(root) : this._zoomin(d));
	
	node.append("text").classed("artists", true);
		
    this._position(this._group, root);
	this._determineArtistViewport(node, root);

}

INTSYS.Treemap.prototype._determineArtistViewport = function(node, root){
	node.selectAll(".artists")
		.selectAll("tspan")
      	.data(d => {
      		if(d === root){
      			return "";
      		}
      		if(d.data.level !== 1){
      			return this._generateItemsToDisplay(d);
      		} 
   			let name = d.data.name;
      		let height = this._yScale(d.y1) - this._yScale(d.y0);
      		let textHeight = node.select("text").node().getBBox().height;
      		let lines = Math.floor(height / textHeight);
      		d.data.arOffset = d.data.arOffset ? d.data.arOffset : 0;
      		
      		let itemsToDisplay = this._generateItemsToDisplay(d);
			itemsToDisplay.push({id: -1, name: "All artists:", color: d.data.color})
      		itemsToDisplay = itemsToDisplay.concat(d.data.items);
      		itemsToDisplay.forEach(d => d.parentHeight = height);
      		return itemsToDisplay.slice(d.data.arOffset, (d.data.arOffset+2+lines) * 8);
      	})
      	.join("tspan")
      	.attr("font-size", "1.3em")
      	.attr("font-weight", d => d.id === -1 || (this._currentlySelectedArtist && this._currentlySelectedArtist.id === d.id) ? "bold" : "normal")
      	.attr("x", 5)
        .attr("y", (d, i) => INTSYS.TRMAP_CONS.LINE_HEIGHT * 12 + (i+1) * INTSYS.TRMAP_CONS.LINE_HEIGHT * 13)
        .attr("visibility", (d, i, n) => +d3.select(n[i]).attr("y") > +d.parentHeight ? "hidden" : "visible")
        .attr("fill", d => {
        	if(this._currentlySelectedArtist && this._currentlySelectedArtist.id === d.id){
        		return INTSYS.CONS.SELECTED_COLOR;
        	}
        	if(this._preferences.includes(d.id)){
        		return INTSYS.CONS.PREFERENCE_COLOR;
        	}
        	if(this._recommendations.includes(d.id)){
        		return INTSYS.CONS.RECOMMENDATION_COLOR;
        	}
        	if(d.id === -1){
        		return d.color;
        	}
        	return INTSYS.CONS.LABEL_COLORS[2];
        })
        .attr("cursor", d => d.id === -1 ? "" : "pointer")
        .on("click", (event, d) => d.id === -1 ? "" : this._selectArtist(d))
      	.text(d => {
      		if(d.id === -1){
      			return d.name;
      		}
      		return "- " + d.name + (this._preferences.includes(d.id) ? " (liked)" : "") + (this._recommendations.includes(d.id) ? " (recommended)" : "") + ((this._currentlySelectedArtist && this._currentlySelectedArtist.id === d.id) ? " (selected)" : "");
      	});
};

INTSYS.Treemap.prototype._generateItemsToDisplay = function(d){
	let itemsToDisplay = [{id:-1, name: "Sample artists:", color:d.data.color}]
    itemsToDisplay = itemsToDisplay.concat(d.data.representatives);
    if(this._currentlySelectedArtist){
	    let genresOfSelectedArtist = d.data.level !== 1 ? this._superGenresOfSelectedArtist : this._subGenresOfSelectedArtist;
	    if(genresOfSelectedArtist.includes(d.data.id)){
			itemsToDisplay.push({id:-1, name: "Currently selected artist:", color:d.data.color});
	      	itemsToDisplay.push(this._currentlySelectedArtist);
	    }
    }
    if(this._genreDict[d.data.id]){
    	if(this._genreDict[d.data.id]["preferences"]){
			itemsToDisplay.push({id:-1, name: "Artists with liked songs:", color:d.data.color});
      		itemsToDisplay = itemsToDisplay.concat(this._genreDict[d.data.id]["preferences"]);
      	}
      	if(this._genreDict[d.data.id]["recommendations"]){
      		itemsToDisplay.push({id:-1, name: "Artists with recommended songs:", color:d.data.color});
      		itemsToDisplay = itemsToDisplay.concat(this._genreDict[d.data.id]["recommendations"]);
      	}
	}
	return itemsToDisplay;
}

INTSYS.Treemap.prototype._selectArtist = function(d) {
	this.selectArtist(d.id);
	showArtist([ {
		name : "artistId",
		value : "" + d.id
	} ]);
}

INTSYS.Treemap.prototype._onScroll = function(event, d, node, root) {
	if(d.data.level === 1){
       	let name = d.data.name
    	let linePlus = event.deltaY / (INTSYS.TRMAP_CONS.LINE_HEIGHT * 20);
    	d.data.arOffset = Math.min(Math.max(d.data.arOffset ? d.data.arOffset + linePlus : linePlus, 0), d.data.value - 2);
    	
	   	this._determineArtistViewport(node, root);
    }
}

INTSYS.Treemap.prototype._position = function(group, root) {
    group.selectAll("g")
        .attr("transform", d => d === root ? 'translate(0,-30)' : 'translate('+ this._xScale(d.x0) + ', ' + this._yScale(d.y0) + ')')
      	.select("rect")
        .attr("width", d => d === root ? this._width : this._xScale(d.x1) - this._xScale(d.x0))
        .attr("height", d => d === root ? 30 : this._yScale(d.y1) - this._yScale(d.y0))
}

// When zooming in, draw the new nodes on top, and fade them in.
INTSYS.Treemap.prototype._zoomin = function(d) {
    const oldNodes = this._group.attr("pointer-events", "none");
    const newNodes = this._group = this._svg.append("g");

	// scales from nested cell size to screen size
    this._xScale.domain([d.x0, d.x1]);
    this._yScale.domain([d.y0, d.y1]);
    
    var self = this;
    var positionProxy = function(group, root){
    	self._position(group, root);
    }
    
    this._svg.transition()
        .duration(750)
        .call(t => oldNodes.transition(t).remove()
        .call(positionProxy, d.parent))
        .call(t => newNodes.transition(t)
        .attrTween("opacity", () => d3.interpolate(0, 1))
        .call(positionProxy, d));
    
	setCookie("treemapNode", d.data.name)
	this._currentNode = d;
    this._render();
}


// When zooming out, draw the old nodes on top, and fade them out.
INTSYS.Treemap.prototype._zoomout = function(d) {
    const oldNodes = this._group.attr("pointer-events", "none");
    const newNodes = this._group = this._svg.insert("g", "*");
    this._currentNode = d.parent;
    this._render();

    this._xScale.domain([d.parent.x0, d.parent.x1]);
    this._yScale.domain([d.parent.y0, d.parent.y1]);

	var self = this;
    var positionProxy = function(group, root){
    	self._position(group, root);
    }

     this._svg.transition()
        .duration(750)
        .call(t => oldNodes.transition(t).remove()
          .attrTween("opacity", () => d3.interpolate(1, 0))
          .call(positionProxy, d))
        .call(t => newNodes.transition(t)
          .call(positionProxy, d.parent));
	
	setCookie("treemapNode", d.parent.data.name);
}

INTSYS.Treemap.prototype._reload = function() {
	let d = this._currentNode;
	this._group.attr("pointer-events", "none").remove(); // remove old nodes
    const newNodes = this._group = this._svg.append("g");
    
    this._xScale.domain([d.x0, d.x1]);
    this._yScale.domain([d.y0, d.y1]);
    
    newNodes.attr("opacity", 1);
    this._position(newNodes, d);
    this._render();
}


INTSYS.Treemap.prototype._breadcrumps = function(d){
    return d.ancestors().reverse().map(d => d.data.name).join("/").toUpperCase();
}

INTSYS.Treemap.prototype.resetTreemap = function(){
	this._setupTreemap();
}