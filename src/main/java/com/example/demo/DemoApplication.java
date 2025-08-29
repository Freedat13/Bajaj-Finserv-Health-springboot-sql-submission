package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Call generateWebhook API
        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Freeda Thennela");
        requestBody.put("regNo", "22BCE7545");
        requestBody.put("email", "freedathennela@gmail.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<WebhookResponse> response =
                restTemplate.exchange(generateWebhookUrl, HttpMethod.POST, requestEntity, WebhookResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String webhookUrl = response.getBody().getWebhook();
            String accessToken = response.getBody().getAccessToken();

            System.out.println("Webhook: " + webhookUrl);
            System.out.println("Token: " + accessToken);

            // 2. Prepare SQL query
            String sqlQuery = """
                    SELECT 
                        p.AMOUNT AS SALARY,
                        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
                        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
                        d.DEPARTMENT_NAME
                    FROM PAYMENTS p
                    JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
                    JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
                    WHERE DAY(p.PAYMENT_TIME) <> 1
                    ORDER BY p.AMOUNT DESC
                    LIMIT 1;
                    """;

            // 3. Submit SQL query to webhook
            Map<String, String> finalBody = new HashMap<>();
            finalBody.put("finalQuery", sqlQuery);

            try {
                // First attempt with Bearer
                HttpHeaders postHeaders = new HttpHeaders();
                postHeaders.setContentType(MediaType.APPLICATION_JSON);
                postHeaders.setBearerAuth(accessToken);

                HttpEntity<Map<String, String>> postEntity = new HttpEntity<>(finalBody, postHeaders);

                ResponseEntity<String> submitResponse =
                        restTemplate.postForEntity(webhookUrl, postEntity, String.class);

                System.out.println("✅ Submission with Bearer worked: " + submitResponse.getBody());

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                System.out.println("❌ Submission with Bearer failed (" + e.getStatusCode() + "). Retrying without Bearer...");

                // Retry without Bearer
                HttpHeaders altHeaders = new HttpHeaders();
                altHeaders.setContentType(MediaType.APPLICATION_JSON);
                altHeaders.set("Authorization", accessToken); // raw token

                HttpEntity<Map<String, String>> altEntity = new HttpEntity<>(finalBody, altHeaders);
                ResponseEntity<String> altResponse =
                        restTemplate.postForEntity(webhookUrl, altEntity, String.class);

                System.out.println("✅ Retry Response: " + altResponse.getBody());
            }

        } else {
            System.out.println("Failed to generate webhook!");
        }
    }

    // Helper class to parse JSON response
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WebhookResponse {
        private String webhook;
        private String accessToken;

        public String getWebhook() { return webhook; }
        public void setWebhook(String webhook) { this.webhook = webhook; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }
}
