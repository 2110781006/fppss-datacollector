package org.fppssdc;

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
        properties.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        Integer interval = 60 * 5;//5 minutes

        if ( System.getenv("FPPSS_INTERVAL") != null && !System.getenv("FPPSS_INTERVAL").isEmpty() && !System.getenv("FPPSS_INTERVAL").equals("") )
            interval = Integer.parseInt(System.getenv("FPPSS_INTERVAL"));


        System.out.println("Start Application");
        System.out.println("Java version: "+properties.getProperty("java.version"));
        System.out.println("Java runtime: "+properties.getProperty("java.runtime.name"));
        System.out.println("Java vm name: "+properties.getProperty("java.vm.name"));
        System.out.println("User path: "+properties.getProperty("user.dir"));
        System.out.println("OS name: "+properties.getProperty("os.name"));
        System.out.println("OS version: "+properties.getProperty("os.version"));
        System.out.println("FPPSS_REST_URL: "+System.getenv("FPPSS_REST_URL"));
        System.out.println("Interval: "+interval+" seconds");

        HashMap<Integer, Thread> startedCollectors = new HashMap<>();

        while(true)//start provider accounts loop
        {
            try
            {
                System.out.println("Check if new provider");
                FppssRestConnector fppssRestConnector = new FppssRestConnector(System.getenv("FPPSS_REST_URL"));

                fppssRestConnector.login();

                ProviderAccountObject[] providerAccounts = fppssRestConnector.getProviderAccounts();

                for (var providerAccount : providerAccounts)//start collectors for provider accounts
                {
                    if (providerAccount.getProviderName().equals("huf"))//Huawei Fusion Inverter
                    {
                        if ( !startedCollectors.containsKey(providerAccount.getProviderAccountId()) )//start collector if not running
                        {
                            System.out.println("Start new HuawayFusionCollector: "+providerAccount);
                            HuaweiFusionCollector huaweiFusionCollector = new HuaweiFusionCollector(providerAccount, interval);

                            Thread huaweiFusionCollectorThread = new Thread(huaweiFusionCollector);

                            huaweiFusionCollectorThread.start();

                            startedCollectors.put(providerAccount.getProviderAccountId(), huaweiFusionCollectorThread);
                        }
                    }
                    else if (providerAccount.getProviderName().equals("nb"))//Netz Burgenland
                    {
                        if ( !startedCollectors.containsKey(providerAccount.getProviderAccountId()) )//start collector if not running
                        {
                            System.out.println("Start new NetzBurgenlandCollector: "+providerAccount);
                            NetzBurgenlandCollector netzBurgenlandCollector = new NetzBurgenlandCollector(providerAccount, interval);

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
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    Thread.sleep(60000);//check each hour if new provider
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
