/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.security.httpsignature;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.httpsignature.MessageSigner;
import org.apache.cxf.rs.security.httpsignature.MessageVerifier;
import org.apache.cxf.rs.security.httpsignature.filters.CreateSignatureClientFilter;
import org.apache.cxf.rs.security.httpsignature.filters.VerifySignatureClientFilter;
import org.apache.cxf.rt.security.rs.PrivateKeyPasswordProvider;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test for the HTTP Signature functionality in the cxf-rt-rs-security-http-signature module.
 */
public class JAXRSHTTPSignatureTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerHttpSignature.PORT;

    @BeforeClass
    public static void startServers() {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerHttpSignature.class, true));
    }

    @Test
    public void testHttpSignature() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigprops/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureOutProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignaturePropertiesPasswordProvider() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        PrivateKeyPasswordProvider passwordProvider = storeProperties -> "password".toCharArray();
        properties.put("rs.security.key.password.provider", passwordProvider);
        properties.put("rs.security.http.signature.key.id", "alice-key-id");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureRsaSha512() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", "SHA-256", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureRsaSha512ServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", "SHA-256", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512props/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignaturePropertiesRsaSha512() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.signature.algorithm", "rsa-sha512");
        properties.put("rs.security.http.signature.key.id", "alice-key-id");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponse() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        List<Object> providers = new ArrayList<>();
        providers.add(signatureFilter);
        providers.add(signatureResponseFilter);
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponseServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        List<Object> providers = new ArrayList<>();
        providers.add(signatureFilter);
        providers.add(signatureResponseFilter);
        String address = "http://localhost:" + PORT + "/httpsigresponseprops/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponseProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new CreateSignatureClientFilter());
        providers.add(new VerifySignatureClientFilter());
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
            "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        properties.put("rs.security.signature.in.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/bob.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureNoRequestTarget() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept");
        MessageSigner messageSigner =
            new MessageSigner("rsa-sha512", "SHA-256", keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignSpecificHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignSpecificHeaderProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        List<String> headerList = Arrays.asList("accept", "(request-target)");
        properties.put("rs.security.http.signature.out.headers", headerList);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHeaderTrailingWhitespace() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("custom", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyid -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        client.header("custom", " someval    ");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testMultipleHeaderConcatenation() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("custom", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        client.header("custom", "someval, someval2");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    //
    // Negative tests
    //

    @Test
    public void testNonMatchingSignatureAlgorithm() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", "SHA-256", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testNoHttpSignature() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testWrongHTTPMethod() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setHttpMethod("GET");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testWrongURI() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setUri("/httpsig/bookstore/books2");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testChangedSignatureMethod() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setChangeSignatureAlgorithm(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testEmptySignatureValue() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setEmptySignatureValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testChangedSignatureValue() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setChangeSignatureValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testDifferentSigningKey() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        MessageSigner messageSigner = new MessageSigner(keyId -> keyPair.getPrivate(), "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testHttpSignatureMissingRequiredHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testUnknownKeyId() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "unknown-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testPropertiesWrongSignatureVerification() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new CreateSignatureClientFilter());
        providers.add(new VerifySignatureClientFilter());
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
            "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        properties.put("rs.security.signature.in.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        try {
            client.post(new Book("CXF", 126L));
            fail("Failure expected on the wrong signature verification keystore");
        } catch (Exception ex) {
            // expected
        }
    }

    @Provider
    @Priority(Priorities.AUTHENTICATION)
    private final class ClientTestFilter implements ClientRequestFilter {

        private MessageSigner messageSigner;
        private String httpMethod;
        private String uri;
        private boolean changeSignatureAlgorithm;
        private boolean emptySignatureValue;
        private boolean changeSignatureValue;

        @Override
        public void filter(ClientRequestContext requestCtx) {

            MultivaluedMap<String, Object> requestHeaders = requestCtx.getHeaders();

            Map<String, List<String>> convertedHeaders = convertHeaders(requestHeaders);
            try {
                messageSigner.sign(convertedHeaders,
                                   uri != null ? uri : requestCtx.getUri().getPath(),
                                   httpMethod != null ? httpMethod : requestCtx.getMethod());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (changeSignatureAlgorithm) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                signatureValue = signatureValue.replace("rsa-sha256", "rsa-sha512");
                requestHeaders.put("Signature", Collections.singletonList(signatureValue));
            } else if (emptySignatureValue) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                signatureValue =
                    signatureValue.substring(0, signatureValue.indexOf("signature=") + "signature=\"".length()) + "\"";
                requestHeaders.put("Signature", Collections.singletonList(signatureValue));
            } else if (changeSignatureValue) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                String signature =
                    signatureValue.substring(signatureValue.indexOf("signature=") + "signature=\"".length(),
                                                            signatureValue.length() - 1);
                byte[] decodedSignature = Base64.getDecoder().decode(signature);
                decodedSignature[0]++;
                signatureValue =
                    signatureValue.substring(0, signatureValue.indexOf("signature=") + "signature=\"".length())
                    + Base64.getEncoder().encodeToString(decodedSignature)
                    + "\"";
                requestHeaders.put("Signature", Collections.singletonList(signatureValue));
            } else {
                requestHeaders.put("Signature", Collections.singletonList(convertedHeaders.get("Signature").get(0)));
            }
        }

        // Convert the headers from List<Object> -> List<String>
        private Map<String, List<String>> convertHeaders(MultivaluedMap<String, Object> requestHeaders) {
            Map<String, List<String>> convertedHeaders = new HashMap<>(requestHeaders.size());
            for (Map.Entry<String, List<Object>> entry : requestHeaders.entrySet()) {
                convertedHeaders.put(entry.getKey(),
                                     entry.getValue().stream().map(Object::toString).collect(Collectors.toList()));
            }
            return convertedHeaders;
        }

        public void setMessageSigner(MessageSigner messageSigner) {
            Objects.requireNonNull(messageSigner);
            this.messageSigner = messageSigner;
        }


        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public void setChangeSignatureAlgorithm(boolean changeSignatureAlgorithm) {
            this.changeSignatureAlgorithm = changeSignatureAlgorithm;
        }

        public void setChangeSignatureValue(boolean changeSignatureValue) {
            this.changeSignatureValue = changeSignatureValue;
        }

        public void setEmptySignatureValue(boolean emptySignatureValue) {
            this.emptySignatureValue = emptySignatureValue;
        }

    }
}
