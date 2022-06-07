package info.interactivesystems.musicmap.utils.json;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import info.interactivesystems.mapviews.mapitems.ArtistMapItem;
import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.entities.Genre;

public class Lvl1GenreForJson {

    private int id;
    private String name;
    private int value;
    private double x;
    private double y;
    private Collection<ArtistForJson> representatives;
    private final int level = 1;
    private List<ArtistForJson> items;
    private String color;

    public Lvl1GenreForJson() {
	super();
    }

    public Lvl1GenreForJson(GenreMapItem genreMapItem, List<ArtistMapItem> items) {
	super();
	Genre genre = (Genre) genreMapItem.getUserData();
	this.id = genre.getId();
	this.name = genre.getTitle();
	this.x = genreMapItem.getX();
	this.y = genreMapItem.getY();
	this.representatives = convertArtistMapItemsToArtistsForJson(genreMapItem.getRepresentatives());
	this.items = convertArtistMapItemsToArtistsForJson(items);
	this.value = items.size();
	this.color = genreMapItem.getColor();
    }

    private List<ArtistForJson> convertArtistMapItemsToArtistsForJson(Collection<ArtistMapItem> artistMapItems) {
	return artistMapItems.stream().map(artistMapItem -> new ArtistForJson(artistMapItem)).collect(Collectors.toList());
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public int getValue() {
	return value;
    }

    public void setValue(int value) {
	this.value = value;
    }

    public Collection<ArtistForJson> getItems() {
	return items;
    }

    public void setItems(List<ArtistForJson> items) {
	this.value = items.size();
	this.items = items;
    }

    public int getLevel() {
	return level;
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

    public Collection<ArtistForJson> getRepresentatives() {
	return representatives;
    }

    public void setRepresentatives(Collection<ArtistForJson> representatives) {
	this.representatives = representatives;
    }

    public String getColor() {
	return color;
    }

    public void setColor(String color) {
	this.color = color;
    }
}
