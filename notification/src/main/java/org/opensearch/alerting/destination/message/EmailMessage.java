/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.destination.message;

import org.opensearch.common.Strings;
import org.opensearch.common.settings.SecureString;

import java.net.URI;
import java.util.List;

/**
 * This class holds the content of an Email message
 */
public class EmailMessage extends BaseMessage {

    private String message;
    private String host;
    private int port;
    private String method;
    private String from;
    private List<String> recipients;
    private String subject;
    private final SecureString username;
    private final SecureString password;

    private EmailMessage(final DestinationType destinationType,
                         final String destinationName,
                         final String host,
                         final Integer port,
                         final String method,
                         final String from,
                         final List<String> recipients,
                         final String subject,
                         final SecureString username,
                         final SecureString password,
                         final String message) {

        super(destinationType, destinationName, message);

        if (DestinationType.EMAIL != destinationType) {
            throw new IllegalArgumentException("Channel Type does not match Email");
        }

        if (Strings.isNullOrEmpty(message)) {
            throw new IllegalArgumentException("Message content is missing");
        }

        if(Strings.isNullOrEmpty(host)) {
            throw new IllegalArgumentException("Host name should be provided");
        }

        if(Strings.isNullOrEmpty(from)) {
            throw new IllegalArgumentException("From address should be provided");
        }

        if(recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("List of recipients should be provided");
        }

        this.message = message;
        this.host = host;
        this.port = port == null ? 25 : port;
        this.method = method == null ? "none" : method;
        this.from = from;
        this.recipients = recipients;
        this.subject = subject == "" ? destinationName : subject;
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "DestinationType: " + destinationType + ", DestinationName: " +  destinationName +
                ", Host: " + host + ", Port: " + port + ", Message: " + message;
    }

    public static class Builder {
        private String message;
        private DestinationType destinationType;
        private String destinationName;
        private String host;
        private Integer port;
        private String method;
        private String from;
        private List<String> recipients;
        private String subject;
        private SecureString username;
        private SecureString password;

        public Builder(String destinationName) {
            this.destinationName = destinationName;
            this.destinationType = DestinationType.EMAIL;
        }

        public EmailMessage.Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public EmailMessage.Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public EmailMessage.Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public EmailMessage.Builder withFrom(String from) {
            this.from = from;
            return this;
        }

        public EmailMessage.Builder withRecipients(List<String> recipients) {
            this.recipients = recipients;
            return this;
        }

        public EmailMessage.Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public EmailMessage.Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public EmailMessage.Builder withUserName(SecureString username) {
            this.username = username;
            return this;
        }

        public EmailMessage.Builder withPassword(SecureString password) {
            this.password = password;
            return this;
        }

        public EmailMessage build() {
            return new EmailMessage(
                    this.destinationType, this.destinationName,
                    this.host, this.port, this.method,
                    this.from, this.recipients, this.subject,
                    this.username, this.password, this.message);
        }
    }

    public String getHost() { return host; }

    public int getPort() {
        return port;
    }

    public String getMethod() {
        return method;
    }
    
    public String getFrom() {
        return from;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getSubject() {
        return subject;
    }

    public SecureString getUsername() {
        return username;
    }

    public SecureString getPassword() {
        return password;
    }

    public URI getUri() {
        return buildUri(null, null, host, port, null, null);
    }
}