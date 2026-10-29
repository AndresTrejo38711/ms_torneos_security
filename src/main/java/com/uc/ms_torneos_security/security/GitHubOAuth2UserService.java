package com.uc.ms_torneos_security.security;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// GitHub solo incluye el correo en el perfil (/user) si el usuario lo hizo público.
// Por eso, después de cargar el perfil, se pide su lista de correos (/user/emails, scope user:email)
// y se deja en "email" el principal y verificado. (Google y Microsoft lo entregan en su token).
@Component
public class GitHubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    private final RestClient restClient = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User profile = delegate.loadUser(userRequest);
        ClientRegistration.ProviderDetails.UserInfoEndpoint userInfo = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint();

        Map<String, Object> attributes = new HashMap<>(profile.getAttributes());
        attributes.put("email", findVerifiedEmail(userInfo.getUri(), userRequest));

        return new DefaultOAuth2User(
                profile.getAuthorities(),
                attributes,
                userInfo.getUserNameAttributeName()
        );
    }

    private String findVerifiedEmail(String userInfoUri, OAuth2UserRequest userRequest) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(userInfoUri + "/emails")
                    .headers(headers -> headers.setBearerAuth(userRequest.getAccessToken().getTokenValue()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return emails == null ? null : emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary"))
                            && Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException exception) {
            // Así el error llega al manejador de fallos del login y no como un error 500.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response"),
                    "No se pudieron leer los correos de GitHub",
                    exception
            );
        }
    }
}
