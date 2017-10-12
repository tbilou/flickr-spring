package com.tbilou.flickrspring.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty("listeners.photosets.enabled")
public class FlickrPhotosetListener {

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


}
