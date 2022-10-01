/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc;

import com.google.gson.Gson;
import org.fppssdc.model.ProviderAccountObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class FppssRestConnector
{
    private String url;
    private HttpClient client = null;

    public FppssRestConnector(String url)
    {
        this.url = url;

        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public ProviderAccountObject[] getProviderAccounts() throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/api/v1/provideraccounts"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var providerAccounts = gson.fromJson(response.body(), ProviderAccountObject[].class);

        return providerAccounts;
    }
}
