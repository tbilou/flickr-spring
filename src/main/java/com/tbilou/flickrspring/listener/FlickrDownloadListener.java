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
@ConditionalOnProperty("listeners.download.enabled")
public class FlickrDownloadListener {

    private final FlickrService flickrService;

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
