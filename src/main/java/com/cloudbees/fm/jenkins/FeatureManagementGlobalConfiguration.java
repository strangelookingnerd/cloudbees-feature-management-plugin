/*
 * Copyright 2021 CloudBees, Inc.
 * All rights reserved.
 */

package com.cloudbees.fm.jenkins;

import hudson.Extension;
import hudson.ExtensionList;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Extension
public class FeatureManagementGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(FeatureManagementGlobalConfiguration.class.getName());

    /**
     * The access token to be used when accessing the public API.
     * <p/>
     * TODO: Use a security credential.
     * TODO: Allow local override - the access token will only allow access to one team's data.
     *       A Jenkins instance might be shared between multiple teams => requiring different access tokens
     *       on different jobs.
     */
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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
