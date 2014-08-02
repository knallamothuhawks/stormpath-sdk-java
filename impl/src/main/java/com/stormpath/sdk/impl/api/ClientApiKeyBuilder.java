/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.sdk.impl.api;

import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeyBuilder;
import com.stormpath.sdk.lang.Classes;
import com.stormpath.sdk.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

/** @since 1.0.RC */
public class ClientApiKeyBuilder implements ApiKeyBuilder {

    private static final Logger log = LoggerFactory.getLogger(ClientApiKeyBuilder.class);

    public static final String DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION =
        System.getProperty("user.home") + File.separatorChar + ".stormpath" + File.separatorChar + "apiKey.properties";

    //private ApiKey apiKey;
    private String      apiKeyId;
    private String      apiKeySecret;
    private String      apiKeyFileLocation;
    private InputStream apiKeyInputStream;
    private Reader      apiKeyReader;
    private Properties  apiKeyProperties;
    private String apiKeyIdPropertyName     = "apiKey.id";
    private String apiKeySecretPropertyName = "apiKey.secret";

    @Override
    public ApiKeyBuilder setId(String id) {
        this.apiKeyId = id;
        return this;
    }

    @Override
    public ApiKeyBuilder setSecret(String secret) {
        this.apiKeySecret = secret;
        return this;
    }

    @Override
    public ApiKeyBuilder setProperties(Properties properties) {
        this.apiKeyProperties = properties;
        return this;
    }

    @Override
    public ApiKeyBuilder setReader(Reader reader) {
        this.apiKeyReader = reader;
        return this;
    }

    @Override
    public ApiKeyBuilder setInputStream(InputStream is) {
        this.apiKeyInputStream = is;
        return this;
    }

    @Override
    public ApiKeyBuilder setFileLocation(String location) {
        this.apiKeyFileLocation = location;
        return this;
    }

    @Override
    public ApiKeyBuilder setIdPropertyName(String idPropertyName) {
        this.apiKeyIdPropertyName = idPropertyName;
        return this;
    }

    @Override
    public ApiKeyBuilder setSecretPropertyName(String secretPropertyName) {
        this.apiKeySecretPropertyName = secretPropertyName;
        return this;
    }

    protected Properties getDefaultApiKeyFileProperties() {
        Properties props = new Properties();

        try {
            Reader reader = createFileReader(DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION);
            props = toProperties(reader);
        } catch (IOException ignored) {
            log.debug("Unable to find or load default api key properties file [" +
                      DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION + "].  This can be safely ignored as this is a " +
                      "fallback location - other more specific locations will be checked.", ignored);
        }
        return props;
    }

    protected Properties getEnvironmentVariableProperties() {
        Properties props = new Properties();

        String value = System.getenv("STORMPATH_API_KEY_ID");
        if (Strings.hasText(value)) {
            props.put(this.apiKeyIdPropertyName, value);
        }

        value = System.getenv("STORMPATH_API_KEY_SECRET");
        if (Strings.hasText(value)) {
            props.put(this.apiKeySecretPropertyName, value);
        }

        return props;
    }

    protected Properties getSystemProperties() {
        Properties props = new Properties();

        String value = System.getProperty("stormpath.apiKey.id");
        if (Strings.hasText(value)) {
            props.put(this.apiKeyIdPropertyName, value);
        }

        value = System.getProperty("stormpath.apiKey.secret");
        if (Strings.hasText(value)) {
            props.put(this.apiKeySecretPropertyName, value);
        }

        return props;
    }

