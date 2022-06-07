package info.interactivesystems.musicmap.utils.json;

import java.util.List;

public class GenreHierarchyForJson {

    private final String name = "allGenres";
    private List<Lvl0GenreForJson> children;

    public GenreHierarchyForJson() {
	super();
    }

    public GenreHierarchyForJson(List<Lvl0GenreForJson> children) {
	super();
	this.children = children;
    }

    public List<Lvl0GenreForJson> getChildren() {
	return children;
    }

    public void setChildren(List<Lvl0GenreForJson> children) {
	this.children = children;
    }

    public String getName() {
	return name;
    }

}
