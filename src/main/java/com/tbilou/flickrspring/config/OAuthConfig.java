package com.tbilou.flickrspring.config;


import com.github.scribejava.apis.FlickrApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuthConfig {

    @Bean
    public OAuthService service(@Value("${api.key}") String apiKey, @Value("${api.secret}") String sharedSecret) {
        return new ServiceBuilder(apiKey)
                .apiSecret(sharedSecret)
                .build(FlickrApi.instance());
    }

    @Bean
    public OAuth1AccessToken accessToken(@Value("${oauth.token}") String token, @Value("${oauth.secret}") String tokenSecret) {
        return new OAuth1AccessToken(token, tokenSecret);
    }
}
