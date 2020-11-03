package com.casm.acled.crawler.management;

import com.google.common.base.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "crawler.configservice")
public class ConfigService {

    // Assign default values here
    private Path workingDir = Paths.get("test");
    private Path scraperDir = Paths.get("/home/sw206/git/alced-scrapers");
    private String userAgent = "CASM Tech";
    private int maxDelay = 10;
    private String email;

    // NOTE: we have to define standard getters and setters to get @ConfigurationProperties to work.

    public Path getWorkingDir() {
        return workingDir;
    }
    public void setWorkingDir(Path workingDir) {
        this.workingDir = workingDir;
    }

    public Path getScraperDir() {
        return scraperDir;
    }
    public void setScraperDir(Path scraperDir) {
        this.scraperDir = scraperDir;
    }

    public String getUserAgent() {
        return userAgent;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getMaxDelay() {
        return maxDelay;
    }
    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public boolean isEmailConfigured(){
        return !Strings.isNullOrEmpty(email);
    }
}
