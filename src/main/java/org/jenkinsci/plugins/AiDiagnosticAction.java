package org.jenkinsci.plugins;

import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import jakarta.servlet.ServletException;
import java.io.IOException;

public class AiDiagnosticAction implements Action {

    private final transient Run<?, ?> run;
    private final String aiExplanation;
    private final String repoFullName;
    private final String filePath;
    private final String originalContent;
    private final String newContent;

    public AiDiagnosticAction(Run<?, ?> run, String aiExplanation, String repoFullName, String filePath, String originalContent, String newContent) {
        this.run = run;
        this.aiExplanation = aiExplanation;
        this.repoFullName = repoFullName;
        this.filePath = filePath;
        this.originalContent = originalContent;
        this.newContent = newContent;
    }

    @Override
    public String getIconFileName() { return "symbol-sparkles plugin-ionicons-api"; }

    @Override
    public String getDisplayName() { return "Review AI Fix"; }

    @Override
    public String getUrlName() { return "ai-remediation"; }

    public Run<?, ?> getRun() { return run; }
    public String getOriginalContent() { return originalContent; }
    public String getNewContent() { return newContent; }
    public String getAiExplanation() { return aiExplanation; }
    public String getFilePath() { return filePath; }

    @RequirePOST
    public HttpResponse doSubmitFix(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        Jenkins.get().checkPermission(Item.BUILD);
        
        boolean approved = Boolean.parseBoolean(req.getParameter("approved"));
        
        if (approved) {
            try {
                GitHubRemediationService githubService = Jenkins.get().getExtensionList(GitHubRemediationService.class).get(0);
                
                // CALLING THE NEW SYNCHRONOUS METHOD
                String githubUrl = githubService.createRepoAndPushFix(run, filePath, newContent);
                
                // Redirect user directly to the new GitHub repo/file
                return HttpResponses.redirectTo(githubUrl);
                
            } catch (Exception e) {
                // Ideally, log this and show an error on the page
                e.printStackTrace();
                // For now, redirect back to avoid a blank screen
                return HttpResponses.redirectToDot();
            }
        }
        
        return HttpResponses.redirectToDot();
    }
}