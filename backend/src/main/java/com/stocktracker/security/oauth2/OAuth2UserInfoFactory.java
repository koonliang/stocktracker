package com.stocktracker.security.oauth2;

import com.stocktracker.entity.User.AuthProvider;
import com.stocktracker.exception.BadRequestException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new BadRequestException("Login with " + registrationId + " is not supported");
        }
    }
}
