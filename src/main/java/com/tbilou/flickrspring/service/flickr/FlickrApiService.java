package com.tbilou.flickrspring.service.flickr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrApiService {

    private final OAuthTemplate oAuthTemplate;
    private Configuration conf;

    @Cacheable("flickr")
    public JsonArray photosetsGetList() {
        return JsonPath.read(getList(), "$.photosets.photoset");
    }

    @Cacheable("flickr")
    public JsonObject photosetsGetPhotos(String id, String page) {
        return JsonPath.read(getPhotos(id, page), "$");
    }

    @Cacheable("flickr")
    public JsonObject photosetPages(String id) {
        return JsonPath.read(photosetsGetPhotos(id, ""), "$.photoset");
    }

    @Cacheable("flickr")
    public JsonObject photosNotInSet(String page) {
        String response = notInSet(page);
        return JsonPath.read(response, "$.photos");
    }

    public JsonObject photosRecentlyUpdated(String unixTimestamp, String page) {
        return JsonPath.read(recentlyUpdated(unixTimestamp, page), "$.photos");
    }

    private String getList() {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1", "flickr.photosets.getList"));
    }

    public String getPhotos(String id, String page) {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1&extras=url_o,date_taken&photoset_id=%s&page=%s&media=photos", "flickr.photosets.getPhotos", id, page));
    }

    private String recentlyUpdated(String unixTimestamp, String page) {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1&min_date=%s&extras=%s&per_page=500&page=%s&media=photos", "flickr.photos.recentlyUpdated", unixTimestamp, "url_o%2C+media", page));
    }

    private String notInSet(String page) {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1&extras=url_o&page=%s&media=photos", "flickr.photos.getNotInSet", page));
    }

    public String getAllContexts(String id) {
        String json = oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1&photo_id=%s", "flickr.photos.getAllContexts", id));
        JsonArray set = JsonPath.parse(json).read("$..set");
        if (set.size() > 0) {
            JsonElement s = set.get(0).getAsJsonArray().get(0);
            return s.getAsJsonObject().get("title").getAsString();
        } else {
            return "NoSet";
        }

    }


}
