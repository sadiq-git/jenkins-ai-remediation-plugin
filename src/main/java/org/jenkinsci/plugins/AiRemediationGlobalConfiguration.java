// File: src/main/java/org/jenkinsci/plugins/AiRemediationGlobalConfiguration.java
package org.jenkinsci.plugins;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;

@Extension
public class AiRemediationGlobalConfiguration extends GlobalConfiguration {

    private String groqCredentialsId;
    private String githubCredentialsId;
    private String modelName = "llama-3.3-70b-versatile";

    public AiRemediationGlobalConfiguration() {
        load();
    }

    public static AiRemediationGlobalConfiguration get() {
        return GlobalConfiguration.all().get(AiRemediationGlobalConfiguration.class);
    }

    public String getGroqCredentialsId() { return groqCredentialsId; }

    @DataBoundSetter
    public void setGroqCredentialsId(String groqCredentialsId) {
        this.groqCredentialsId = groqCredentialsId;
        save();
    }

    public String getGithubCredentialsId() { return githubCredentialsId; }

    @DataBoundSetter
    public void setGithubCredentialsId(String githubCredentialsId) {
        this.githubCredentialsId = githubCredentialsId;
        save();
    }

    public String getModelName() { return modelName; }

    @DataBoundSetter
    public void setModelName(String modelName) {
        this.modelName = modelName;
        save();
    }

    public ListBoxModel doFillGroqCredentialsIdItems() {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return new StandardListBoxModel();
        }
        return new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        Jenkins.get(),
                        StringCredentials.class,
                        Collections.<DomainRequirement>emptyList(),
                        credentials -> true
                );
    }

    public ListBoxModel doFillGithubCredentialsIdItems() {
        return doFillGroqCredentialsIdItems();
    }
}