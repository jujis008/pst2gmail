package com.atsid.outlook.pst;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service factory class for interacting with GMail services.
 */
@Component
@Log4j
public class GmailServiceFactory {
    /**
     * Application name.
     */
    @Value("${gmail.application.name}")
    private String APPLICATION_NAME;
    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".credentials/email-import-test");
    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;
    /**
     * Global instance of the scopes required by this quickstart.
     */
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_INSERT);
    private Map<String, String> emailCredentialMap = new HashMap<>();
    private Map<String, Gmail> emailServiceMap = new HashMap<>();

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    private Credential authorize(String jsonCredentialFile) throws IOException {
        // Load client secrets.
        try (InputStream in = new FileInputStream(jsonCredentialFile)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                            .setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
            Credential credential =
                    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
            log.debug("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
            return credential;
        } finally {
        }
    }

    /**
     * Adds a new set of credentials to the credential map.
     *
     * @param emailAddress       Email address of account we are attempting to process
     * @param jsonCredentialFile JSON credential file used to authenticate for specified email address
     */
    public void addCredentials(String emailAddress, String jsonCredentialFile) {
        emailCredentialMap.put(emailAddress, jsonCredentialFile);
    }

    /**
     * Build and return an authorized Gmail client service.
     *
     * @param emailAddress Email address of account being migrated
     * @return an authorized Gmail client service
     * @throws IOException
     */
    public Gmail getGmailService(String emailAddress) throws IOException {
        if (emailCredentialMap.containsKey(emailAddress) && !emailServiceMap.containsKey(emailAddress)) {
            Credential credential = authorize(emailCredentialMap.get(emailAddress));
            Gmail gmailService =
                    new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
                            .build();
            emailServiceMap.put(emailAddress, gmailService);

            return gmailService;
        } else if (emailCredentialMap.containsKey(emailAddress)) {
            return emailServiceMap.get(emailAddress);
        } else {
            throw new IllegalStateException("Do not have credentials to import using " + emailAddress);
        }
    }
}