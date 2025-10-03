package nav;

import skynav.integration.NavIntegration;
import skynav.integration.nav.SkyMobilePort;

public class Navision {
    private static SkyMobilePort port = null;

    public static SkyMobilePort getPort(){
        return getPort(false);
    }

    public static SkyMobilePort getPort(boolean reconnect){
        try {
            if(port == null || reconnect){
                //Read conf file and get connection
                NavisionLocalParams params = NavisionUtils.getNavisionLocalParameters();

                if(params == null){
                    throw new Exception("Error getting parameters to connect to Navision");
                }

                //Establish connection by building a new port
                String strWS_URL = params.getCoreBankingUrl();
                String strUsername = params.getCoreBankingUsername();
                String strPassword = params.getCoreBankingPassword();
                String strNamespaceUrl = params.getCoreBankingNamespaceUrl();
                String strLocalPart = params.getCoreBankingLocalPort();

                NavIntegration integration = new NavIntegration(strWS_URL, strUsername, strPassword,
                        strNamespaceUrl, strLocalPart);
                port = integration.getSkyMobilePort();
            }
        } catch (Exception e) {
            System.err.println("Navision.getPort(boolean): Error getting SkyMobilePort ("+e.getMessage()+")");
        }
        return port;
    }

    /*public static SkyMobilePort getPort(boolean reconnect){

        try {
            if(port == null || reconnect){
                //Establish connection by building a new port
                String strWS_URL = "http://ucscs17.ukulima.fosa:9052/UkulimaDBLive/WS/UKULIMA%20SACCO%20SOCIETY%20LTD/Codeunit/SkyMobile";
                String strUsername = "ukulima.fosa\\skyworld";
                String strPassword = "";
                String strNamespaceUrl = "urn:microsoft-dynamics-schemas/codeunit/SkyMobile";
                String strLocalPart = "SkyMobile";

                NavIntegration integration = new NavIntegration(strWS_URL, strUsername, strPassword,
                        strNamespaceUrl, strLocalPart);
                port = integration.getSkyMobilePort();
            }
        } catch (Exception e) {
            System.err.println("Navision.getPort(boolean): Error getting SkyMobilePort ("+e.getMessage()+")");
        }
        return port;
    }*/
}