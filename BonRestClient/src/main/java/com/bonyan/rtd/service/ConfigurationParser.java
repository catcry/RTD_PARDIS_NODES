package com.bonyan.rtd.service;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.bonyan.rtd.AESRestCrypto;
import com.bonyan.rtd.RestClient;
import org.eclipse.jetty.util.security.Password;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationParser extends DefaultHandler {
    public static final String SLASH = "/";
    public static final String CONNECTOR_STRING = "connector";
    private final RestClient app;
    private List<ConfigurationConnector> connectors = new ArrayList<>();
    private List<ConfigurationResource> resources = new ArrayList<>();
    private ConfigurationConnector connector;
    private ConfigurationResource resource;
    private ConfigurationClient client;
    private ConfigurationKeyStore clientKeyStore;
    private ConfigurationKeyStore clientTrustStore;
    private List<String> cipherSuites = new ArrayList<>();
    private List<String> supportedCipherSuites;

    public ConfigurationParser(RestClient app) {
        this.app = app;
    }

    public static String convertToFileURL(String filename) {
        String path = (new File(filename)).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith(SLASH)) {
            path = SLASH + path;
        }

        return "file:" + path;
    }

    public RestClient getApp() {
        return app;
    }

    public List<ConfigurationConnector> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<ConfigurationConnector> connectors) {
        this.connectors = connectors;
    }

    public List<ConfigurationResource> getResources() {
        return resources;
    }

    public void setResources(List<ConfigurationResource> resources) {
        this.resources = resources;
    }

    public ConfigurationConnector getConnector() {
        return connector;
    }

    public void setConnector(ConfigurationConnector connector) {
        this.connector = connector;
    }

    public ConfigurationResource getResource() {
        return resource;
    }

    public void setResource(ConfigurationResource resource) {
        this.resource = resource;
    }

    public ConfigurationClient getClient() {
        return client;
    }

    public void setClient(ConfigurationClient client) {
        this.client = client;
    }

    public ConfigurationKeyStore getClientKeyStore() {
        return clientKeyStore;
    }

    public void setClientKeyStore(ConfigurationKeyStore clientKeyStore) {
        this.clientKeyStore = clientKeyStore;
    }

    public ConfigurationKeyStore getClientTrustStore() {
        return clientTrustStore;
    }

    public void setClientTrustStore(ConfigurationKeyStore clientTrustStore) {
        this.clientTrustStore = clientTrustStore;
    }

    public List<String> getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public List<String> getSupportedCipherSuites() {
        return supportedCipherSuites;
    }

    public void setSupportedCipherSuites(List<String> supportedCipherSuites) {
        this.supportedCipherSuites = supportedCipherSuites;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        switch (qName) {
            case "userCredentials":
                this.client.userCredentialsInit();
                break;
            case "user":
                handleUser(atts);
                break;
            case "connectors":
                handleConnectors();
                break;
            case CONNECTOR_STRING:
                handleConnector(atts);
                break;
            case "keyStore":
                handleKeyStore(atts);
                break;
            case "trustStore":
                handleTrustStore(atts);
                break;
            case "includeCipherSuites":
            case "excludeCipherSuites":
                handleCipherSuites();
                break;
            case "cipherSuite":
                handleCipherSuite(atts);
                break;
            case "resources":
                handleResources();
                break;
            case "resource":
                handleResource(atts);
                break;
            case "url":
                handleUrl(atts);
                break;
            default:
                // Handle unexpected elements or ignore them
                break;
        }
    }

    private void handleUser(Attributes atts) {
        if (this.client.userCredentialsList != null) {
            this.client.userCredentialsList.add(new UserCredentials(
                    atts.getValue("name"),
                    atts.getValue("password_type"),
                    atts.getValue("password")
            ));
        }
    }

    private void handleConnectors() {
        this.connectors = new ArrayList<>();
    }

    private void handleConnector(Attributes atts) {
        this.connector = new ConfigurationConnector();
        this.connector.type = atts.getValue("type");
        this.connector.hostName = atts.getValue("hostName");
        this.connector.port = Integer.parseInt(atts.getValue("port"));
        this.connector.name = atts.getValue("name");
        this.connector.keyStore = null;
        this.connector.trustStore = null;
    }

    private void handleKeyStore(Attributes atts) {
        ConfigurationKeyStore keyStore = new ConfigurationKeyStore();
        keyStore.file = atts.getValue("file");
        keyStore.type = atts.getValue("type");
        keyStore.alias = atts.getValue("alias");
        keyStore.keyStorePass = atts.getValue("keyStorePass");
        keyStore.keyPass = atts.getValue("keyPass");
        this.client.keyStore = keyStore;
    }

    private void handleTrustStore(Attributes atts) {
        ConfigurationKeyStore trustStore = new ConfigurationKeyStore();
        trustStore.file = atts.getValue("file");
        trustStore.type = atts.getValue("type");
        trustStore.alias = atts.getValue("alias");
        trustStore.keyStorePass = atts.getValue("keyStorePass");
        trustStore.keyPass = atts.getValue("keyPass");
        this.client.trustStore = trustStore;
    }

    private void handleCipherSuites() {
        this.cipherSuites = new ArrayList<>();
    }

    private void handleCipherSuite(Attributes atts) {
        String cipherSuite = atts.getValue("name");
        if (this.supportedCipherSuites.contains(cipherSuite)) {
            this.cipherSuites.add(cipherSuite);
        } else {
            this.app.getNodeContext().writeMessage("RESTIF103", new String[]{cipherSuite});
            RestClient.logger.info("Unsupported cipher suite: " + cipherSuite);
        }
    }

    private void handleResources() {
        this.resources = new ArrayList<>();
    }

    private void handleResource(Attributes atts) {
        this.resource = new ConfigurationResource();
        this.resource.name = atts.getValue("name");
        this.resource.connector = atts.getValue(CONNECTOR_STRING);
        this.resource.path = atts.getValue("path");
        this.resource.urls = new ArrayList<>();
    }

    private void handleUrl(Attributes atts) {
        ConfigurationURL url = new ConfigurationURL();
        url.type = atts.getValue("type");
        url.name = atts.getValue("name");
        this.resource.urls.add(url);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equals(CONNECTOR_STRING)) {
            this.connectors.add(this.connector);
        }

        if (qName.equals("includeCipherSuites")) {
            this.client.includeCipherSuites = this.cipherSuites;
        }

        if (qName.equals("excludeCipherSuites")) {
            this.client.excludeCipherSuites = this.cipherSuites;
        }

        if (qName.equals("resource")) {
            this.resources.add(this.resource);
        }

    }

    public void parseConfigurationFile(InputStream configFileStream) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        XMLReader reader = null;

        try {
            reader = factory.newSAXParser().getXMLReader();
        } catch (SAXException | ParserConfigurationException exception) {
            Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, exception);
            this.app.getNodeContext().writeMessage("RESTIF007", new String[]{exception.getMessage()});
            RestClient.logger.error(exception, "Unable to create XML parser");
        }

        Objects.requireNonNull(reader).setContentHandler(this);

        try {
            reader.parse(new InputSource(configFileStream));
        } catch (SAXException | IOException exception) {
            Logger.getLogger(RestClient.class.getName()).log(Level.SEVERE, null, exception);
            this.app.getNodeContext().writeMessage("RESTIF008", exception.getMessage());
            RestClient.logger.error(exception, "Unable to parse configuration file: /configuration.xml");
        }
    }

    public static class ConfigurationClient {
        private ConfigurationKeyStore keyStore = null;
        private ConfigurationKeyStore trustStore = null;
        private List<UserCredentials> userCredentialsList;
        private List<String> includeCipherSuites = new ArrayList<>();
        private List<String> excludeCipherSuites = new ArrayList<>();

        public ConfigurationKeyStore getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(ConfigurationKeyStore keyStore) {
            this.keyStore = keyStore;
        }

        public ConfigurationKeyStore getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(ConfigurationKeyStore trustStore) {
            this.trustStore = trustStore;
        }

        public List<UserCredentials> getUserCredentialsList() {
            return userCredentialsList;
        }

        public void setUserCredentialsList(List<UserCredentials> userCredentialsList) {
            this.userCredentialsList = userCredentialsList;
        }

        public List<String> getIncludeCipherSuites() {
            return includeCipherSuites;
        }

        public void setIncludeCipherSuites(List<String> includeCipherSuites) {
            this.includeCipherSuites = includeCipherSuites;
        }

        public List<String> getExcludeCipherSuites() {
            return excludeCipherSuites;
        }

        public void setExcludeCipherSuites(List<String> excludeCipherSuites) {
            this.excludeCipherSuites = excludeCipherSuites;
        }

        public void userCredentialsInit() {
            this.userCredentialsList = new ArrayList<>();
        }
    }

    public static class ConfigurationConnector {
        private String type = null;
        private String hostName = "0.0.0.0";
        private int port = 0;
        private String name = null;
        private ConfigurationKeyStore keyStore;
        private ConfigurationKeyStore trustStore;
        private List<String> includeCipherSuites = new ArrayList<>();
        private List<String> excludeCipherSuites = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public ConfigurationKeyStore getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(ConfigurationKeyStore keyStore) {
            this.keyStore = keyStore;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ConfigurationKeyStore getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(ConfigurationKeyStore trustStore) {
            this.trustStore = trustStore;
        }

        public List<String> getIncludeCipherSuites() {
            return includeCipherSuites;
        }

        public void setIncludeCipherSuites(List<String> includeCipherSuites) {
            this.includeCipherSuites = includeCipherSuites;
        }

        public List<String> getExcludeCipherSuites() {
            return excludeCipherSuites;
        }

        public void setExcludeCipherSuites(List<String> excludeCipherSuites) {
            this.excludeCipherSuites = excludeCipherSuites;
        }
    }

    public static class ConfigurationKeyStore {
        private String file;
        private String type;
        private String alias;
        private String keyStorePass;
        private String keyPass;

        public ConfigurationKeyStore() {
            this.file = null;
            this.type = null;
            this.alias = null;
            this.keyStorePass = null;
            this.keyPass = null;
        }

        public ConfigurationKeyStore(String file, String type, String alias, String keyStorePass, String keyPass) {
            this.file = file;
            this.type = type;
            this.alias = alias;
            this.keyStorePass = keyStorePass;
            this.keyPass = keyPass;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getKeyStorePass() {
            return keyStorePass;
        }

        public void setKeyStorePass(String keyStorePass) {
            this.keyStorePass = keyStorePass;
        }

        public String getKeyPass() {
            return keyPass;
        }

        public void setKeyPass(String keyPass) {
            this.keyPass = keyPass;
        }
    }

    public static class ConfigurationResource {
        private String name;
        private String connector;
        private String path;
        private List<ConfigurationURL> urls;

        public ConfigurationResource() {
            this.name = null;
            this.connector = null;
            this.path = null;
            this.urls = new ArrayList<>();
        }

        public ConfigurationResource(String name, String connector, String path, List<ConfigurationURL> urls) {
            this.name = name;
            this.connector = connector;
            this.path = path;
            this.urls = urls;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getConnector() {
            return connector;
        }

        public void setConnector(String connector) {
            this.connector = connector;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<ConfigurationURL> getUrls() {
            return urls;
        }

        public void setUrls(List<ConfigurationURL> urls) {
            this.urls = urls;
        }
    }

    public static class ConfigurationURL {
        private String type = null;
        private String name = null;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UserCredentials {
        private String userId;
        private String password;
        private String passwordType;

        public UserCredentials(String userId, String passwordType, String password) {
            this.userId = userId;
            this.passwordType = passwordType;
            RestClient.logger.debug("Retrieved UserId: " + userId + " from credential storage.");

            if ("AES".equals(passwordType)) {
                this.password = AESRestCrypto.decryptPassword(password).replaceAll("\\u0000", "");
            } else if ("OBF".equals(passwordType)) {
                this.password = Password.deobfuscate(password);
            } else {
                this.password = password;
            }
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordType() {
            return passwordType;
        }

        public void setPasswordType(String passwordType) {
            this.passwordType = passwordType;
        }
    }
}
