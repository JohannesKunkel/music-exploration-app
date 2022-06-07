package info.interactivesystems.musicmap.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.interactivesystems.musicmap.dao.MusicDAO;
import info.interactivesystems.musicmap.entities.Artist;
import info.interactivesystems.musicmap.entities.Image;
import info.interactivesystems.musicmap.entities.Track;

@Named
@ApplicationScoped
public class ImageCache {
    private static final String TRACK_IMG_DIR = System.getProperty("jboss.home.dir") + "/img/track_images/";
    private static final String ARTIST_IMG_DIR = System.getProperty("jboss.home.dir") + "/img/artist_images/";
    private static final Logger logger = LoggerFactory.getLogger(ImageCache.class);

    @Inject
    private MusicDAO musicDao;

    private boolean ready;

    @PostConstruct
    private void init() {
	logger.debug("Caching images...");
	Collection<Artist> allArtists = musicDao.getAllArtists();
	int i = 0;
	for (Artist artist : allArtists) {
	    cacheArtistImages(artist);
	    for (Track track : artist.getTracks()) {
		cacheTrackImages(track);
	    }
	    i++;
	    if (i % 500 == 0) {
		logger.debug("Cached images for {} of {} artists.", i, allArtists.size());
	    }

	}
	logger.debug("Done caching images.");
	ready = true;
    }

    private void cacheArtistImages(Artist artist) {
	if (!imagesExist(ARTIST_IMG_DIR, SpotifyUtils.uriToId(artist.getUri())) && !artist.getImages().isEmpty()) {
	    saveImage(getLargestImage(artist.getImages()).getUrl(), ARTIST_IMG_DIR, SpotifyUtils.uriToId(artist.getUri()) + "_l");
	}
    }

    private void cacheTrackImages(Track track) {
	if (!imagesExist(TRACK_IMG_DIR, SpotifyUtils.uriToId(track.getUri())) && !track.getImages().isEmpty()) {
	    saveImage(getLargestImage(track.getImages()).getUrl(), TRACK_IMG_DIR, SpotifyUtils.uriToId(track.getUri()) + "_l");
	}
    }

    private boolean imagesExist(String dir, String id) {
	File file = new File(dir + id + "_l.jpg");
	return file.exists();
    }

    private void saveImage(String imageUrlString, String dir, String fileName) {
	try {
	    URL imageUrl = new URL(imageUrlString);
	    BufferedImage image = readImage(imageUrl.openStream());
	    if (image != null) {
		File file = new File(dir + fileName + ".jpg");
		ImageIO.write(image, "jpg", file);
	    }
	} catch (IOException e) {
	    logger.warn("Cannot save image. {}", imageUrlString);
	    logger.error(e.getMessage());
	}
    }

    private BufferedImage readImage(InputStream stream) throws IOException {
	try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {

	    // Find potential readers
	    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

	    // For each reader: try to read
	    while (readers != null && readers.hasNext()) {
		ImageReader reader = readers.next();
		try {
		    reader.setInput(input);
		    BufferedImage image = reader.read(0);
		    return image;
		} catch (IIOException e) {
		    // Try next reader, ignore.
		} catch (Exception e) {
		    // Unexpected exception. do not continue
		    throw e;
		} finally {
		    // Close reader resources
		    reader.dispose();
		}
	    }

	    // Couldn't resize with any of the readers
	    throw new IIOException("Unable to resize image");
	}
    }

    private Image getLargestImage(Collection<Image> images) {
	Image largestImage = images.iterator().next();

	for (Image image : images) {
	    if (image.getWidth() > largestImage.getWidth()) {
		largestImage = image;
	    }
	}
	return largestImage;
    }

    public boolean isReady() {
	return ready;
    }

}
