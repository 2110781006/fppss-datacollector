/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fppssdc.connectors.FppssRestConnector;
import org.fppssdc.model.MeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class NetzBurgenlandCollector extends Collector
{
    static final String apiUrl = "https://smartmeter.netzburgenland.at/enView.Portal/api";

    private HttpClient client = null;

    /**
     * Constructor
     */
    public NetzBurgenlandCollector(ProviderAccountObject providerAccount, Integer interval)
    {
        super.providerAccount = providerAccount;
        super.interval = interval;

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
     * Get consumption meterpoints
     * @return
     * @throws Exception
     */
    private ArrayList<MeteringPoint> getConsumptionMeteringPoints() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/meteringpoints/consumption"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        ArrayList<MeteringPoint> meteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeteringPoint.MeteringPointType type= MeteringPoint.MeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeteringPoint.MeteringPointType.AccountingPoint;

            var datapoints = new ArrayList<String>();

            datapoints.add("1.8.1");//consumption dp

            meteringPoints.add(new MeteringPoint(e.get("identifier").toString().replace("\"", ""), MeteringPoint.Type.Consumption, type, datapoints));
        }

        return meteringPoints;
    }

    /**
     * Get feedin meterpoints
     * @return
     * @throws Exception
     */
    private ArrayList<MeteringPoint> getFeedInMeteringPoints() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/meteringpoints/feedin"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        ArrayList<MeteringPoint> meteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeteringPoint.MeteringPointType type= MeteringPoint.MeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeteringPoint.MeteringPointType.AccountingPoint;

            var datapoints = new ArrayList<String>();

            datapoints.add("2.8.1");//consumption dp

            meteringPoints.add(new MeteringPoint(e.get("identifier").toString().replace("\"", ""), MeteringPoint.Type.Feedin, type, datapoints));
        }

        return meteringPoints;
    }

    /**
     * Get consumption meter day values
     * @param meteringPoint
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterConsumptionDayValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/month?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif"))
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),tempTime.getDayOfMonth(),0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get feedin meter day values
     * @param meteringPoint
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterFeedinDayValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/month?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif") )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),tempTime.getDayOfMonth(),0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    public ArrayList<TimeValueObject> getMeterConsumptionMonthValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/year?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif") )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));

                        if ( tempTime.toEpochSecond() > OffsetDateTime.now().toEpochSecond() )//use start time because timestamp is in future
                            tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).minusDays(1);

                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    public ArrayList<TimeValueObject> getMeterFeedinMonthValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/year?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif"))
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));

                        if ( tempTime.toEpochSecond() > OffsetDateTime.now().toEpochSecond() )//use start time because timestamp is in future
                            tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).minusDays(1);

                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get year values of given timerange and meteringpoint
     * @param meteringPoint
     * @param from
     * @param to
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterConsumptionYearValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-01-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/overview?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif"))
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime dateTime = OffsetDateTime.of(OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).getYear(),
                                1,1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get year feedin values of given timerange and meteringpoint
     * @param meteringPoint
     * @param from
     * @param to
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterFeedinYearValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-01-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/overview?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                //.header("Connection", "keep-alive")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Hochtarif"))
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime dateTime = OffsetDateTime.of(OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).getYear(),
                                1,1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Login to netz burgenland api
     * @throws Exception
     */
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
                .POST(HttpRequest.BodyPublishers.ofString("username="+providerAccount.getProviderAccountUsername()+"&password="+providerAccount.getDecryptedPw()))
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.statusCode() != 200 || ((String)response.body()).contains("error") )//error while login
            throw new Exception("Login error");
    }

    /**
     * logoff from netz burgenland api
     * @throws Exception
     */
    private void logoff() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://smartmeter.netzburgenland.at/EnView.Portal/Account/LogOff"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.66 Safari/537.36 Edg/103.0.1264.44")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username="+providerAccount.getProviderAccountUsername()+"&password="+providerAccount.getDecryptedPw()))
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

    private ArrayList<TimeValueObject> getMeterConsumptionSpontanValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        if ( from.toEpochSecond() < to.minusMonths(5).toEpochSecond() )//max 1 month
            from = to.minusMonths(1);

        String fromStr = from.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));//always get a week before
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/date?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        if (response.statusCode() == 200)
        {
            Gson gson = new Gson();

            var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

            String datapointname = meteringPoint.getDatapoints().get(0);

            for ( JsonElement jsonElement : jsonArray )
            {
                JsonObject jsonObject = (JsonObject) jsonElement;

                if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Gesamtverbrauch"))
                {
                    JsonArray data = jsonObject.get("data").getAsJsonArray();

                    for ( JsonElement d : data )
                    {
                        JsonObject o = (JsonObject) d;

                        if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                        {
                            OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                            OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                    tempTime.getMonthValue(),tempTime.getDayOfMonth(), tempTime.getHour(), tempTime.getMinute(),0,0, ZoneOffset.UTC);

                            timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                    o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                        }
                    }
                }
            }
        }
        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> getMeterFeedinSpontanValuesFromNetzBurgenland(MeteringPoint meteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        if ( from.toEpochSecond() < to.minusMonths(5).toEpochSecond() )//max 1 month
            from = to.minusMonths(1);

        String fromStr = from.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));//always get a week before
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/date?end="+toStr+"%2B01:00&meteringPointIdentifier="+meteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        if (response.statusCode() == 200)
        {
            Gson gson = new Gson();

            var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

            String datapointname = meteringPoint.getDatapoints().get(0);

            for ( JsonElement jsonElement : jsonArray )
            {
                JsonObject jsonObject = (JsonObject) jsonElement;

                if ( jsonObject.get("name").getAsString().equals(datapointname) || jsonObject.get("name").getAsString().equals("Gesamteinspeisung"))
                {
                    JsonArray data = jsonObject.get("data").getAsJsonArray();

                    for ( JsonElement d : data )
                    {
                        JsonObject o = (JsonObject) d;

                        if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                        {
                            OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                            OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                    tempTime.getMonthValue(),tempTime.getDayOfMonth(), tempTime.getHour(), tempTime.getMinute(),0,0, ZoneOffset.UTC);

                            timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                    o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meteringPoint.getType().ordinal()));
                        }
                    }
                }
            }
        }
        return timeValueObjects;
    }

    @Override
    public void run()
    {
        System.out.println("Start Netzburgenland Collector for Provideraccount: "+providerAccount);

        while(true)//start workerthread
        {
            try
            {
                login();

                FppssRestConnector fppssRestConnector = new FppssRestConnector(System.getenv("FPPSS_REST_URL"));

                //////////////
                //consumption
                //////////////
                ArrayList<MeteringPoint> consumptionMeteringPoints = getConsumptionMeteringPoints();

                for (var meteringPoint : consumptionMeteringPoints)
                {
                    if (meteringPoint.getMeteringPointType() == MeteringPoint.MeteringPointType.AccountingPoint)//consumption
                    {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterConsumptionYearValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterConsumptionMonthValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterConsumptionDayValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.day, dayValues);

                        //hour values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.hour, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject>spontanValues = getMeterConsumptionSpontanValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        ArrayList<TimeValueObject> hourValues = buildHourValuesFromSpontanValues(meteringPoint, spontanValues);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.hour, hourValues);

                        //spontan values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.spontan, providerAccount.getProviderAccountId());
                        spontanValues = getMeterConsumptionSpontanValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.spontan, spontanValues);
                    }
                }

                //////////////
                //feedin
                //////////////
                ArrayList<MeteringPoint> feedinMeteringPoints  = getFeedInMeteringPoints();

                for (var meteringPoint : feedinMeteringPoints)
                {
                    if (meteringPoint.getMeteringPointType() == MeteringPoint.MeteringPointType.AccountingPoint)//consumption
                    {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterFeedinYearValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterFeedinMonthValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterFeedinDayValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.day, dayValues);

                        //hour values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.hour, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject>spontanValues = getMeterFeedinSpontanValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        ArrayList<TimeValueObject> hourValues = buildHourValuesFromSpontanValues(meteringPoint, spontanValues);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.hour, hourValues);

                        //spontan values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.spontan, providerAccount.getProviderAccountId());
                        spontanValues = getMeterFeedinSpontanValuesFromNetzBurgenland(meteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.spontan, spontanValues);
                    }
                }

                logoff();
            }
            catch (Exception e)
            {
                try
                {
                    logoff();
                }
                catch (Exception ex)
                {
                    //do not show error
                }
                e.printStackTrace();
            }

            try
            {
                Thread.sleep(interval*1000);
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
}
