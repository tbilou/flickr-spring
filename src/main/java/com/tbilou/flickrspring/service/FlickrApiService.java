package com.tbilou.flickrspring.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class FlickrApiService {

    private final OAuthTemplate oAuthTemplate;

    public JsonArray photosets() {
        return JsonPath.read(photosetsGetList(), "$.photosets.photoset");
    }

    public JsonObject photoset(String id, String page) {
        return JsonPath.read(photosetsGetPhotos(id, page), "$");
    }

    public JsonObject photosetPages(String id) {
        return JsonPath.read(photosetsGetPhotos(id, ""), "$.photoset");
    }

    private String photosetsGetList() {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1", "flickr.photosets.getList"));
    }

    public String photosetsGetPhotos(String id, String page) {
        return oAuthTemplate.get(String.format("https://api.flickr.com/services/rest/?method=%s&format=json&nojsoncallback=1&extras=url_o&photoset_id=%s&page=%s&media=photos", "flickr.photosets.getPhotos", id, page));
    }


}
