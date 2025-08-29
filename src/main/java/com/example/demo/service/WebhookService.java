package com.example.demo.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void executeFlow() {
        // Step 1: Call generateWebhook
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Freeda Thennela");   
        requestBody.put("regNo", "22BCE7545");   
        requestBody.put("email", "freedathennela@gmail.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        String webhookUrl = (String) response.getBody().get("webhook");
        String accessToken = (String) response.getBody().get("accessToken");

        // Step 2: SQL query
        String finalQuery = """
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

        submitSolution(webhookUrl, accessToken, finalQuery);
    }

    private void submitSolution(String webhookUrl, String token, String finalQuery) {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("finalQuery", finalQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        System.out.println("✅ Response from server: " + response.getBody());
    }
}
