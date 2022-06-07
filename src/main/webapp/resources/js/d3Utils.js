d3.selection.prototype.first = function() {  
	return d3.select(this.nodes()[0]);  
};

d3.selection.prototype.last = function() {
   return d3.select(this.nodes()[this.size() - 1]); 
};