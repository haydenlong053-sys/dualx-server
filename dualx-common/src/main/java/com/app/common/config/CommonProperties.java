package com.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "com.app.common", ignoreUnknownFields = true)
public class CommonProperties {

    private Async async = new Async();

    private Upload upload = new Upload();

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public static class Async {
        private int corePoolSize = 10;
        private int maxPoolSize = 40;
        private int queueCapacity = 20;
        private int keepAliveSeconds = 30;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    public static class Upload {
        private String filePath;
        private String fileUrlPrefix;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileUrlPrefix() {
            return fileUrlPrefix;
        }

        public void setFileUrlPrefix(String fileUrlPrefix) {
            this.fileUrlPrefix = fileUrlPrefix;
        }
    }

}
