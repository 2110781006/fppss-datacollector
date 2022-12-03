/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.connectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.fppssdc.model.MeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

public class FppssRestConnector
{
    private String url;
    private HttpClient client = null;

    public static String accessToken = "";

    public FppssRestConnector(String url)
    {
        this.url = url;

        try
        {
            SSLContext sslContext = SSLContext.getInstance("SSL");

            // set up a TrustManager that trusts everything
            sslContext.init(null, new TrustManager[]{new X509TrustManager()
            {
                public X509Certificate[] getAcceptedIssuers()
                {
                    //System.out.println("getAcceptedIssuers =============");
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType)
                {
                    //System.out.println("checkClientTrusted =============");
                }

                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType)
                {
                    //System.out.println("checkServerTrusted =============");
                }
            }}, new SecureRandom());

            client = HttpClient.newBuilder()
                    .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .sslContext(sslContext)
                    .build();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
                .header("Authorization", "Bearer "+accessToken)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var providerAccounts = gson.fromJson(response.body(), ProviderAccountObject[].class);

        return providerAccounts;
    }

    public void login() throws Exception
    {
        String body = ""+
                "client_id="+System.getenv("FPPSS_KEYCLOAK_CLIENT_ID")+"&"+
                "username="+System.getenv("FPPSS_KEYCLOAK_USER")+"&"+
                "password="+System.getenv("FPPSS_KEYCLOAK_PASSWORD")+"&"+
                "grant_type=password";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(System.getenv("FPPSS_KEYCLOAK_TOKEN_URL")))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "*/*")
                //.header("Accept-Encoding", "gzip, deflate, br")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.statusCode() != 200 )//error while login
            throw new Exception("Login error FPPSS-API");

        if ( response.statusCode() == 200 )
        {
            Gson gson = new Gson();
            var resp = gson.fromJson((String)response.body(), JsonObject.class);

            accessToken = resp.get("access_token").toString().replace("\"", "");
        }
    }

    /*public void logoff() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/unisess/v1/logout"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                //.POST(HttpRequest.BodyPublishers.ofString(body))
                .GET()
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }*/

    /**
     * Get last timestamp of meteringpoint datapoint and resolution
     * @param meteringPoint
     * @param resolution
     * @param providerAccountId
     * @return
     * @throws Exception
     */
    public OffsetDateTime getMeterLastTimestamp(MeteringPoint meteringPoint, TimeValueObject.Resolution resolution, int providerAccountId) throws Exception
    {
        String postfixType = "";
        String postfixResolution = "";

        switch (meteringPoint.getType())
        {
            case Consumption: postfixType="consumption"; break;
            case Feedin: postfixType="feedin"; break;
            case Production: postfixType="production"; break;
        }

        switch (resolution)
        {
            case hour: postfixResolution="hour"; break;
            case day: postfixResolution="day"; break;
            case month: postfixResolution="month"; break;
            case year: postfixResolution="year"; break;
            case spontan: postfixResolution="spontan"; break;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/api/v1/values/"+postfixType+"/"+postfixResolution+"/lastTimeStamp/"+providerAccountId+"/"+meteringPoint.getId()+"/"+meteringPoint.getDatapoints().get(0)))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Authorization", "Bearer "+accessToken)
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
     * Save consumption meter year values in database
     * @param timeValueObjects
     */
    public void saveMeterValuesInDatabase(MeteringPoint meteringPoint, TimeValueObject.Resolution resolution, ArrayList<TimeValueObject> timeValueObjects) throws Exception
    {
        String postfixType = "";
        String postfixResolution = "";

        switch (meteringPoint.getType())
        {
            case Consumption: postfixType="consumption"; break;
            case Feedin: postfixType="feedin"; break;
            case Production: postfixType="production"; break;
        }

        switch (resolution)
        {
            case hour: postfixResolution="hour"; break;
            case day: postfixResolution="day"; break;
            case month: postfixResolution="month"; break;
            case year: postfixResolution="year"; break;
            case spontan: postfixResolution="spontan"; break;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+"/api/v1/values/"+postfixType+"/"+postfixResolution))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("content-type", "application/json")
                .header("Authorization", "Bearer "+accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(TimeValueObject.toJson(timeValueObjects)))
                .build();

        var res = client.send(request, HttpResponse.BodyHandlers.ofString());

    }
}
