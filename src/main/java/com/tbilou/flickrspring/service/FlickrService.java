package com.tbilou.flickrspring.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrService {

    private final FlickrApiService flickrApiService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.flickr.photos}")
    private String queueDownload;

    public void processPhotoset(JsonObject photoset) {
        // call flickr.photosets.getPhotos
        String info = flickrApiService.photosetsGetPhotos(photoset.get("id").getAsString(), photoset.get("page").getAsString());
        ReadContext ctx = JsonPath.parse(info);

        JsonArray photos = ctx.read("$.photoset.photo");
        JsonElement name = ctx.read("$.photoset.title");

        List<JsonObject> messages = new ArrayList<>();

        // Create a json object to send to the download queue
        for (JsonElement photo : photos) {
            JsonObject msg = new JsonObject();
            msg.addProperty("id", photo.getAsJsonObject().get("id").getAsString());
            msg.addProperty("title", photo.getAsJsonObject().get("title").getAsString());
            msg.addProperty("url", photo.getAsJsonObject().get("url_o").getAsString());
            msg.addProperty("photosetName", name.getAsString());
            messages.add(msg);
        }

        // Send all the messages to the download queue
        log.info("Sending {} messages", messages.size());
        messages.stream()
                .forEach(m -> rabbitTemplate.convertAndSend(queueDownload, m.toString()));
    }
}
