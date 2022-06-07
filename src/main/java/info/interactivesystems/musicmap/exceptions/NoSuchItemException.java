package info.interactivesystems.musicmap.exceptions;

public class NoSuchItemException extends Exception {
    private static final long serialVersionUID = 2L;
    private static String exceptionMessage = "No such item found in background data: ";

    public NoSuchItemException(String uri) {
	super(exceptionMessage + uri);
    }
}