    @Override
    public ApiKey build() {

        //Issue 82 heuristics (see: https://github.com/stormpath/stormpath-sdk-java/labels/enhancement)

        //1. Try to load the default api key properties file.  All other config options have higher priority than this:
        Properties props = getDefaultApiKeyFileProperties();

        String id = getPropertyValue(props, this.apiKeyIdPropertyName);
        String secret = getPropertyValue(props, this.apiKeySecretPropertyName);

        //2. Try environment variables:
        props = getEnvironmentVariableProperties();
        id = getPropertyValue(props, this.apiKeyIdPropertyName, id);
        secret = getPropertyValue(props, this.apiKeySecretPropertyName, secret);

        //3. Try system properties:
        props = getSystemProperties();
        id = getPropertyValue(props, this.apiKeyIdPropertyName, id);
        secret = getPropertyValue(props, this.apiKeySecretPropertyName, secret);

        //4. Try any configured properties files:
        if (Strings.hasText(this.apiKeyFileLocation)) {
            try {
                Reader reader = createFileReader(this.apiKeyFileLocation);
                props = toProperties(reader);
            } catch (IOException e) {
                String msg = "Unable to read properties from specified apiKeyFileLocation [" + this.apiKeyFileLocation + "].";
                throw new IllegalArgumentException(msg, e);
            }

            id = getPropertyValue(props, this.apiKeyIdPropertyName, id);
            secret = getPropertyValue(props, this.apiKeySecretPropertyName, secret);
        }

        if (this.apiKeyInputStream != null) {
            try {
                Reader reader = toReader(this.apiKeyInputStream);
                props = toProperties(reader);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read properties from specified apiKeyInputStream.", e);
            }

            id = getPropertyValue(props, this.apiKeyIdPropertyName, id);
            secret = getPropertyValue(props, this.apiKeySecretPropertyName, secret);
        }

        if (this.apiKeyReader != null) {
            try {
                props = toProperties(this.apiKeyReader);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read properties from specified apiKeyReader.", e);
            }

            id = getPropertyValue(props, this.apiKeyIdPropertyName, id);
            secret = getPropertyValue(props, this.apiKeySecretPropertyName, secret);
        }

        if (this.apiKeyProperties != null && !this.apiKeyProperties.isEmpty()) {
            id = getPropertyValue(this.apiKeyProperties, this.apiKeyIdPropertyName, id);
            secret = getPropertyValue(this.apiKeyProperties, this.apiKeySecretPropertyName, secret);
        }

        //5. Explicitly-configured values always take precedence:
        id = valueOf(this.apiKeyId, id);
        secret = valueOf(this.apiKeySecret, secret);

        if (!Strings.hasText(id)) {
            String msg = "Unable to find an API Key 'id', either from explicit configuration (" +
                         getClass().getSimpleName() + ".setApiKeyId) or from fallback locations " +
                         "1) the system property 'stormpath.apiKey.id', 2) environment variable " +
                         "'STORMPATH_API_KEY_ID' or 3) in the default apiKey.properties file location " +
                         DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION + ".  Please ensure you manually configure an " +
                         "API Key ID or ensure that it exists in one of these fallback locations.";
            throw new IllegalStateException(msg);
        }

        if (!Strings.hasText(secret)) {
            String msg = "Unable to find an API Key 'secret', either from explicit configuration (" +
                         getClass().getSimpleName() + ".setApiKeySecret) or from fallback locations " +
                         "1) the system property 'stormpath.apiKey.secret', 2) environment variable " +
                         "'STORMPATH_API_KEY_SECRET' or 3) in the default apiKey.properties file location " +
                         DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION + ".  Please ensure you manually configure an " +
                         "API Key secret or ensure that it exists in one of these fallback locations.";
            throw new IllegalStateException(msg);
        }

        return createApiKey(id, secret);
    }

    //since 0.5
    protected ApiKey createApiKey(String id, String secret) {
        return new ClientApiKey(id, secret);
    }

    private static String getPropertyValue(Properties properties, String propName) {
        String value = properties.getProperty(propName);
        if (value != null) {
            value = value.trim();
            if ("".equals(value)) {
                value = null;
            }
        }
        return value;
    }

    private static String valueOf(String discoveredValue, String defaultValue) {
        if (!Strings.hasText(discoveredValue)) {
            return defaultValue;
        }
        return discoveredValue;

    }

