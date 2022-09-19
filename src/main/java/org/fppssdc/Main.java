package org.fppssdc;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main
{
    public static void main(String[] args)
    {
        System.out.println("start");

        NetzBurgenlandCollector netzBurgenlandCollector = new NetzBurgenlandCollector("xxx", "xxx");

        new Thread(netzBurgenlandCollector).start();

        /*try
        {
            netzBurgenlandCollector.run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/
        System.out.println("after");

        /*HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        String body = "{ "+
                        "\"organizationName\":\"\", "+
                        "\"verifycode\": null, "+
                        "\"multiRegionName\": null, "+
                        "\"username\": \"mael.schm%40scht-it.com\", "+
                        "\"password\": \"xxx#\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://eu5.fusionsolar.huawei.com/unisso/v2/validateUser.action"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try
        {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.statusCode());
            System.out.println(response.body());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/device/v1/device-history-data?signalIds=30014&signalIds=30017&deviceDn=NE%3D35257139&date=1662412089463&_=1662404911145"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        try
        {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.statusCode());
            System.out.println(response.body());

            while(response.statusCode() >= 300 && response.statusCode() < 400)
            {
                request = HttpRequest.newBuilder()
                        .uri(response.uri())
                        .header("User-Agent", "okhttp/3.10.0")
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            System.out.println(response.statusCode());
            System.out.println(response.body());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/
    }
}
