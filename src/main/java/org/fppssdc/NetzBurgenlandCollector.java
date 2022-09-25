/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fppssdc.model.ProviderAccountObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class NetzBurgenlandCollector extends Collector
{
    static final String apiUrl = "https://smartmeter.netzburgenland.at/enView.Portal/api";
    private ProviderAccountObject providerAccount;

    private HttpClient client = null;

    /**
     * Constructor
     */
    public NetzBurgenlandCollector(ProviderAccountObject providerAccount)
    {
        this.providerAccount = providerAccount;

        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    private ArrayList<MeeteringPoint> getConsumptionMeeteringPoints() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/meteringpoints/consumption"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        ArrayList<MeeteringPoint> meeteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeeteringPoint.MeeteringPointType type= MeeteringPoint.MeeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeeteringPoint.MeeteringPointType.AccountingPoint;

            meeteringPoints.add(new MeeteringPoint(e.get("identifier").toString().replace("\"", ""), type, new ArrayList<>()));
        }

        return meeteringPoints;
    }

    private ArrayList<MeeteringPoint> getFeedInMeeteringPoints() throws Exception
    {//testcommit
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/meteringpoints/feedin"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        ArrayList<MeeteringPoint> meeteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeeteringPoint.MeeteringPointType type= MeeteringPoint.MeeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeeteringPoint.MeeteringPointType.AccountingPoint;
            //System.out.println(e.get("dataPoints"));
            meeteringPoints.add(new MeeteringPoint(e.get("identifier").toString().replace("\"", ""), type, new ArrayList<>()));
        }

        return meeteringPoints;
    }

    public void saveMeterConsumptionYearValuesInDatabase()
    {

    }

    public void getMeterConsumptionDayValuesFromNetzBurgenland(MeeteringPoint meeteringPoint) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/month?end=2022-02-01T00:00:00%2B01:00&meteringPointIdentifier="+meeteringPoint.id+"&start=2022-01-01T00:00:00%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    public void getMeterConsumptionMonthValuesFromNetzBurgenland(MeeteringPoint meeteringPoint) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/year?end=2023-01-01T00:00:00%2B01:00&meteringPointIdentifier="+meeteringPoint.id+"&start=2022-01-01T00:00:00%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    public void getMeterConsumptionYearValuesFromNetzBurgenland(MeeteringPoint meeteringPoint) throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/overview?end=2023-01-01T00:00:00%2B01:00&meteringPointIdentifier="+meeteringPoint.id+"&start=2018-01-01T00:00:00%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

    private void login() throws Exception
    {
        SessionInfo sessionInfo = getSessionInfo();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.netzburgenland.at:8453/auth/realms/netzburgenland/login-actions/authenticate?"+
                        "session_code="+sessionInfo.code+"&"+
                        "execution="+sessionInfo.execution+"&"+
                        "client_id="+sessionInfo.clientId+"&"+
                        "tab_id="+sessionInfo.tabId))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.66 Safari/537.36 Edg/103.0.1264.44")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+providerAccount.getProviderAccountUsername()+"&password="+providerAccount.getProviderAccountPassword()))
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.statusCode() != 200 || ((String)response.body()).contains("error") )//error while login
            throw new Exception("Login error");
    }

    private void logoff() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://smartmeter.netzburgenland.at/EnView.Portal/Account/LogOff"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.66 Safari/537.36 Edg/103.0.1264.44")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+providerAccount.getProviderAccountUsername()+"&password="+providerAccount.getProviderAccountPassword()))
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.statusCode() != 200 || ((String)response.body()).contains("error") )//error while login
            throw new Exception("Logoff error");
    }

    /**
     * get session info
     * @return
     */
    private SessionInfo getSessionInfo() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://smartmeter.netzburgenland.at/enView.Portal/"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        //get login form
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //get dom of login page
        Document dom = Jsoup.parse(response.body());

        String loginurl = dom.getElementById("kc-form-login").attr("action");
        URL uri = new URL(loginurl);
        ArrayList<String> parameter = new ArrayList<String>(Arrays.stream(uri.getQuery().split("&")).toList());

        HashMap<String, String> parameterMap = new HashMap<>();

        for ( var par : parameter )//store parameter to hashmap
            parameterMap.put(par.split("=")[0], par.split("=")[1]);

        SessionInfo sessionInfo = new SessionInfo();

        sessionInfo.code = parameterMap.get("session_code");
        sessionInfo.execution = parameterMap.get("execution");
        sessionInfo.clientId = parameterMap.get("client_id");
        sessionInfo.tabId = parameterMap.get("tab_id");

        return sessionInfo;
    }

    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                login();

                //////////////
                //consumption
                //////////////
                ArrayList<MeeteringPoint> consumptionMeeteringPoints = new ArrayList<>();

                consumptionMeeteringPoints = getConsumptionMeeteringPoints();

                for (var meeteringPoint : consumptionMeeteringPoints)
                {
                    if (meeteringPoint.meeteringPointType == MeeteringPoint.MeeteringPointType.AccountingPoint)//consumption
                    {
                        //year values
                        getMeterConsumptionYearValuesFromNetzBurgenland(meeteringPoint);
                        saveMeterConsumptionYearValuesInDatabase();

                        //month values
                        getMeterConsumptionMonthValuesFromNetzBurgenland(meeteringPoint);

                        //day values
                        getMeterConsumptionDayValuesFromNetzBurgenland(meeteringPoint);
                    }
                }

                //////////////
                //feedin
                //////////////
                ArrayList<MeeteringPoint> feedinMeeteringPoints = new ArrayList<>();

                feedinMeeteringPoints = getFeedInMeeteringPoints();

                logoff();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                Thread.sleep(10000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Session info struct
     */
    private class SessionInfo
    {
        public String code;
        public String execution;
        public String clientId;
        public String tabId;
    }

    private class MeeteringPoint
    {
        enum MeeteringPointType
        {
            AccountingPoint
        }

        private String id;
        private MeeteringPointType meeteringPointType;
        ArrayList<String> datapoints;

        public MeeteringPoint(String id, MeeteringPointType meeteringPointType, ArrayList<String> datapoints)
        {
            this.id = id;
            this.meeteringPointType = meeteringPointType;
            this.datapoints = datapoints;
        }
    }
}
