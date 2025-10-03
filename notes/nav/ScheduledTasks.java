package nav;

import ke.skyworld.mbanking.ussdapi.APIUtils;
import utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTasks {

    public static void startNavTransactionPoster(long intervalPeriod){
        try{
            ScheduledExecutorService service = Executors
                    .newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(() -> {
                try {
                    APIUtils.hashPINsOnNAV();

                    //System.out.println("Started ScheduledTasks.NavTransactionsPoster: WITHDRAWALS");
                    double lnStartTime = (double) System.currentTimeMillis();
                    Navision.getPort().callServiceFunction(1); //1 - WITHDRAWALS
                    double lnEndTime = (double) System.currentTimeMillis();
                    double lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                    String strFormatedTime = Utils.formatDouble(lnTimeTaken, "#,###.##");
                    //System.out.println("Finished ScheduledTasks.NavTransactionsPoster: WITHDRAWALS in "+lnTimeTaken+" Seconds");
                    Thread.sleep(intervalPeriod*1000);

                    //System.out.println("Started ScheduledTasks.NavTransactionsPoster: MOBILE LOAN");
                    lnStartTime = (double) System.currentTimeMillis();
                    Navision.getPort().callServiceFunction(2); //2 - MOBILE LOAN
                    lnEndTime = (double) System.currentTimeMillis();
                    lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                    strFormatedTime = Utils.formatDouble(lnTimeTaken, "#,###.##");
                    //System.out.println("Finished ScheduledTasks.NavTransactionsPoster: MOBILE LOAN in "+lnTimeTaken+" Seconds");
                    Thread.sleep(intervalPeriod*1000);

                    //System.out.println("Started ScheduledTasks.NavTransactionsPoster: SMS CHARGES");
                    lnStartTime = (double) System.currentTimeMillis();
                    Navision.getPort().callServiceFunction(3); //3 - SMS CHARGES
                    lnEndTime = (double) System.currentTimeMillis();
                    lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                    strFormatedTime = Utils.formatDouble(lnTimeTaken, "#,###.##");
                    //System.out.println("Finished ScheduledTasks.NavTransactionsPoster: SMS CHARGES in "+lnTimeTaken+" Seconds");
                    Thread.sleep(intervalPeriod * 1000);

                    //System.out.println("Started ScheduledTasks.NavTransactionsPoster: ATM CHARGES");
                    lnStartTime = (double) System.currentTimeMillis();
                    Navision.getPort().callServiceFunction(4); //4 - ATM
                    lnEndTime = (double) System.currentTimeMillis();
                    lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                    strFormatedTime = Utils.formatDouble(lnTimeTaken, "#,###.##");
                    //System.out.println("Finished ScheduledTasks.NavTransactionsPoster: ATM CHARGES in "+lnTimeTaken+" Seconds");
                    Thread.sleep(intervalPeriod * 1000);
                } catch (Exception e) {
                    //TODO:RETURN THIS ON PRODUCTION ENVIRONMENT
                    //System.err.println("ScheduledTasks.NavTransactionsPoster() Error: " + e.getMessage());
                }
            }, 0, intervalPeriod, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("ScheduledTasks.startNavTransactionPoster() Error: " + e.getMessage());
        }
    }

    public static void startNavLinkHealthChecker(long intervalPeriod){
        try{
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(() -> {
                try {
                    System.out.println("Started ScheduledTasks.NavLinkHealthChecker");

                    //Check if Nav link is set
                    if(Navision.getPort() != null) {
                        try{
                            String healthCheckResponse = Navision.getPort().checkLinkHealth();

                            if(healthCheckResponse.equalsIgnoreCase("OK"))
                                System.out.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                                        " Navision link is healthy. Waiting for "+intervalPeriod+" seconds before checking again");
                            else
                                reconnectToNavision();

                        } catch (Exception e){
                            reconnectToNavision();
                        }
                    }else{
                        System.err.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                                " Navision link is not connected. Connecting...");
                        //Connect to Navision
                        reconnectToNavision();
                    }
                } catch (Exception e) {
                    System.err.println("ScheduledTasks.NavLinkHealthChecker() Error: " + e.getMessage());
                }
            }, 0, intervalPeriod, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("ScheduledTasks.startNavLinkHealthChecker() Error: " + e.getMessage());
        }
    }

    private static void reconnectToNavision(){
        try {

            System.out.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                    " Navision connection attempted. Checking connection health...");

            //Reconnect to repair link
            Navision.getPort(true);
            String healthCheckResponse = Navision.getPort().checkLinkHealth();

            //wait for {reconnectionInterval} seconds and retry to connect is there was an issue
            //Thread.sleep(1000*reconnectionInterval);
            if(healthCheckResponse.equalsIgnoreCase("OK")){
                System.out.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                        " Navision link is healthy.");
            } else {
                System.err.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                        " Navision link is not healthy.");
            }
        } catch (Exception e){
            System.err.println("ScheduledTasks.startNavLinkHealthChecker() ->" +
                    " Navision link has an issue.");
        }
    }
}
