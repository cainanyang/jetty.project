//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientURITest extends AbstractHttpClientServerTest
{
    public HttpClientURITest(SslContextFactory sslContextFactory)
    {
        super(sslContextFactory);
    }

    @Test
    public void testIPv6Host() throws Exception
    {
        start(new EmptyServerHandler());

        String host = "::1";
        Request request = client.newRequest(host, connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS);

        Assert.assertEquals(host, request.getHost());
        StringBuilder uri = new StringBuilder();
        URIUtil.appendSchemeHostPort(uri, scheme, host, connector.getLocalPort());
        Assert.assertEquals(uri.toString(), request.getURI().toString());

        Assert.assertEquals(HttpStatus.OK_200, request.send().getStatus());
    }

    @Test
    public void testPath() throws Exception
    {
        final String path = "/path";
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(path);

        Assert.assertEquals(path, request.getPath());
        Assert.assertNull(request.getQuery());
        Fields params = request.getParams();
        Assert.assertEquals(0, params.size());
        Assert.assertTrue(request.getURI().toString().endsWith(path));

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testPathWithQuery() throws Exception
    {
        String name = "a";
        String value = "1";
        final String query = name + "=" + value;
        final String path = "/path";
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
            }
        });

        String pathQuery = path + "?" + query;
        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(pathQuery);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(1, params.size());
        Assert.assertEquals(value, params.get(name).value());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testPathWithParam() throws Exception
    {
        String name = "a";
        String value = "1";
        final String query = name + "=" + value;
        final String path = "/path";
        String pathQuery = path + "?" + query;
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(path)
                .param(name, value);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(1, params.size());
        Assert.assertEquals(value, params.get(name).value());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testPathWithQueryAndParam() throws Exception
    {
        String name1 = "a";
        String value1 = "1";
        String name2 = "b";
        String value2 = "2";
        final String query = name1 + "=" + value1 + "&" + name2 + "=" + value2;
        final String path = "/path";
        String pathQuery = path + "?" + query;
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(path + "?" + name1 + "=" + value1)
                .param(name2, value2);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(2, params.size());
        Assert.assertEquals(value1, params.get(name1).value());
        Assert.assertEquals(value2, params.get(name2).value());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testPathWithQueryAndParamValueEncoded() throws Exception
    {
        final String name1 = "a";
        final String value1 = "\u20AC";
        final String encodedValue1 = URLEncoder.encode(value1, "UTF-8");
        final String name2 = "b";
        final String value2 = "\u00A5";
        String encodedValue2 = URLEncoder.encode(value2, "UTF-8");
        final String query = name1 + "=" + encodedValue1 + "&" + name2 + "=" + encodedValue2;
        final String path = "/path";
        String pathQuery = path + "?" + query;
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
                Assert.assertEquals(value1, request.getParameter(name1));
                Assert.assertEquals(value2, request.getParameter(name2));
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(path + "?" + name1 + "=" + encodedValue1)
                .param(name2, value2);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(2, params.size());
        Assert.assertEquals(value1, params.get(name1).value());
        Assert.assertEquals(value2, params.get(name2).value());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testNoParameterNameNoParameterValue() throws Exception
    {
        final String path = "/path";
        final String query = "="; // Bogus query
        String pathQuery = path + "?" + query;
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(pathQuery);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(0, params.size());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testNoParameterNameWithParameterValue() throws Exception
    {
        final String path = "/path";
        final String query = "=1"; // Bogus query
        String pathQuery = path + "?" + query;
        start(new AbstractHandler()
        {
            @Override
            public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                Assert.assertEquals(path, request.getRequestURI());
                Assert.assertEquals(query, request.getQueryString());
            }
        });

        Request request = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scheme)
                .timeout(5, TimeUnit.SECONDS)
                .path(pathQuery);

        Assert.assertEquals(path, request.getPath());
        Assert.assertEquals(query, request.getQuery());
        Assert.assertTrue(request.getURI().toString().endsWith(pathQuery));
        Fields params = request.getParams();
        Assert.assertEquals(0, params.size());

        ContentResponse response = request.send();

        Assert.assertEquals(HttpStatus.OK_200, response.getStatus());
    }
}
