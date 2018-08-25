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
@ConditionalOnProperty("listeners.photosets.add.enabled")
public class FlickrPhotosetAddListener {

    private final FlickrService flickrService;

    /**
     * Create a message for each image found in this set page
     *
     * @param json {photoId:"123654", photosetId:"77612536123234234111"}
     */
    @RabbitListener(queues = "${queue.flickr.photosets.add}")
    public void addPhotoToPhotoset(String json) {
        log.info("Adding photo to photoset {}", json);

        // Convert json into an Object
        JsonObject msg = new JsonParser().parse(json).getAsJsonObject();
        flickrService.addPhotoToPhotoset(msg);
    }


}
