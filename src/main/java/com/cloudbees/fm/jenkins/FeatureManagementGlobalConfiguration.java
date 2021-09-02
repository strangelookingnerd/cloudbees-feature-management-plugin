/*
 * Copyright 2021 CloudBees, Inc.
 * All rights reserved.
 */

package com.cloudbees.fm.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.security.ACL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class FeatureManagementGlobalConfiguration extends GlobalConfiguration {

    /**
     * The access token to be used when accessing the public API.
     * <p/>
     * TODO: Allow local override - the access token will only allow access to one team's data.
     *       A Jenkins instance might be shared between multiple teams => requiring different access tokens
     *       on different jobs.
     */
    private String accessTokenCredentialId;

    public FeatureManagementGlobalConfiguration() {
        load();
    }

    public String getAccessTokenCredentialId() {
        return accessTokenCredentialId;
    }

    public void setAccessTokenCredentialId(String accessTokenCredentialId) {
        this.accessTokenCredentialId = accessTokenCredentialId;
        save();
    }
    
    public Optional<StringCredentials> getAccessTokenCredential() {
        if (StringUtils.isBlank(this.accessTokenCredentialId)) {
            return Optional.empty();
        }
        
        List<StringCredentials> creds = CredentialsMatchers.filter(
                CredentialsProvider.lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.emptyList()),
                CredentialsMatchers.withId(this.accessTokenCredentialId.trim())
        );

        return creds.stream().findFirst();
    }
    
    /**
     * Gets the {@link FeatureManagementGlobalConfiguration} singleton.
     *
     * @return the {@link FeatureManagementGlobalConfiguration} singleton.
     */
    public static FeatureManagementGlobalConfiguration get() {
        return ExtensionList.lookup(GlobalConfiguration.class).get(FeatureManagementGlobalConfiguration.class);
    }
}
