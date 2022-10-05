package org.fppssdc;

import org.fppssdc.collectors.Collector;
import org.fppssdc.collectors.HuaweiFusionCollector;
import org.fppssdc.collectors.NetzBurgenlandCollector;
import org.fppssdc.connectors.FppssRestConnector;
import org.fppssdc.model.ProviderAccountObject;

import java.util.HashMap;
import java.util.Properties;

/**
 * Main class
 */
public class Main
{
    public static void main(String[] args)
    {
        Properties properties = System.getProperties();

        System.out.println("Start Application");
        System.out.println("Java version: "+properties.getProperty("java.version"));
        System.out.println("Java runtime: "+properties.getProperty("java.runtime.name"));
        System.out.println("Java vm name: "+properties.getProperty("java.vm.name"));
        System.out.println("User path: "+properties.getProperty("user.dir"));
        System.out.println("OS name: "+properties.getProperty("os.name"));
        System.out.println("OS version: "+properties.getProperty("os.version"));
        System.out.println("FPPSS_REST_URL: "+System.getenv("FPPSS_REST_URL"));

        HashMap<Integer, Thread> startedCollectors = new HashMap<Integer, Thread>();

        while(true)//start provider accounts loop
        {
            try
            {
                FppssRestConnector fppssRestConnector = new FppssRestConnector(System.getenv("FPPSS_REST_URL"));

                ProviderAccountObject[] providerAccounts = fppssRestConnector.getProviderAccounts();

                for (var providerAccount : providerAccounts)//start collectors for provider accounts
                {
                    if (providerAccount.getProviderName().equals("huf"))//Huawei Fusion Inverter
                    {
                        if ( !startedCollectors.containsKey(providerAccount.getProviderAccountId()) )//start collector if not running
                        {
                            HuaweiFusionCollector huaweiFusionCollector = new HuaweiFusionCollector(providerAccount);

                            Thread huaweiFusionCollectorThread = new Thread(huaweiFusionCollector);

                            huaweiFusionCollectorThread.start();

                            startedCollectors.put(providerAccount.getProviderAccountId(), huaweiFusionCollectorThread);
                        }
                    }
                    else if (providerAccount.getProviderName().equals("nb"))//Netz Burgenland
                    {
                        if ( !startedCollectors.containsKey(providerAccount.getProviderAccountId()) )//start collector if not running
                        {
                            NetzBurgenlandCollector netzBurgenlandCollector = new NetzBurgenlandCollector(providerAccount);

                            Thread netzBurgenlandCollectorThread = new Thread(netzBurgenlandCollector);

                            netzBurgenlandCollectorThread.start();

                            startedCollectors.put(providerAccount.getProviderAccountId(), netzBurgenlandCollectorThread);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                new Exception("Could not get provider accounts").printStackTrace();
            }
            finally
            {
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

        /*NetzBurgenlandCollector netzBurgenlandCollector = new NetzBurgenlandCollector("xxx", "xxx");

        new Thread(netzBurgenlandCollector).start();*/

        /*try
        {
            netzBurgenlandCollector.run();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/


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
