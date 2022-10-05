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
import org.fppssdc.model.MeeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class NetzBurgenlandCollector extends Collector
{
    static final String apiUrl = "https://smartmeter.netzburgenland.at/enView.Portal/api";

    private HttpClient client = null;

    /**
     * Constructor
     */
    public NetzBurgenlandCollector(ProviderAccountObject providerAccount)
    {
        super.providerAccount = providerAccount;

        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /**
     * Get consumption meeterpoints
     * @return
     * @throws Exception
     */
    private ArrayList<MeeteringPoint> getConsumptionMeeteringPoints() throws Exception
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

        ArrayList<MeeteringPoint> meeteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeeteringPoint.MeeteringPointType type= MeeteringPoint.MeeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeeteringPoint.MeeteringPointType.AccountingPoint;

            var datapoints = new ArrayList<String>();

            datapoints.add("1.8.1");//consumption dp

            meeteringPoints.add(new MeeteringPoint(e.get("identifier").toString().replace("\"", ""), MeeteringPoint.Type.Consumption, type, datapoints,false));
        }

        return meeteringPoints;
    }

    /**
     * Get feedin meeterpoints
     * @return
     * @throws Exception
     */
    private ArrayList<MeeteringPoint> getFeedInMeeteringPoints() throws Exception
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

        ArrayList<MeeteringPoint> meeteringPoints = new ArrayList<>();

        JsonArray jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        for ( JsonElement o : jsonArray)
        {
            JsonObject e = o.getAsJsonObject();

            MeeteringPoint.MeeteringPointType type= MeeteringPoint.MeeteringPointType.AccountingPoint;

            if ( e.get("meteringPointType").equals("AccountingPoint") )
                type = MeeteringPoint.MeeteringPointType.AccountingPoint;

            var datapoints = new ArrayList<String>();

            datapoints.add("2.8.1");//consumption dp

            meeteringPoints.add(new MeeteringPoint(e.get("identifier").toString().replace("\"", ""), MeeteringPoint.Type.Feedin, type, datapoints, true));
        }

        return meeteringPoints;
    }

    /**
     * Get consumption meter day values
     * @param meeteringPoint
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterConsumptionDayValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/month?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
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

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get feedin meeter day values
     * @param meeteringPoint
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterFeedinDayValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/month?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
                .header("User-Agent", "PostmanRuntime/7.29.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate, br")
                .GET()
                .build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        var jsonArray = gson.fromJson((String) response.body(), JsonArray.class);

        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
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

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    public ArrayList<TimeValueObject> getMeterConsumptionMonthValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/year?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
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

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    public ArrayList<TimeValueObject> getMeterFeedinMonthValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/year?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
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

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime tempTime = OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"",""));
                        OffsetDateTime dateTime = OffsetDateTime.of(tempTime.getYear(),
                                tempTime.getMonthValue(),1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get year values of given timerange and meeteringpoint
     * @param meeteringPoint
     * @param from
     * @param to
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterConsumptionYearValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-01-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/consumption/overview?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
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

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime dateTime = OffsetDateTime.of(OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).getYear(),
                                1,1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
                    }
                }
            }
        }

        return timeValueObjects;
    }

    /**
     * Get year feedin values of given timerange and meeteringpoint
     * @param meeteringPoint
     * @param from
     * @param to
     * @throws Exception
     */
    public ArrayList<TimeValueObject> getMeterFeedinYearValuesFromNetzBurgenland(MeeteringPoint meeteringPoint, OffsetDateTime from, OffsetDateTime to) throws Exception
    {
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-01-01'T'00:00:00"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+"/feedin/overview?end="+toStr+"%2B01:00&meteringPointIdentifier="+meeteringPoint.getId()+"&start="+fromStr+"%2B01:00"))
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

        String datapointname = meeteringPoint.getDatapoints().get(0);

        for ( JsonElement jsonElement : jsonArray )
        {
            JsonObject jsonObject = (JsonObject) jsonElement;

            if ( jsonObject.get("name").getAsString().equals(datapointname) )
            {
                JsonArray data = jsonObject.get("data").getAsJsonArray();

                for ( JsonElement d : data )
                {
                    JsonObject o = (JsonObject) d;

                    if ( o.get("value") != null && o.get("reading") != null )//full qualified value
                    {
                        OffsetDateTime dateTime = OffsetDateTime.of(OffsetDateTime.parse(o.get("endTimestamp").getAsString().replace("\"","")).getYear(),
                                1,1,0,0,0,0, ZoneOffset.UTC);

                        timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                                o.get("value").getAsBigDecimal(), o.get("reading").getAsBigDecimal(), meeteringPoint.isFeedin()));
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
                ArrayList<MeeteringPoint> consumptionMeeteringPoints = getConsumptionMeeteringPoints();

                for (var meeteringPoint : consumptionMeeteringPoints)
                {
                    if (meeteringPoint.getMeeteringPointType() == MeeteringPoint.MeeteringPointType.AccountingPoint)//consumption
                    {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterConsumptionYearValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterConsumptionMonthValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterConsumptionDayValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.day, dayValues);
                    }
                }

                //////////////
                //feedin
                //////////////
                ArrayList<MeeteringPoint> feedinMeeteringPoints  = getFeedInMeeteringPoints();

                for (var meeteringPoint : feedinMeeteringPoints)
                {
                    if (meeteringPoint.getMeeteringPointType() == MeeteringPoint.MeeteringPointType.AccountingPoint)//consumption
                    {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterFeedinYearValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterFeedinMonthValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterFeedinDayValuesFromNetzBurgenland(meeteringPoint, lastTimestamp, OffsetDateTime.now());
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.day, dayValues);
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
}
