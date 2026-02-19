package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject; 

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AiRemediationListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(AiRemediationListener.class.getName());

    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        if (run.getResult() == Result.FAILURE) {
            try {
                // 1. Get Logs
                List<String> logLines = run.getLog(150);
                String log = String.join("\n", logLines);
                String safeLog = LogSanitizer.sanitize(log);

                // 2. Get AI Response (JSON)
                AiDiagnosticService service = Jenkins.get().getExtensionList(AiDiagnosticService.class).get(0);
                String jsonResponse = service.getRemediation(safeLog);
                
                // 3. Parse JSON
                // Default values in case parsing fails
                String explanation = "AI could not generate a structured fix.";
                String fileName = "Jenkinsfile";
                String fixedCode = "// No fix provided by AI";

                try {
                    JSONObject json = JSONObject.fromObject(jsonResponse);
                    if (json.has("explanation")) explanation = json.getString("explanation");
                    if (json.has("fileName")) fileName = json.getString("fileName");
                    if (json.has("fixedCode")) fixedCode = json.getString("fixedCode");
                } catch (Exception parseEx) {
                    LOGGER.warning("Failed to parse JSON from AI: " + jsonResponse);
                    explanation = "Raw Output:\n" + jsonResponse;
                }

                // 4. Attach Action with REAL AI DATA
                run.addAction(new AiDiagnosticAction(
                    run, 
                    explanation, 
                    "jenkins-job-" + run.getParent().getName(), // Suggested repo name
                    fileName, 
                    "// Original code unavailable", 
                    fixedCode // THIS IS THE IMPORTANT PART
                ));
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to run AI remediation", e);
            }
        }
    }
}