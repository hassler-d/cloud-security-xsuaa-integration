package com.sap.cloud.security.cas.client;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

/**
 * TODO: extract as library
 */
public class DefaultADCService implements ADCService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultADCService.class);

    private final CloseableHttpClient httpClient;

    public DefaultADCService() {
        this.httpClient = HttpClients.createDefault();
    }

    public DefaultADCService(CloseableHttpClient client) {
        this.httpClient = client;
    }

    @Override
    public ADCServiceResponse isUserAuthorized(URI adcUri, ADCServiceRequest request) {
        HttpPost httpPost;
        ADCServiceResponse response = new ADCServiceResponse();

        try {
            URIBuilder builder = new URIBuilder(adcUri);
            httpPost = new HttpPost(builder.build());
            httpPost.setEntity(new StringEntity(request.asInputJson()));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String responseContent = null;
                if (statusCode == HttpStatus.SC_OK) {
                    LOGGER.debug("Successfully requested ADC service (status code: {})", statusCode);
                    // TODO use HttpClientUtil?
                    responseContent = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()))
                            .lines().collect(Collectors.joining(System.lineSeparator()));
                    response.setResult(responseContent.toString());
                } else {
                    LOGGER.error("Error requesting ADC service (status code: {}): {}", statusCode, responseContent);
                }
            } catch (IOException e) {
                LOGGER.error("Unexpected error retrieving JWT token: " + e.getMessage(), e);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Error building url: {}", e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Error set entity (body) with json {}: {}", request.asInputJson(), e.getMessage(), e);
        }
        return response;
    }

    public boolean ping(URI adcBaseUri) {
        URI adcHealthEndpoint = expandPath(adcBaseUri, "/health");
        HttpGet httpGet = new HttpGet(adcHealthEndpoint);
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseContent = null;
            if (statusCode == HttpStatus.SC_OK) {
                LOGGER.info("Successfully requested ADC service (status code: {})", statusCode);
                return true;
            } else {
                LOGGER.error("Error requesting ADC service (status code: {}): {}", statusCode, responseContent);
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected error retrieving JWT token: " + e.getMessage(), e);
        }
        return false;
    }

    // TODO replace with UriUtil.expandPath
    public static URI expandPath(URI baseUri, String pathToAppend) {
        try {
            String newPath = baseUri.getPath() + pathToAppend;
            return new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(),
                    replaceDoubleSlashes(newPath), baseUri.getQuery(), baseUri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String replaceDoubleSlashes(String newPath) {
        return newPath.replaceAll("//", "/");
    }

}
