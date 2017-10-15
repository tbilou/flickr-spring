package com.tbilou.flickrspring.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.tbilou.flickrspring.service.flickr.FlickrApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrService {

    private static final int PHOTOS_PER_PAGE = 500;

    private final FlickrApiService flickrApiService;
    private final RabbitTemplate rabbitTemplate;
    private final CloseableHttpClient httpClient;

    @Value("${queue.flickr.photosets.photos}")
    private String queuePhotos;

    @Value("${queue.flickr.download}")
    private String queueDownload;

    @Value("${queue.flickr.context}")
    private String queueContext;


    /**
     * Gets the list of photosets from flickr
     * There is no pagination needed here because a single
     * request can return > 1000 sets
     */
    public void getPhotosetsList() {
        JsonArray photosets = flickrApiService.photosetsGetList();

        // For each ID request more information
        for (JsonElement p : photosets) {
            JsonObject item = p.getAsJsonObject();

            final String title = item.get("title").getAsJsonObject().get("_content").getAsString();
            final String photosetId = item.get("id").getAsString();
            int pages = (int) Math.ceil((double) item.get("photos").getAsInt() / (double) PHOTOS_PER_PAGE);

            // if a photoset has more than 500 photos make sure we send one request for each page
            sendMessagesToPhotosQueue(photosetId, pages, title);
        }
    }

    /**
     *   For each photoset, query flickr to get all the photos for the give page
     *   We create a json message for each photo in the set and send it to rabbit
     */
    public void getPhotosInPhotoset(JsonObject photoset) {
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
            msg.addProperty("datetaken", photo.getAsJsonObject().get("datetaken").getAsString());
            messages.add(msg);
        }

        // Send all the messages to the download queue
        log.info("Sending {} messages", messages.size());
        messages.stream()
                .forEach(m -> rabbitTemplate.convertAndSend(queueDownload, m.toString()));

        log.info("Sending {} messages to elasticsearch", messages.size());
        messages.stream()
                .forEach(m -> sendToElasticSearch(m));
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

    private void sendToElasticSearch(JsonObject m) {

        String url = MessageFormat.format("http://localhost:9200/flickr/photo/{0}", m.get("id").getAsString());
        HttpPut request = new HttpPut(url);
        StringEntity params = new StringEntity(new Gson().toJson(m), "UTF-8");
        params.setContentType("application/json");
        request.addHeader("content-type", "application/json");
        request.addHeader("Accept", "*/*");
        request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
        request.addHeader("Accept-Language", "en-US,en;q=0.8");
        request.setEntity(params);
        try (final CloseableHttpResponse resp = httpClient.execute(request)) {
            final int statusCode = resp.getStatusLine().getStatusCode();
            log.debug("Status {}", statusCode);
        } catch (IOException e) {
            log.error("Failed to send to Elastic Search");
        }
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

                JsonObject msg = new JsonObject();
                msg.addProperty("id", photo.getAsJsonObject().get("id").getAsString());
                msg.addProperty("title", StringUtils.isEmpty(title) ? msg.get("id").getAsString() : title);
                msg.addProperty("url", photo.getAsJsonObject().get("url_o").getAsString());
                msg.addProperty("photosetName", "NotInSet");
                messages.add(msg);
            }
            currentPage++;
        }
        while (currentPage <= photos.get("pages").getAsInt());

        // Send all the messages to the download queue
        log.info("Sending {} messages", messages.size());
        messages.stream()
                .forEach(m -> rabbitTemplate.convertAndSend(queueDownload, m.toString()));

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
