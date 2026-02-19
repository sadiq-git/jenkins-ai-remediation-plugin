package org.jenkinsci.plugins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.Extension;
import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

@Extension
public class GitHubRemediationService {
    private static final Logger LOGGER = Logger.getLogger(GitHubRemediationService.class.getName());

    public String createRepoAndPushFix(Run<?, ?> run, String fileName, String newContent) throws Exception {
        String githubToken = getGitHubToken();
        if (githubToken == null) throw new IllegalStateException("GitHub Credentials not configured.");

        GitHub github = new GitHubBuilder().withOAuthToken(githubToken).build();
        String currentUser = github.getMyself().getLogin();
        
        String jobName = run.getParent().getName().replaceAll("[^a-zA-Z0-9-]", "-").toLowerCase();
        String repoName = "jenkins-job-" + jobName;
        String fullRepoName = currentUser + "/" + repoName;
        
        GHRepository repo = null;
        
        // 1. Try to find existing repo
        try {
            repo = github.getRepository(fullRepoName);
            LOGGER.info("Found existing repository: " + fullRepoName);
        } catch (IOException e) {
            // Not found, proceeding to create
        }

        // 2. Create if missing
        if (repo == null) {
            LOGGER.info("Creating new repository: " + repoName);
            repo = github.createRepository(repoName)
                    .description("AI Remediation Artifacts for Jenkins Job: " + run.getParent().getName())
                    .private_(true)
                    .autoInit(true)
                    .create();
            Thread.sleep(3000); // Give GitHub a moment to propagate
            
            // Fetch the object again to ensure we have a stable reference
            repo = github.getRepository(fullRepoName);
        }
        
        if (repo == null) {
            throw new IllegalStateException("Failed to resolve repository reference for " + fullRepoName);
        }

        // 3. Create or Update File
        String commitMessage = "AI Fix from Jenkins Build #" + run.getId();
        String fileUrl;
        
        try {
            GHContent content = repo.getFileContent(fileName);
            content.update(newContent, commitMessage);
            fileUrl = content.getHtmlUrl();
        } catch (IOException e) {
            // File doesn't exist, create it
            GHContentUpdateResponse response = repo.createContent()
                    .path(fileName)
                    .content(newContent)
                    .message(commitMessage)
                    .commit();
            fileUrl = response.getContent().getHtmlUrl();
        }

        LOGGER.info("Successfully pushed fix to: " + fileUrl);
        return fileUrl;
    }

    private String getGitHubToken() {
        AiRemediationGlobalConfiguration config = AiRemediationGlobalConfiguration.get();
        if (config == null || config.getGithubCredentialsId() == null) return null;
        
        StringCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StringCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList()
                ),
                CredentialsMatchers.withId(config.getGithubCredentialsId())
        );
        return (c != null) ? c.getSecret().getPlainText() : null;
    }
}