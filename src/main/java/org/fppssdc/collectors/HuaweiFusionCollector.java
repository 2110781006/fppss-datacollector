/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fppssdc.connectors.FppssRestConnector;
import org.fppssdc.model.MeeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

public class HuaweiFusionCollector extends Collector
{
    private HttpClient client = null;

    /**
     * Constructor
     */
    public HuaweiFusionCollector(ProviderAccountObject providerAccount)
    {
        super.providerAccount = providerAccount;

        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    void logout() throws Exception
    {
        /*String body = "{ "+
                "\"organizationName\":\"\", "+
                "\"verifycode\": null, "+
                "\"multiRegionName\": null, "+
                "\"username\": \""+providerAccount.getProviderAccountUsername()+"\", "+
                "\"password\": \""+providerAccount.getDecryptedPw()+"\" }";*/

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/unisess/v1/logout"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                //.POST(HttpRequest.BodyPublishers.ofString(body))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("#####"+response.statusCode());
        //System.out.println("#####"+response.body());

        /*if ( response.statusCode() != 200 || ((String)response.body()).contains("loginBtn") )//error while login
            throw new Exception("Login error");

        if ( response.statusCode() == 200 )
        {
            Gson gson = new Gson();
            var resp = gson.fromJson((String)response.body(), JsonObject.class);

            if ( resp.get("errorCode") != null && !resp.get("errorCode").isJsonNull() && !resp.get("errorCode").equals("null") )
                throw new Exception("Login error");
        }*/
    }

    void login() throws Exception
    {
        String body = "{ "+
                "\"organizationName\":\"\", "+
                "\"verifycode\": null, "+
                "\"multiRegionName\": null, "+
                "\"username\": \""+providerAccount.getProviderAccountUsername()+"\", "+
                "\"password\": \""+providerAccount.getDecryptedPw()+"\" }";

        System.out.println(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://eu5.fusionsolar.huawei.com/unisso/v2/validateUser.action"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                //.header("Accept-Encoding", "gzip, deflate, br")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if ( response.statusCode() != 200 || ((String)response.body()).contains("loginBtn") )//error while login
            throw new Exception("Login error");

        if ( response.statusCode() == 200 )
        {
            System.out.println(response.body());
            Gson gson = new Gson();
            var resp = gson.fromJson((String)response.body(), JsonObject.class);

            if ( resp.get("errorCode") != null && !resp.get("errorCode").isJsonNull() && !resp.get("errorCode").equals("null") )
                throw new Exception("Login error");
        }

        System.out.println(response.body());
    }

    private HttpResponse execHttpRequest(HttpRequest request) throws Exception
    {


        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        //response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.headers());
        //System.out.println(response.body());

        int cnt = 0;

        while(response.statusCode() != 200 && cnt < 30)//redirect to target url
        {
            System.out.println("------"+response.statusCode()+":::"+response.uri());
            request = HttpRequest.newBuilder()
                    .uri(response.uri())
                    .header("User-Agent", "okhttp/3.10.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //Thread.sleep(1000);
            System.out.println("+++++"+response.statusCode()+":::"+response.uri());
            cnt++;
        }

        System.out.println(response.statusCode());
        System.out.println(response.body());

        return response;
    }

    private ArrayList<TimeValueObject> getMeterProductionYearValuesFromHuawei(MeeteringPoint meeteringPoint, OffsetDateTime from) throws Exception
    {

        if ( from.toEpochSecond() < OffsetDateTime.parse("2021-12-31T23:00:00Z").toEpochSecond() )
            from = OffsetDateTime.parse("2021-12-31T23:00:00Z");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/"+
                        "overview/energy-balance?stationDn="+ URLEncoder.encode(meeteringPoint.getId())+"&timeDim=6&queryTime="+from.toEpochSecond()+"000&timeZone=1&timeZoneStr=Europe%2FVienna&_="+OffsetDateTime.now().toEpochSecond()+"000"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .GET()
                .build();

        HttpResponse response = execHttpRequest(request);

        if ( response.statusCode() == 200 )
        {
            Gson gson = new Gson();

            var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

            JsonObject data = (JsonObject) jsonObject.get("data");

            JsonArray years = data.getAsJsonArray("xAxis");
            JsonArray producedPower = data.getAsJsonArray("productPower");

            ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

            String datapointname = meeteringPoint.getDatapoints().get(0);

            for ( int i = 0; i < years.size(); i++ )
            {
                OffsetDateTime dateTime = OffsetDateTime.parse(years.get(i).getAsString()+"-01-01T00:00:00Z");

                BigDecimal value = new BigDecimal(0);

                if ( !producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--") )
                    value = producedPower.get(i).getAsBigDecimal();

                timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                        value, new BigDecimal(0), meeteringPoint.getType().ordinal()));
            }

            return timeValueObjects;
        }

        return null;
    }

    private ArrayList<TimeValueObject> getMeterProductionHourValuesFromHuawei(MeeteringPoint meeteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        String csrToken = getCsrToken();

        from = OffsetDateTime.of(from.getYear(),from.getMonthValue(),from.getDayOfMonth(),0,0,0,0,ZoneOffset.UTC);//always whole day


        while ( from.toEpochSecond() <= OffsetDateTime.now().toEpochSecond() )
        {
            if (from.toEpochSecond() < OffsetDateTime.parse("2021-12-31T23:00:00Z").toEpochSecond())
                from = OffsetDateTime.parse("2021-12-31T23:00:00Z");

            String body = "{\n" +
                    "    \"moList\": [],\n" +
                    "    \"counterIDs\": [\n" +
                    "        \"counter_10000005\"\n" +
                    "    ],\n" +
                    "    \"statDim\": \"2\",\n" +
                    "    \"statTime\": "+from.toEpochSecond()+"000,\n" +
                    "    \"statType\": 1,\n" +
                    "    \"timeZone\": 2,\n" +
                    "    \"timeZoneStr\": \"Europe/Vienna\"\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/report/v1/station/home-station-kpi-chart"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36 Edg/100.0.1185.29")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Accept-Language","en-US,en;q=0.9")
                    .header("Cache-Control","no-cache")
                    .header("Pragma","no-cache")
                    .header("roarand",csrToken)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse response = execHttpRequest(request);

            if (response.statusCode() == 200)
            {
                Gson gson = new Gson();

                var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

                JsonObject data1 = (JsonObject) jsonObject.get("data");
                JsonObject data2 = (JsonObject) data1.get("data");

                JsonArray hours = data2.getAsJsonArray("xAxis");

                JsonArray yAsix = data2.getAsJsonArray("yAxis");

                JsonArray producedPower = yAsix.get(0).getAsJsonArray();

                String datapointname = meeteringPoint.getDatapoints().get(0);

                for (int i = 0; i < hours.size(); i++)
                {
                    String month = String.valueOf(from.getMonthValue());
                    String day = String.valueOf(from.getDayOfMonth());

                    if ( month.length() == 1 )//add leading zero
                        month = "0"+month;

                    if ( day.length() == 1 )//add leading zero
                        day = "0"+day;

                    OffsetDateTime dateTimeLocal = OffsetDateTime.parse(from.getYear() + "-" + month + "-"+day+"T"+hours.get(i).getAsString().replace("\"","")+":00:00Z");

                    LocalDateTime ldt = LocalDateTime.parse(dateTimeLocal.toString().replace("T", " ").replace("Z",""), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.GERMANY));

                    Instant instant = ldt.atZone(ZoneId.of("Europe/Paris")).toInstant();

                    OffsetDateTime dateTime = OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));

                    //OffsetDateTime dateTimeLocal = OffsetDateTime.parse(from.getYear() + "-" + month + "-"+day+"T"+hours.get(i).getAsString().replace("\"","")+":00:00Z");

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--") &&
                            !producedPower.get(i).getAsString().equals("-"))
                        value = producedPower.get(i).getAsBigDecimal();

                    timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meeteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusDays(1);
        }

        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> getMeterProductionMonthValuesFromHuawei(MeeteringPoint meeteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        while ( from.getYear() <= OffsetDateTime.now().getYear() )
        {
            /*if (from.toEpochSecond() < OffsetDateTime.parse("2021-12-31T23:00:00Z").toEpochSecond())
                from = OffsetDateTime.parse("2021-12-31T23:00:00Z");*/

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meeteringPoint.getId()) + "&timeDim=5&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
                    .header("User-Agent", "okhttp/3.10.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            HttpResponse response = execHttpRequest(request);

            if (response.statusCode() == 200)
            {
                Gson gson = new Gson();

                var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

                JsonObject data = (JsonObject) jsonObject.get("data");

                JsonArray months = data.getAsJsonArray("xAxis");
                JsonArray producedPower = data.getAsJsonArray("productPower");

                String datapointname = meeteringPoint.getDatapoints().get(0);

                for (int i = 0; i < months.size(); i++)
                {
                    OffsetDateTime dateTime = OffsetDateTime.parse(from.getYear() + "-" + months.get(i).getAsString() + "-01T00:00:00Z");

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meeteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusYears(1);
        }

        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> getMeterProductionDayValuesFromHuawei(MeeteringPoint meeteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        from = OffsetDateTime.of(from.getYear(),from.getMonthValue(),1,0,0,0,0,ZoneOffset.UTC);//always whole month

        if (from.toEpochSecond() < OffsetDateTime.parse("2022-01-01T00:00:00Z").toEpochSecond())
            from = OffsetDateTime.parse("2022-01-01T00:00:00Z");

        while ( from.toEpochSecond() <= OffsetDateTime.now().toEpochSecond() )
        {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meeteringPoint.getId()) + "&timeDim=4&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
                    .header("User-Agent", "okhttp/3.10.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            HttpResponse response = execHttpRequest(request);

            if (response.statusCode() == 200)
            {
                Gson gson = new Gson();

                var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

                JsonObject data = (JsonObject) jsonObject.get("data");

                JsonArray days = data.getAsJsonArray("xAxis");
                JsonArray producedPower = data.getAsJsonArray("productPower");
                System.out.println("from.toEpochSecond()"+from.toEpochSecond()+days);
                String datapointname = meeteringPoint.getDatapoints().get(0);

                for (int i = 0; i < days.size(); i++)
                {
                    String month = String.valueOf(from.getMonthValue());

                    if ( month.length() == 1 )//add leading zero
                        month = "0"+month;

                    OffsetDateTime dateTime = OffsetDateTime.parse(from.getYear() + "-" + month + "-"+days.get(i).getAsString()+"T00:00:00Z");

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meeteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusMonths(1);
        }

        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> buildHourValuesFromSpontanValues(MeeteringPoint meeteringPoint, ArrayList<TimeValueObject> spontanValues)
    {
        HashMap<OffsetDateTime, ArrayList<Float>> hourValuesMap = new HashMap<>();

        ArrayList<TimeValueObject> hourValues = new ArrayList<>();

        for ( var spontanValue : spontanValues )
        {
            OffsetDateTime hourTimestamp = OffsetDateTime.of(spontanValue.getTimestamp().getYear(),
                    spontanValue.getTimestamp().getMonthValue(),spontanValue.getTimestamp().getDayOfMonth(),
                    spontanValue.getTimestamp().getHour(),0,0,0, ZoneOffset.UTC);

            if ( !hourValuesMap.containsKey(hourTimestamp) )
                hourValuesMap.put(hourTimestamp, new ArrayList<Float>());

            hourValuesMap.get(hourTimestamp).add(spontanValue.getValue().floatValue());
        }

        for ( var hour : hourValuesMap.keySet() )
        {
            ArrayList<Float> values = hourValuesMap.get(hour);
            BigDecimal sum = new BigDecimal(values.stream().collect(Collectors.summingDouble(d->d)));

            hourValues.add(new TimeValueObject(hour, meeteringPoint.getId(), meeteringPoint.getDatapoints().get(0), providerAccount.getProviderAccountId(),
                    sum, new BigDecimal(0), meeteringPoint.getType().ordinal()));
        }

        return hourValues;
    }

    private ArrayList<TimeValueObject> getMeterProductionSpontanValuesFromHuawei(MeeteringPoint meeteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        from = OffsetDateTime.of(from.getYear(),from.getMonthValue(),from.getDayOfMonth(),0,0,0,0,ZoneOffset.UTC);//always whole day

        if (from.toEpochSecond() < OffsetDateTime.parse("2022-01-01T00:00:00Z").toEpochSecond())
            from = OffsetDateTime.parse("2022-01-01T00:00:00Z");

        while ( from.toEpochSecond() <= OffsetDateTime.now().toEpochSecond() )
        {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meeteringPoint.getId()) + "&timeDim=2&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
                    .header("User-Agent", "okhttp/3.10.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .GET()
                    .build();

            HttpResponse response = execHttpRequest(request);

            if (response.statusCode() == 200)
            {
                Gson gson = new Gson();

                var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

                JsonObject data = (JsonObject) jsonObject.get("data");

                JsonArray times = data.getAsJsonArray("xAxis");
                JsonArray producedPower = data.getAsJsonArray("productPower");

                String datapointname = meeteringPoint.getDatapoints().get(0);

                for (int i = 0; i < times.size(); i++)
                {
                    LocalDateTime ldt = LocalDateTime.parse(times.get(i).getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.GERMANY));

                    Instant instant = ldt.atZone(ZoneId.of("Europe/Paris")).toInstant();

                    OffsetDateTime dateTime = OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    if ( value.intValue() == 0 )//do not write zero
                        continue;

                    timeValueObjects.add(new TimeValueObject(dateTime, meeteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meeteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusDays(1);
        }

        return timeValueObjects;
    }

    private String getCsrToken() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/unisess/v1/auth/session"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .GET()
                .build();

        HttpResponse response = execHttpRequest(request);

        if (response.statusCode() == 200)
        {
            Gson gson = new Gson();

            var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

            return jsonObject.get("csrfToken").getAsString();
        }

        return null;
    }

    private String getDeviceId(String csrToken) throws Exception
    {
        String body = "{\n" +
                "    \"curPage\": 1,\n" +
                "    \"pageSize\": 10,\n" +
                "    \"gridConnectedTime\": \"\",\n" +
                "    \"queryTime\": "+LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)+",\n" +
                "    \"timeZone\": 2,\n" +
                "    \"sortId\": \"createTime\",\n" +
                "    \"sortDir\": \"DESC\",\n" +
                "    \"locale\": \"en_US\"\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/station/station-list"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.60 Safari/537.36 Edg/100.0.1185.29")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language","en-US,en;q=0.9")
                .header("Cache-Control","no-cache")
                .header("Pragma","no-cache")
                .header("roarand",csrToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse response =execHttpRequest(request);

        if (response.statusCode() == 200)
        {
            Gson gson = new Gson();

            var jsonObject = gson.fromJson((String) response.body(), JsonObject.class);

            JsonObject data = (JsonObject) jsonObject.get("data");

            JsonArray list = data.getAsJsonArray("list");
            JsonObject device = (JsonObject) list.get(0);

            return device.get("dn").getAsString();
        }

        return null;
    }

    @Override
    public void run()
    {
        System.out.println("Start Huawei-Fusion Collector for Provideraccount: "+providerAccount);

        while(true)//start workerthread
        {
            try
            {
                login();

                String csrToken = getCsrToken();

                String deviceId = getDeviceId(csrToken);

                FppssRestConnector fppssRestConnector = new FppssRestConnector(System.getenv("FPPSS_REST_URL"));

                ArrayList<MeeteringPoint> meeteringPoints = new ArrayList<>();
                var datapoints = new ArrayList<String>();
                datapoints.add("productPower");
                meeteringPoints.add(new MeeteringPoint(deviceId, MeeteringPoint.Type.Production, MeeteringPoint.MeeteringPointType.AccountingPoint, datapoints));

                for (var meeteringPoint : meeteringPoints)
                {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterProductionYearValuesFromHuawei(meeteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterProductionMonthValuesFromHuawei(meeteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterProductionDayValuesFromHuawei(meeteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.day, dayValues);

                        //hour values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.hour, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> hourValues = getMeterProductionHourValuesFromHuawei(meeteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.hour, hourValues);

                        //spontan values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meeteringPoint, TimeValueObject.Resolution.spontan, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject>spontanValues = getMeterProductionSpontanValuesFromHuawei(meeteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meeteringPoint, TimeValueObject.Resolution.spontan, spontanValues);


                }

                Thread.sleep(2000);
                logout();
            }
            catch (Exception e)
            {
                try
                {
                    //logoff();
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
}
