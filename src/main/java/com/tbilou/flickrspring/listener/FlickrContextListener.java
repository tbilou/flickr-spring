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
@ConditionalOnProperty("listeners.context.enabled")
public class FlickrContextListener {

    private final FlickrService flickrService;

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
}
