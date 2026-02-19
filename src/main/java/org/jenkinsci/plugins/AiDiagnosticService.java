package org.jenkinsci.plugins;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import hudson.Extension;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;

import java.util.Collections;
import java.time.Duration;

@Extension
public class AiDiagnosticService {

    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";

    public String getRemediation(String errorLog) {
        AiRemediationGlobalConfiguration config = AiRemediationGlobalConfiguration.get();
        String apiKey = getSecret(config.getGroqCredentialsId());
        
        if (apiKey == null) {
            return "{\"error\": \"Groq API Key not configured.\"}";
        }

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(GROQ_BASE_URL)
                .modelName(config.getModelName())
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();

        // STRICT JSON PROMPT
        String prompt = "You are a Jenkins Pipeline Expert. Analyze the failure log below.\n" +
                "You must respond with ONLY a raw JSON object. Do not include Markdown formatting (```json).\n" +
                "The JSON must have this structure:\n" +
                "{\n" +
                "  \"explanation\": \"A short summary of the error and the fix\",\n" +
                "  \"fileName\": \"The likely file causing the issue (e.g. Jenkinsfile)\",\n" +
                "  \"fixedCode\": \"The complete, corrected code block for that file\"\n" +
                "}\n\n" +
                "FAILURE LOG:\n" + errorLog;

        // Strip potential markdown code blocks if the model ignores instructions
        String response = model.generate(prompt);
        return cleanJson(response);
    }

    private String cleanJson(String response) {
        if (response.startsWith("```json")) {
            return response.replace("```json", "").replace("```", "").trim();
        }
        return response;
    }

    private String getSecret(String credentialsId) {
        if (credentialsId == null) return null;
        StringCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StringCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()
                ),
                CredentialsMatchers.withId(credentialsId)
        );
        return (c != null) ? c.getSecret().getPlainText() : null;
    }
}