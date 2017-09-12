package com.tbilou.flickrspring.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrListener {

    private final FlickrService flickrService;

    /**
     * Create a message for each image found in this set page
     *
     * @param json {id:"123654", name:"SomePhotoset", page:"1"}
     */
    @RabbitListener(queues = "${queue.flickr.photosets.photos}")
    public void getPhotosInPhotoset(String json) {
        log.info("Getting information about Photoset {}", json);

        // Convert json into an Object
        JsonObject photoset = new JsonParser().parse(json).getAsJsonObject();
        flickrService.getPhotosInPhotoset(photoset);
    }

    /**
     * Gets the photoset name for a given photo id
     *
     * @param json {"id":"35701101", "title":"IMG_7654", "url":"https://farm5.staticflickr.com/.../35701101_o.jpg"}
     */
    @RabbitListener(queues = "${queue.flickr.context}")
    public void getPhotosetForPhoto(String json) {
        log.info("Getting Photoset name for request {}", json);

        // Convert json into an Object
        JsonObject photoset = new JsonParser().parse(json).getAsJsonObject();
        flickrService.getPhotosetNameForPhoto(photoset);
    }

    /**
     * Downloads a photo to disk
     *
     * @param json {"id":"35701101", "title":"IMG_7654", "url":"https://farm5.staticflickr.com/.../35701101_o.jpg", "photosetName":"SomePhotoset"}
     */
    @RabbitListener(queues = "${queue.flickr.download}")
    public void downloadPhoto(String json) {
        log.info("Getting information about Photo to download {}", json);

        // Convert json into an Object
        JsonObject photo = new JsonParser().parse(json).getAsJsonObject();
        flickrService.downloadPhoto(photo);
    }
}
