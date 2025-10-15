import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import jakarta.activation.DataHandler;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

public class GmailQuickstart {
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final Logger logger = Logger.getLogger(GmailQuickstart.class.getName());
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = List.of(
        GmailScopes.GMAIL_SEND,
        GmailScopes.GMAIL_MODIFY,
        GmailScopes.GMAIL_READONLY
    );
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // 1️⃣ Try environment variables (Render / production)
        String clientId = System.getenv("GMAIL_CLIENT_ID");
        String clientSecret = System.getenv("GMAIL_CLIENT_SECRET");
        String refreshToken = System.getenv("GMAIL_REFRESH_TOKEN");
        if (clientId != null && clientSecret != null && refreshToken != null) {
            System.out.println("INFO: Using environment variables for Gmail credentials.");

            // Modern approach using GoogleCredentials
           GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

            // Wrap in Credential object expected by Gmail API (if using older client library)
            return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                    .build()
                    .setRefreshToken(refreshToken);
        }

        // 2️⃣ Fallback for local development (unchanged)
        System.out.println("INFO: Using local OAuth flow for Gmail credentials.");
        InputStream in = GmailQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Local credentials file not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        System.out.println("GMAIL_CLIENT_ID=" + clientSecrets.getInstalled().getClientId());
        System.out.println("GMAIL_CLIENT_SECRET=" + clientSecrets.getInstalled().getClientSecret());


        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        // return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        System.out.println("GMAIL_REFRESH_TOKEN="+ credential.getRefreshToken());
        return credential;

    }




    public static void main(String... args) throws IOException, GeneralSecurityException {
        String apiEndpoint = "https://tabula-java.onrender.com/process-all";
        if (apiEndpoint == null || apiEndpoint.isEmpty()) {
            logger.severe("API endpoint is not set.");
        }

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String user = "me";
        ListMessagesResponse messagesResponse = service.users().messages()
                .list(user)
                .setQ("subject:\"Talabiya Processor\" is:unread")
                .execute();

        List<com.google.api.services.gmail.model.Message> messages = messagesResponse.getMessages();
        if (messages == null || messages.isEmpty()) {
            System.out.println("No unread emails found with subject 'Talabiya Processor'.");
            return;
        }

        System.out.println("Found " + messages.size() + " unread email(s) with subject 'Talabiya Processor'.");

        File ExpiriesFile = null;
        File DetailedFile = null;
        File BreifFile = null;
        File RequestedFile = null;
        com.google.api.services.gmail.model.Message messageTitle = null;

        for (com.google.api.services.gmail.model.Message msg : messages) {
            com.google.api.services.gmail.model.Message message =
                    service.users().messages().get(user, msg.getId()).setFormat("full").execute();
            messageTitle = message;

            List<MessagePart> parts = message.getPayload().getParts();
            if (parts != null) {
                for (MessagePart part : parts) {
                    String filename = part.getFilename();
                    if (filename != null && !filename.isEmpty()) {
                        System.out.println("Attachment found: " + filename);
                        try {
                            MessagePartBody attachmentBody = service.users().messages().attachments()
                                    .get("me", message.getId(), part.getBody().getAttachmentId())
                                    .execute();

                            byte[] fileData = Base64.getUrlDecoder().decode(attachmentBody.getData());
                            File tempFile = Files.createTempFile("attachment_", "_" + filename).toFile();

                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                fos.write(fileData);
                            }

                            logger.info("Saved attachment: " + tempFile.getAbsolutePath());
                            String name = filename.toLowerCase();

                            if (name.contains("expiries")) {
                                ExpiriesFile = tempFile;
                            } else if (name.contains("detailed")) {
                                DetailedFile = tempFile;
                            } else if (name.contains("breif")) {
                                BreifFile = tempFile;
                            } else if (name.contains("requested")) {
                                RequestedFile = tempFile;
                            }

                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error writing attachment to file", e);
                        }
                    }
                }
            }
        }

        logger.info("Waking the API up...");
         wakeUpRenderService(apiEndpoint);
        try { Thread.sleep(8000); } catch (InterruptedException ignored) {}

        logger.info("Sending files to external API...");
        String htmlContent = callCustomApiWithMultipleFiles(apiEndpoint, ExpiriesFile, DetailedFile, BreifFile, RequestedFile, logger);

        if (htmlContent != null) {
            logger.info("Got HTML content from API, preparing reply...");

            String to = messageTitle.getPayload().getHeaders().stream()
                    .filter(h -> h.getName().equalsIgnoreCase("From"))
                    .map(MessagePartHeader::getValue)
                    .findFirst()
                    .orElse(null);

            if (to == null) {
                logger.warning("No 'From' header found — cannot send reply.");
            } else {
                try {
                    Properties props = new Properties();
                    Session session = Session.getDefaultInstance(props, null);

                    MimeMessage mimeMessage = new MimeMessage(session);
                    mimeMessage.setFrom(new InternetAddress("me"));
                    mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
                    mimeMessage.setSubject("Processed Catalogue");

                    Multipart multipart = new MimeMultipart();

                    MimeBodyPart htmlPart = new MimeBodyPart();
                    htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
                    multipart.addBodyPart(htmlPart);

                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    String fileName = "processed_catalogue.html";
                    byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
                    jakarta.activation.DataSource source = new ByteArrayDataSource(htmlBytes, "text/html");
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(fileName);
                    multipart.addBodyPart(attachmentPart);

                    mimeMessage.setContent(multipart);

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    mimeMessage.writeTo(buffer);
                    String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

                    com.google.api.services.gmail.model.Message replyMessage = new com.google.api.services.gmail.model.Message();
                    replyMessage.setRaw(encodedEmail);

                    service.users().messages().send("me", replyMessage).execute();
                    logger.info("Reply sent successfully with attachment: " + fileName);

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to send reply email", e);
                }
            }
        }

        try {
            ModifyMessageRequest mods = new ModifyMessageRequest().setRemoveLabelIds(Collections.singletonList("UNREAD"));
            service.users().messages().modify("me", messageTitle.getId(), mods).execute();
            logger.info("Marked original message as read.");
        } catch (Exception e) {
            logger.warning("Failed to mark message as read: " + e.getMessage());
        }

        File[] tempFiles = {ExpiriesFile, DetailedFile, BreifFile};
        for (File tempFile : tempFiles) {
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    logger.info("Deleted temp file: " + tempFile.getAbsolutePath());
                } else {
                    logger.warning("Failed to delete temp file: " + tempFile.getAbsolutePath());
                }
            }
        }
    }

    private static String callCustomApiWithMultipleFiles(String apiEndpoint, File expiriesFile, File detailedFile,
                                                         File breifFile, File requestedFile, Logger logger) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadRequest = new HttpPost(apiEndpoint);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            if (expiriesFile != null)
                builder.addBinaryBody("expiries", expiriesFile, ContentType.APPLICATION_OCTET_STREAM, expiriesFile.getName());
            if (detailedFile != null)
                builder.addBinaryBody("detailed", detailedFile, ContentType.APPLICATION_OCTET_STREAM, detailedFile.getName());
            if (breifFile != null)
                builder.addBinaryBody("breif", breifFile, ContentType.APPLICATION_OCTET_STREAM, breifFile.getName());
            if (requestedFile != null)
                builder.addBinaryBody("requested", requestedFile, ContentType.APPLICATION_OCTET_STREAM, requestedFile.getName());

            HttpEntity multipart = builder.build();
            uploadRequest.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("Custom API responded with status: " + statusCode);

                if (statusCode == 200) {
                    String jsonResponse = new String(response.getEntity().getContent().readAllBytes());
                    logger.info("API Response: " + jsonResponse);

                    ObjectMapper mapper = new ObjectMapper();
                    java.util.Map<String, String> map = mapper.readValue(jsonResponse, java.util.Map.class);
                    return map.get("html");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send files to custom API", e);
        }
        return null;
    }
    private static void wakeUpRenderService(String apiEndpoint) {
        try {
            System.out.println("Waking up Render API...");
            URL url = new URL(apiEndpoint + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            System.out.println("Wake-up response: " + responseCode);
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("Warning: Failed to wake up Render service (maybe still starting up). Continuing...");
        }
    }

}