    private static String getPropertyValue(Properties properties, String propName, String defaultValue) {
        String value = getPropertyValue(properties, propName);
        return valueOf(value, defaultValue);
    }

    protected Reader createFileReader(String apiKeyFileLocation) throws IOException {
        InputStream is = ResourceUtils.getInputStreamForPath(apiKeyFileLocation);
        return toReader(is);
    }

    private static Reader toReader(InputStream is) throws IOException {
        return new InputStreamReader(is, "ISO-8859-1");
    }

    private static Properties toProperties(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }

    private static class ResourceUtils {

        /** Resource path prefix that specifies to load from a classpath location, value is <b>{@code classpath:}</b> */
        public static final String CLASSPATH_PREFIX = "classpath:";
        /** Resource path prefix that specifies to load from a url location, value is <b>{@code url:}</b> */
        public static final String URL_PREFIX       = "url:";
        /** Resource path prefix that specifies to load from a file location, value is <b>{@code file:}</b> */
        public static final String FILE_PREFIX      = "file:";

        /** Prevent instantiation. */
        private ResourceUtils() {
        }

        /**
         * Returns {@code true} if the resource path is not null and starts with one of the recognized
         * resource prefixes ({@link #CLASSPATH_PREFIX CLASSPATH_PREFIX},
         * {@link #URL_PREFIX URL_PREFIX}, or {@link #FILE_PREFIX FILE_PREFIX}), {@code false} otherwise.
         *
         * @param resourcePath the resource path to check
         * @return {@code true} if the resource path is not null and starts with one of the recognized
         *         resource prefixes, {@code false} otherwise.
         * @since 0.8
         */
        @SuppressWarnings({"UnusedDeclaration"})
        public static boolean hasResourcePrefix(String resourcePath) {
            return resourcePath != null &&
                   (resourcePath.startsWith(CLASSPATH_PREFIX) ||
                    resourcePath.startsWith(URL_PREFIX) ||
                    resourcePath.startsWith(FILE_PREFIX));
        }

        /**
         * Returns the InputStream for the resource represented by the specified path, supporting scheme
         * prefixes that direct how to acquire the input stream
         * ({@link #CLASSPATH_PREFIX CLASSPATH_PREFIX},
         * {@link #URL_PREFIX URL_PREFIX}, or {@link #FILE_PREFIX FILE_PREFIX}).  If the path is not prefixed by one
         * of these schemes, the path is assumed to be a file-based path that can be loaded with a
         * {@link java.io.FileInputStream FileInputStream}.
         *
         * @param resourcePath the String path representing the resource to obtain.
         * @return the InputStraem for the specified resource.
         * @throws java.io.IOException if there is a problem acquiring the resource at the specified path.
         */
        public static InputStream getInputStreamForPath(String resourcePath) throws IOException {

            InputStream is;
            if (resourcePath.startsWith(CLASSPATH_PREFIX)) {
                is = loadFromClassPath(stripPrefix(resourcePath));

            } else if (resourcePath.startsWith(URL_PREFIX)) {
                is = loadFromUrl(stripPrefix(resourcePath));

            } else if (resourcePath.startsWith(FILE_PREFIX)) {
                is = loadFromFile(stripPrefix(resourcePath));

            } else {
                is = loadFromFile(resourcePath);
            }

            if (is == null) {
                throw new IOException("Resource [" + resourcePath + "] could not be found.");
            }

            return is;
        }

        private static InputStream loadFromFile(String path) throws IOException {
            return new FileInputStream(path);
        }

        private static InputStream loadFromUrl(String urlPath) throws IOException {
            URL url = new URL(urlPath);
            return url.openStream();
        }

        private static InputStream loadFromClassPath(String path) {
            return Classes.getResourceAsStream(path);
        }

        private static String stripPrefix(String resourcePath) {
            return resourcePath.substring(resourcePath.indexOf(":") + 1);
        }
    }

}