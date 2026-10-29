package com.uc.ms_torneos_security.client;

import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

// Cliente del microservicio de notificaciones (emails_google, Flask). Este servicio arma el
// mensaje (por ejemplo, con el código de 2FA que él mismo generó y valida); notificaciones solo lo entrega.
@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(@Value("${notifications.url}") String notificationsUrl) {
        // Sin límites de espera, un servicio caído o colgado dejaría el login esperando para siempre.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(notificationsUrl)
                // Envía el cuerpo completo con Content-Length y no por partes ("chunked"),
                // que algunos servidores sencillos no saben leer.
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .build();
    }

    public void sendEmail(String to, String subject, String message) {
        try {
            restClient.post()
                    .uri("/enviar-correo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "destinatario", to,
                            "asunto", subject,
                            "mensaje", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ApplicationException(
                    ErrorCase.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el correo: el servicio de notificaciones no respondió. Intenta de nuevo en unos minutos"
            );
        }
    }
}
