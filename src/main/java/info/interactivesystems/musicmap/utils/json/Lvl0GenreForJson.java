package info.interactivesystems.musicmap.utils.json;

import java.util.Collection;
import java.util.stream.Collectors;

import info.interactivesystems.mapviews.mapitems.GenreMapItem;
import info.interactivesystems.musicmap.entities.Genre;

public class Lvl0GenreForJson {

    private int id;
    private String name;
    private int value;
    private double x;
    private double y;
    private Collection<ArtistForJson> representatives;
    private final int level = 0;
    private Collection<Lvl1GenreForJson> children;
    private String color;

    public Lvl0GenreForJson() {
	super();
    }

    public Lvl0GenreForJson(GenreMapItem genreMapItem, Collection<Lvl1GenreForJson> children) {
	super();
	Genre genre = (Genre) genreMapItem.getUserData();
	this.id = genre.getId();
	this.name = genre.getTitle();
	this.x = genreMapItem.getX();
	this.y = genreMapItem.getY();
	this.representatives = genreMapItem.getRepresentatives().stream().map(artistMapItem -> new ArtistForJson(artistMapItem)).collect(Collectors.toList());
	this.children = children;
	this.value = children.stream().mapToInt(c -> c.getValue()).sum();
	this.color = genreMapItem.getColor();
    }

    public Collection<Lvl1GenreForJson> getChildren() {
	return children;
    }

    public void setChildren(Collection<Lvl1GenreForJson> children) {
	this.children = children;
	this.value = children.stream().mapToInt(c -> c.getValue()).sum();
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
