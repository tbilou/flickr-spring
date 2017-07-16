package com.tbilou.flickrspring.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tbilou.flickrspring.service.FlickrApiService;
import com.tbilou.flickrspring.service.FlickrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FlickrController {

    private static final int PHOTOS_PER_PAGE = 500;

    private final FlickrApiService flickrApiService;
    private final FlickrService flickrService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.flickr.photosets}")
    private String queue;

    @RequestMapping(value = "/start",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity photosets() {

        // kickstart the backup process

        // Get the Json from Flickr
        JsonArray photosets = flickrApiService.photosets();

        // Create messages for each photoset
        // {id:1, page:1, name:blah}
        List<List<JsonObject>> messages = new ArrayList<>();
        for (JsonElement p : photosets) {
            messages.add(getMessages(p.getAsJsonObject()));
        }

        List<JsonObject> flat = messages.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Send message to queue
        flat.stream()
                .forEach(msg -> rabbitTemplate.convertAndSend(queue, msg.toString()));

        log.info("Sent {} messages to {}", flat.size(), queue);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private List<JsonObject> getMessages(JsonObject photosetsList) {

        JsonObject title = photosetsList.get("title").getAsJsonObject();
        String photoset = title.get("_content").getAsString();

        List<JsonObject> result = new ArrayList<>();

        // Calculate the number of pages (total photos / photos_per_page)
        Integer photos = Integer.parseInt(photosetsList.get("photos").getAsString());
        int pages = (int) Math.ceil((double) photos / (double) PHOTOS_PER_PAGE);

        // Create a json message for a photoset
        for (int p = 1; p <= pages; p++) {
            JsonObject msg = new JsonObject();
            msg.addProperty("id", photosetsList.get("id").getAsString());
            msg.addProperty("page", p);
            msg.addProperty("setName", photoset);
            result.add(msg);
        }
        return result;
    }

    @RequestMapping(value = "/photoset/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getPhotoset(@PathVariable String id) {
        JsonObject photoset = flickrApiService.photosetPages(id);
        int pages = photoset.get("pages").getAsInt();
        String setName = photoset.get("title").getAsString();

        for (int p = 1; p <= pages; p++) {
            JsonObject msg = new JsonObject();
            msg.addProperty("id", id);
            msg.addProperty("page", p);
            msg.addProperty("setName", setName);
            // send message
            rabbitTemplate.convertAndSend(queue, msg.toString());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/photoset",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity photoset(@RequestBody String json) {

        // Convert json into an Object
        JsonObject photoset = new JsonParser().parse(json).getAsJsonObject();
        flickrService.processPhotoset(photoset);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
