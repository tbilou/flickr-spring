package com.tbilou.flickrspring.service;

import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTemplate {

    private final com.github.scribejava.core.oauth.OAuthService oAuthService;
    private final OAuth1AccessToken accessToken;

    public String get(String url) {
        final OAuthRequest request = new OAuthRequest(Verb.GET, url);

        oAuthService.signRequest(accessToken, request);

        try {
            final Response response = oAuthService.execute(request);
            log.debug(response.getBody());
            return response.getBody();
        } catch (InterruptedException | ExecutionException | IOException e) {
            log.error("Error getting api response - {}", e.getMessage());
        }
        return null;
    }
}
