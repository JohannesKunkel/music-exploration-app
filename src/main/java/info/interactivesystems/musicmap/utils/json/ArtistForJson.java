package info.interactivesystems.musicmap.utils.json;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.musicmap.entities.Artist;

public class ArtistForJson {
    private int id;
    private String name;
    private String uri;
    private double x;
    private double y;

    public ArtistForJson() {
    }

    public ArtistForJson(ArtistMapItem artistMapItem) {
	Artist artist = (Artist) artistMapItem.getUserData();
	this.id = artist.getId();
	this.uri = artist.getUri();
	this.name = artist.getName();
	this.x = artistMapItem.getX();
	this.y = artistMapItem.getY();
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public String getUri() {
	return uri;
    }

    public void setUri(String uri) {
	this.uri = uri;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public double getX() {
	return x;
    }

    public void setX(double x) {
	this.x = x;
    }

    public double getY() {
	return y;
    }

    public void setY(double y) {
	this.y = y;
    }
}
