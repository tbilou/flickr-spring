package com.tbilou.flickrspring.controller;

import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/backup/")
public class FlickrBackupController {
    private final FlickrService flickrService;

    /**
     * Starts the entire backup process
     * <p>
     * Gets the list of sets from flickr and created one message for each set
     * taking into account if a set has more than 500 photos it creates a message for each page
     * <p>
     * REST -> PhotosetListener -> Download
     */
    @RequestMapping(value = "/full",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity photosets() {

        // kickstart the backup process
        flickrService.getPhotosetsList();
        flickrService.getPhotosNotInSet();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Gets the recentlyUpdated photos
     * <p>
     * REST -> ContextListener -> Download
     */
    @RequestMapping(value = "/recent",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity recentlyUpdated() {
        flickrService.recentlyUpdated();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Use it with curl > flickr.json to create a Json file with all the data from flickr for analysis
     *
     * @return
     */
    @RequestMapping(value = "/all",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity allPhotos() {
        final String result = flickrService.createFullListOfPhotos();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Uses the flickr.json from above to get it's photos from
     * @param year
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/byYear/{year}")
    public ResponseEntity getByYear(@PathVariable Integer year) throws IOException {
        log.info("Downloading photos for year {}", year);
        final String result = flickrService.downloadByYear(year);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
