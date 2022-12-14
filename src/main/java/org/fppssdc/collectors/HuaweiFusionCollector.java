/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fppssdc.connectors.FppssRestConnector;
import org.fppssdc.model.MeteringPoint;
import org.fppssdc.model.ProviderAccountObject;
import org.fppssdc.model.TimeValueObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class HuaweiFusionCollector extends Collector
{
    private HttpClient client = null;

    /**
     * Constructor
     */
    public HuaweiFusionCollector(ProviderAccountObject providerAccount, Integer interval)
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

    void logoff() throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/unisess/v1/logout"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                //.POST(HttpRequest.BodyPublishers.ofString(body))
                .GET()
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    void login() throws Exception
    {
        String body = "{ "+
                "\"organizationName\":\"\", "+
                "\"verifycode\": null, "+
                "\"multiRegionName\": null, "+
                "\"username\": \""+providerAccount.getProviderAccountUsername()+"\", "+
                "\"password\": \""+providerAccount.getDecryptedPw()+"\" }";

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
            Gson gson = new Gson();
            var resp = gson.fromJson((String)response.body(), JsonObject.class);

            if ( resp.get("errorCode") != null && !resp.get("errorCode").isJsonNull() && !resp.get("errorCode").equals("null") )
                throw new Exception("Login error");
        }
    }

    private HttpResponse execHttpRequest(HttpRequest request) throws Exception
    {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int cnt = 0;

        while(response.statusCode() != 200 && cnt < 30)//redirect to target url
        {
            request = HttpRequest.newBuilder()
                    .uri(response.uri())
                    .header("User-Agent", "okhttp/3.10.0")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            cnt++;
        }



        return response;
    }

    private ArrayList<TimeValueObject> getMeterProductionYearValuesFromHuawei(MeteringPoint meteringPoint, OffsetDateTime from) throws Exception
    {

        if ( from.toEpochSecond() < OffsetDateTime.parse("2021-12-31T23:00:00Z").toEpochSecond() )
            from = OffsetDateTime.parse("2021-12-31T23:00:00Z");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/"+
                        "overview/energy-balance?stationDn="+ URLEncoder.encode(meteringPoint.getId())+"&timeDim=6&queryTime="+from.toEpochSecond()+"000&timeZone=1&timeZoneStr=Europe%2FVienna&_="+OffsetDateTime.now().toEpochSecond()+"000"))
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

            String datapointname = meteringPoint.getDatapoints().get(0);

            for ( int i = 0; i < years.size(); i++ )
            {
                OffsetDateTime dateTime = OffsetDateTime.parse(years.get(i).getAsString()+"-01-01T00:00:00Z");

                BigDecimal value = new BigDecimal(0);

                if ( !producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--") )
                    value = producedPower.get(i).getAsBigDecimal();

                timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                        value, new BigDecimal(0), meteringPoint.getType().ordinal()));
            }

            return timeValueObjects;
        }

        return null;
    }

    private ArrayList<TimeValueObject> getMeterProductionHourValuesFromHuawei(MeteringPoint meteringPoint, OffsetDateTime from) throws Exception
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

                String datapointname = meteringPoint.getDatapoints().get(0);

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

                    timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusDays(1);
        }

        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> getMeterProductionMonthValuesFromHuawei(MeteringPoint meteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        while ( from.getYear() <= OffsetDateTime.now().getYear() )
        {
            /*if (from.toEpochSecond() < OffsetDateTime.parse("2021-12-31T23:00:00Z").toEpochSecond())
                from = OffsetDateTime.parse("2021-12-31T23:00:00Z");*/

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meteringPoint.getId()) + "&timeDim=5&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
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

                String datapointname = meteringPoint.getDatapoints().get(0);

                for (int i = 0; i < months.size(); i++)
                {
                    OffsetDateTime dateTime = OffsetDateTime.parse(from.getYear() + "-" + months.get(i).getAsString() + "-01T00:00:00Z");

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusYears(1);
        }

        return timeValueObjects;
    }

    private ArrayList<TimeValueObject> getMeterProductionDayValuesFromHuawei(MeteringPoint meteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        from = OffsetDateTime.of(from.getYear(),from.getMonthValue(),1,0,0,0,0,ZoneOffset.UTC);//always whole month

        if (from.toEpochSecond() < OffsetDateTime.parse("2022-01-01T00:00:00Z").toEpochSecond())
            from = OffsetDateTime.parse("2022-01-01T00:00:00Z");

        while ( from.toEpochSecond() <= OffsetDateTime.now().toEpochSecond() )
        {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meteringPoint.getId()) + "&timeDim=4&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
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

                String datapointname = meteringPoint.getDatapoints().get(0);

                for (int i = 0; i < days.size(); i++)
                {
                    String month = String.valueOf(from.getMonthValue());

                    if ( month.length() == 1 )//add leading zero
                        month = "0"+month;

                    OffsetDateTime dateTime = OffsetDateTime.parse(from.getYear() + "-" + month + "-"+days.get(i).getAsString()+"T00:00:00Z");

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meteringPoint.getType().ordinal()));
                }

                //return timeValueObjects;
            }

            from = from.plusMonths(1);
        }

        return timeValueObjects;
    }



    private ArrayList<TimeValueObject> getMeterProductionSpontanValuesFromHuawei(MeteringPoint meteringPoint, OffsetDateTime from) throws Exception
    {
        ArrayList<TimeValueObject> timeValueObjects = new ArrayList<>();

        from = OffsetDateTime.of(from.getYear(),from.getMonthValue(),from.getDayOfMonth(),0,0,0,0,ZoneOffset.UTC);//always whole day

        if (from.toEpochSecond() < OffsetDateTime.parse("2022-01-01T00:00:00Z").toEpochSecond())
            from = OffsetDateTime.parse("2022-01-01T00:00:00Z");

        while ( from.toEpochSecond() <= OffsetDateTime.now().toEpochSecond() )
        {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/station/v1/" +
                            "overview/energy-balance?stationDn=" + URLEncoder.encode(meteringPoint.getId()) + "&timeDim=2&queryTime=" + from.toEpochSecond() + "000&timeZone=1&timeZoneStr=Europe%2FVienna&_=" + OffsetDateTime.now().toEpochSecond() + "000"))
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

                String datapointname = meteringPoint.getDatapoints().get(0);

                for (int i = 0; i < times.size(); i++)
                {
                    LocalDateTime ldt = LocalDateTime.parse(times.get(i).getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.GERMANY));

                    Instant instant = ldt.atZone(ZoneId.of("Europe/Paris")).toInstant();

                    OffsetDateTime dateTime = OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));

                    BigDecimal value = new BigDecimal(0);

                    if (!producedPower.get(i).getAsString().equals("") && !producedPower.get(i).getAsString().equals("--"))
                        value = producedPower.get(i).getAsBigDecimal();

                    if ( value.floatValue() == 0 )//do not write zero
                        continue;

                    timeValueObjects.add(new TimeValueObject(dateTime, meteringPoint.getId(), datapointname, providerAccount.getProviderAccountId(),
                            value, new BigDecimal(0), meteringPoint.getType().ordinal()));
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
                System.out.println("Get data for provider: "+providerAccount);
                Runtime.getRuntime().gc();//call garbage colletor

                login();

                String csrToken = getCsrToken();

                String deviceId = getDeviceId(csrToken);

                FppssRestConnector fppssRestConnector = new FppssRestConnector(System.getenv("FPPSS_REST_URL"));

                ArrayList<MeteringPoint> meteringPoints = new ArrayList<>();
                var datapoints = new ArrayList<String>();
                datapoints.add("productPower");
                meteringPoints.add(new MeteringPoint(deviceId, MeteringPoint.Type.Production, MeteringPoint.MeteringPointType.AccountingPoint, datapoints));

                for (var meteringPoint : meteringPoints)
                {
                        //year values
                        OffsetDateTime lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.year, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> yearValues = getMeterProductionYearValuesFromHuawei(meteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.year, yearValues);

                        //month values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.month, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> monthValues = getMeterProductionMonthValuesFromHuawei(meteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.month, monthValues);

                        //day values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.day, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> dayValues = getMeterProductionDayValuesFromHuawei(meteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.day, dayValues);

                        //hour values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.hour, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject> hourValues = getMeterProductionHourValuesFromHuawei(meteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.hour, hourValues);

                        //spontan values
                        lastTimestamp = fppssRestConnector.getMeterLastTimestamp(meteringPoint, TimeValueObject.Resolution.spontan, providerAccount.getProviderAccountId());
                        ArrayList<TimeValueObject>spontanValues = getMeterProductionSpontanValuesFromHuawei(meteringPoint, lastTimestamp);
                        fppssRestConnector.saveMeterValuesInDatabase(meteringPoint, TimeValueObject.Resolution.spontan, spontanValues);


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
}
