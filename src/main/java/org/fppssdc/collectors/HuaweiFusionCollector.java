/*
 * Copyright (c) 2022.
 * FPPSS (Fleischhacker, Pilwax, Premauer, Schmit & Stadler)
 */

package org.fppssdc.collectors;

import org.fppssdc.model.ProviderAccountObject;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class HuaweiFusionCollector extends Collector
{
    static final String apiUrl = "https://smartmeter.netzburgenland.at/enView.Portal/api";

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
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        System.out.println(body);
        /*var response1 = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response1.body());
        if ( response1.statusCode() != 200 || ((String)response1.body()).contains("loginBtn") )//error while login
            throw new Exception("Login error");

        if ( response1.statusCode() == 200 )
        {
            Gson gson = new Gson();
            var resp = gson.fromJson((String)response1.body(), JsonObject.class);

            System.out.println(resp.get("errorCode"));
        }

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://region01eu5.fusionsolar.huawei.com/rest/pvms/web/device/v1/device-history-data?signalIds=30014&signalIds=30017&deviceDn=NE%3D35257139&date=1662412089463&_=1662404911145"))
                .header("User-Agent", "okhttp/3.10.0")
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();*/

        /*try
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

    @Override
    public void run()
    {
        System.out.println("Start Huawei-Fusion Collector for Provideraccount: "+providerAccount);

        while(true)//start workerthread
        {
            try
            {
                login();


                //System.out.println(ProviderAccountObject.decryptPw(s));

                //https://eu5.fusionsolar.huawei.com/unisso/login.action?decision=1&service=https://region01eu5.fusionsolar.huawei.com/unisess/v1/auth?service=%2Fnetecowebext%2Fhome%2Findex.html%23%2FLOGIN
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
