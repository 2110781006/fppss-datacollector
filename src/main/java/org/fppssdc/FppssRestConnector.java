/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc;

import com.google.gson.*;
import org.fppssdc.model.MeeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    /**
     * Get all provider accounts
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProviderAccountObject[] getProviderAccounts() throws Exception
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

    /**
     * Get last timestamp of meeteringpoint datapoint and resolution
     * @param meeteringPoint
     * @param resolution
     * @param providerAccountId
     * @return
     * @throws Exception
     */
    public OffsetDateTime getMeterLastTimestamp(MeeteringPoint meeteringPoint, TimeValueObject.Resolution resolution, int providerAccountId) throws Exception
    {
        String postfixType = "";
        String postfixResolution = "";

        switch (meeteringPoint.getType())
        {
            case Consumption: postfixType="consumption"; break;
            case Feedin: postfixType="feedin"; break;
        }

        switch (resolution)
        {
            case hour: postfixResolution="hour"; break;
            case day: postfixResolution="day"; break;
            case month: postfixResolution="month"; break;
            case year: postfixResolution="year"; break;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/api/v1/values/"+postfixType+"/"+postfixResolution+"/lastTimeStamp/"+providerAccountId+"/"+meeteringPoint.getId()+"/"+meeteringPoint.getDatapoints().get(0)))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.body().isEmpty() )//no entry available
        {
            OffsetDateTime of = OffsetDateTime.of(2015,1,1,1,1,1,1, ZoneOffset.UTC);
            return of;
        }
        else
        {
            return OffsetDateTime.parse(response.body().replace("\"",""));
        }
    }

    /**
     * Save consumption meeter year values in database
     * @param timeValueObjects
     */
    public void saveMeterValuesInDatabase(MeeteringPoint meeteringPoint, TimeValueObject.Resolution resolution, ArrayList<TimeValueObject> timeValueObjects) throws Exception
    {
        String postfixType = "";
        String postfixResolution = "";

        switch (meeteringPoint.getType())
        {
            case Consumption: postfixType="consumption"; break;
            case Feedin: postfixType="feedin"; break;
        }

        switch (resolution)
        {
            case hour: postfixResolution="hour"; break;
            case day: postfixResolution="day"; break;
            case month: postfixResolution="month"; break;
            case year: postfixResolution="year"; break;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/api/v1/values/"+postfixType+"/"+postfixResolution))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(TimeValueObject.toJson(timeValueObjects)))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
