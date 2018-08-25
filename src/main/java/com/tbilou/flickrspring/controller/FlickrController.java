package com.tbilou.flickrspring.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbilou.flickrspring.service.DownloadService;
import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/flickr/")
public class FlickrController {
    private final FlickrService flickrService;
    private final DownloadService downloadService;

    /**
     * Allows us to download an entire set.
     * If there are more than 500 photos inside it will create several messages
     * one for each page
     *
     * @param id The id of the set you want to download
     *           <p>
     *           Output {id:"64354684", name:"SomeSet", page:"1"}
     */
    @RequestMapping(value = "/photoset/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getAllPagesForSet(@PathVariable String id) {

        // Process single photoset
        flickrService.getPagesForPhotoset(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Allows you to download a specific page of a set.
     * If the set has more than 500 pages you need to specify which page you want
     *
     * @param json {id:"64354684", name:"SomeSet", page:"1"}
     */
    @RequestMapping(value = "/photoset",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getPageForSet(@RequestBody String json) {

        // Convert json into an Object
        JsonObject photoset = new JsonParser().parse(json).getAsJsonObject();
        flickrService.getPhotosInPhotoset(photoset);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Allows us to download a single photo from flickr
     *
     * @param json {"id":"35701101", "title":"IMG_7654", "url":"https://farm5.staticflickr.com/.../35701101_o.jpg", "photosetName":"SomeSet"}
     */
    @RequestMapping(value = "/photo",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity downloadPhoto(@RequestBody String json) {

        // Convert json into an Object
        JsonObject photo = new JsonParser().parse(json).getAsJsonObject();
        downloadService.downloadAndSave(
                photo.get("url").getAsString(),
                photo.get("title").getAsString(),
                photo.get("photosetName").getAsString(),
                photo.get("id").getAsString()
        );

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *  Create a photoset will all photos for a give year
     */
    @RequestMapping(value = "/photosets/{year}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addPhotosToPhotoset(@PathVariable Integer year) {

        flickrService.createPhotosetWithPhotosFromYear(year);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
