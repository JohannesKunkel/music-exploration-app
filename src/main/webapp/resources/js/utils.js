function euclid(a, b){
  	return Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
}

function scalePosition(scaleFunctionX, scaleFunctionY, point){
	return {x: scaleFunctionX(point.x), y: scaleFunctionY(point.y)}
}

function resizeDialogHeight(){
	var htmlTag = document.getElementById('helpDialog');
	htmlTag.style.height = Math.floor(window.innerHeight)+"px";
}

function setCookie(cname, cvalue) {
  var d = new Date();
  d.setTime(d.getTime() + (24 * 60 * 60 * 1000));
  var expires = "expires="+d.toUTCString();
  document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}

function deleteCookie(cname){
	var cookie = getCookie(cname);
	if(cookie){
		document.cookie = cname + "=;path=/;expires=Thu, 01 Jan 1970 00:00:01 GMT"
	}
}
