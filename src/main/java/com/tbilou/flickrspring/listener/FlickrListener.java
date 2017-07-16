package com.tbilou.flickrspring.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbilou.flickrspring.service.DownloadService;
import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrListener {

    private final FlickrService flickrService;
    private final DownloadService downloadService;

    @Value("${queue.flickr.photos}")
    private String queueDownload;

//    @RabbitListener(queues = "${queue.flickr.photosets}")
//    public void getPhotosetDetails(String json) {
//        log.info("Getting information about Photoset {}", json);
//
//        // Convert json into an Object
//        JsonObject photoset = new JsonParser().parse(json).getAsJsonObject();
//        flickrService.processPhotoset(photoset);
//    }

    @RabbitListener(queues = "${queue.flickr.photos}")
    public void downloadPhoto(String json) {
        log.info("Getting information about Photo to download {}", json);

        // Convert json into an Object
        JsonObject photo = new JsonParser().parse(json).getAsJsonObject();

        log.info("Downloading: {}", photo.get("url").getAsString());
        downloadService.downloadAndSave(photo.get("url").getAsString(), photo.get("title").getAsString(), photo.get("photosetName").getAsString());


    }
}
