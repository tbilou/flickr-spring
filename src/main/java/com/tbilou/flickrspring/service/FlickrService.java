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
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrService {

    private static final int PHOTOS_PER_PAGE = 500;

    private final DownloadService downloadService;
    private final FlickrApiService flickrApiService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.flickr.photosets.photos}")
    private String queuePhotos;

    @Value("${queue.flickr.download}")
    private String queueDownload;

    @Value("${queue.flickr.context}")
    private String queueContext;

    public void getPhotosetsList() {
        // Get the Json from Flickr
        JsonArray photosets = flickrApiService.photosetsGetList();

        // For each ID request more information
        for (JsonElement p : photosets) {
            JsonObject item = p.getAsJsonObject();

            String title = item.get("title").getAsJsonObject().get("_content").getAsString();
            int pages = (int) Math.ceil((double) item.get("photos").getAsInt() / (double) PHOTOS_PER_PAGE);

            sendMessagesToPhotosQueue(item.get("id").getAsString(), pages, title);
        }
    }

    public void getPagesForPhotoset(String id) {
        JsonObject photoset = flickrApiService.photosetPages(id);
        final int pages = photoset.get("pages").getAsInt();
        String setName = photoset.get("title").getAsString();

        sendMessagesToPhotosQueue(id, pages, setName);
    }

    private void sendMessagesToPhotosQueue(String id, int pages, String setName) {
        for (int p = 1; p <= pages; p++) {
            JsonObject msg = new JsonObject();
            msg.addProperty("id", id);
            msg.addProperty("page", p);
            msg.addProperty("setName", setName);
            // send message
            rabbitTemplate.convertAndSend(queuePhotos, msg.toString());
        }
    }

    public void getPhotosInPhotoset(JsonObject photoset) {
        // call flickr.photosetsGetList.getPhotos
        String info = flickrApiService.getPhotos(photoset.get("id").getAsString(), photoset.get("page").getAsString());
        ReadContext ctx = JsonPath.parse(info);

        JsonArray photos = ctx.read("$.photoset.photo");
        JsonElement name = ctx.read("$.photoset.title");

        List<JsonObject> messages = new ArrayList<>();

        // Create a json object to send to the download queue
        for (JsonElement photo : photos) {

            final String title = photo.getAsJsonObject().get("title").getAsString();

            JsonObject msg = new JsonObject();
            msg.addProperty("id", photo.getAsJsonObject().get("id").getAsString());
            msg.addProperty("title", StringUtils.isEmpty(title) ? msg.get("id").getAsString() : title);
            msg.addProperty("url", photo.getAsJsonObject().get("url_o").getAsString());
            msg.addProperty("photosetName", name.getAsString());
            messages.add(msg);
        }

        // Send all the messages to the download queue
        log.info("Sending {} messages", messages.size());
        messages.stream()
                .forEach(m -> rabbitTemplate.convertAndSend(queueDownload, m.toString()));
    }

    public void getPhotosNotInSet() {
        JsonObject photos;
        List<JsonObject> messages = new ArrayList<>();

        int currentPage = 1;
        do {
            photos = flickrApiService.photosNotInSet(String.valueOf(currentPage));
            JsonArray list = photos.get("photo").getAsJsonArray();
            for (JsonElement photo : list) {
                final String title = photo.getAsJsonObject().get("title").getAsString();
                final String media = photo.getAsJsonObject().get("media").getAsString();

                if (!media.equalsIgnoreCase("photo")) {
                    continue;
                }

                JsonObject msg = new JsonObject();
                msg.addProperty("id", photo.getAsJsonObject().get("id").getAsString());
                msg.addProperty("title", StringUtils.isEmpty(title) ? msg.get("id").getAsString() : title);
                msg.addProperty("url", photo.getAsJsonObject().get("url_o").getAsString());
                msg.addProperty("photosetName", "NotInSet");
                messages.add(msg);
            }
        }
        while (currentPage <= photos.get("pages").getAsInt());

    }

    public void downloadPhoto(JsonObject photo) throws RuntimeException {
        log.info("Downloading: {} {}", photo.get("url").getAsString(), photo.get("id"));
        downloadService.downloadAndSave(photo.get("url").getAsString(), photo.get("title").getAsString(), photo.get("photosetName").getAsString(), photo.get("id").getAsString());
    }

    public void recentlyUpdated() {
        final String lastUpdated = loadProperties().getProperty("lastUpdated");

        List<JsonObject> messages = new ArrayList<>();
        JsonObject photos;
        int currentPage = 1;
        do {
            photos = flickrApiService.photosRecentlyUpdated(lastUpdated, "1");
            JsonArray list = photos.get("photo").getAsJsonArray();
            for (JsonElement photo : list) {

                final String title = photo.getAsJsonObject().get("title").getAsString();
                final String media = photo.getAsJsonObject().get("media").getAsString();

                if (!media.equalsIgnoreCase("photo")) {
                    continue;
                }

                JsonObject msg = new JsonObject();
                msg.addProperty("id", photo.getAsJsonObject().get("id").getAsString());
                msg.addProperty("title", StringUtils.isEmpty(title) ? msg.get("id").getAsString() : title);
                msg.addProperty("url", photo.getAsJsonObject().get("url_o").getAsString());
                messages.add(msg);
            }
            currentPage++;
        }
        while (currentPage <= photos.get("pages").getAsInt());

        saveProperties();

        log.debug("Found {} new photos since last update", messages.size());
        messages.stream()
                .forEach(m -> rabbitTemplate.convertAndSend(queueContext, m.toString()));
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("lastUpdated.properties")) {
            // Load the lastUpdated value
            properties.load(input);
        } catch (java.io.IOException e) {
            log.error("Unable to load lastUpdated timestamp");
        }
        return properties;
    }

    private Properties saveProperties() {
        Properties properties = new Properties();
        try (OutputStream output = new FileOutputStream("lastUpdated.properties")) {
            properties.setProperty("lastUpdated", String.valueOf(Instant.now().getEpochSecond()));
            properties.store(output, null);
        } catch (java.io.IOException e) {
            log.error("Unable to save lastUpdated timestamp");
        }
        return properties;
    }


    public void getPhotosetNameForPhoto(JsonObject photo) {

        log.info("Getting photoset for id:{}", photo.get("id").getAsString());
        String photosetName = flickrApiService.getAllContexts(photo.get("id").getAsString());
        photo.addProperty("photosetName", photosetName);
        log.debug("Message:{}", photo);
        rabbitTemplate.convertAndSend(queueDownload, photo.toString());
    }


}
