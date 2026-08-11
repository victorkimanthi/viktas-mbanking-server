package ke.skyworld.mbanking.mbankingapi;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.msg.MSGLocalParameters;
import ke.skyworld.lib.mbanking.msg.MSGProcessor;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.UUID;

public class MBankingAPI {
    public void processOnStartup(){
        try {
            //todo - NAV SPECIFIC CODE
            //ScheduledTasks.startNavTransactionPoster(5);

            //Only Run Once for Security Purposes
            /*SPManagerInterface.updateHashes(PESALocalParameters.getIntegritySecret());
            System.exit(0);*/

            /*USSDAPIConstants.CheckUserReturnVal checkUserReturnVal = new USSDAPI().MOCheckUser(UUID.randomUUID().toString().toLowerCase(), "254713000249");
            System.out.println();
            System.out.println("MO Check User RVal - 254713000249:");
            System.out.println(checkUserReturnVal.getValue());
            System.out.println();

            checkUserReturnVal = new USSDAPI().MOCheckUser(UUID.randomUUID().toString().toLowerCase(), "25471111");
            System.out.println();
            System.out.println("MO Check User RVal - 25471111:");
            System.out.println(checkUserReturnVal.getValue());
            System.out.println();

            String balanceEnquiryMessage = new USSDAPI().MOAccountBalanceEnquiry(UUID.randomUUID().toString().toLowerCase(), UUID.randomUUID().toString().toLowerCase().substring(5), "254713000249");
            System.out.println();
            System.out.println("MO Balance Enquiry Message - 254713000249:");
            System.out.println(balanceEnquiryMessage);
            System.out.println();

            balanceEnquiryMessage = new USSDAPI().MOAccountBalanceEnquiry(UUID.randomUUID().toString().toLowerCase(), UUID.randomUUID().toString().toLowerCase().substring(5), "25471111");
            System.out.println();
            System.out.println("MO Balance Enquiry Message - 25471111:");
            System.out.println(balanceEnquiryMessage);
            System.out.println();*/

            /// MAPP SIMULATION
//            MAPPAPI.MAPPRequestSimulation();

        } catch (Exception e){
            System.err.println("MBankingAPI.processOnStartup Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processOnDBReconnect() {
        try {
            System.out.println("RUNNING MBANKING API NAV RECONNECT FUNCTIONS");
            //todo - NAV SPECIFIC CODE
            //ScheduledTasks.startNavLinkHealthChecker(180);
        } catch (Exception e) {
            System.err.println("MBankingAPI.processOnDBReconnect Error: " + e.getMessage());
        }
    }

    public static String getValueFromLocalParams(MBankingConstants.ApplicationType theApplicationType, String thePath) {
        String rVal = "";
        try {
            String strConfigXML = "";
            if (theApplicationType == MBankingConstants.ApplicationType.PESA) {
                strConfigXML = PESALocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.MSG) {
                strConfigXML = MSGLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.MAPP) {
                strConfigXML = MAPPLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.USSD) {
                strConfigXML = USSDLocalParameters.getClientXMLParameters();
            }

            InputSource source = new InputSource(new StringReader(strConfigXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            rVal = configXPath.evaluate(thePath, xmlDocument, XPathConstants.STRING).toString();
        } catch (Exception e) {
            System.err.println("PESADB.getValueFromLocalParams() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static NodeList getNodeListFromLocalParams(MBankingConstants.ApplicationType theApplicationType, String thePath) {
        NodeList rVal = null;
        try {
            String strConfigXML = "";
            if (theApplicationType == MBankingConstants.ApplicationType.PESA) {
                strConfigXML = PESALocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.MSG) {
                strConfigXML = MSGLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.MAPP) {
                strConfigXML = MAPPLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == MBankingConstants.ApplicationType.USSD) {
                strConfigXML = USSDLocalParameters.getClientXMLParameters();
            }

            InputSource source = new InputSource(new StringReader(strConfigXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            rVal = ((NodeList) configXPath.evaluate(thePath, xmlDocument, XPathConstants.NODESET));
        } catch (Exception e) {
            System.err.println("PESADB.getValueFromLocalParams() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static void processSendMSG(String theReceiverType, String theReceiver, String theMSG, String theCategory) {
        try {
            try {
                String strProductID = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID");
                String strSender = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/SENDER");
                long lnMSGProductId = Long.parseLong(strProductID);
                String strOriginatorID = UUID.randomUUID().toString();

                int status = MSGProcessor.sendMSG(strOriginatorID, lnMSGProductId, "SENDER_ID", strSender,
                        theReceiverType, theReceiver, "TEXT", theMSG, "BulkSMS", MSGConstants.Sensitivity.NORMAL,
                        theCategory, 210, "YES", MSGConstants.MSGMode.SAF, "USSD", "", "MBANKING_SERVER", "");

                if(status <= 0){
                    System.err.println("ERROR Sending "+theCategory+" to " + theReceiver + "\n");
                }

            } catch (Exception e) {
                System.out.println("MBankingAPI.processSendMSG() Error message: " + e.getMessage());
            }

        } finally {}
    }

    public static void processSendEmail(String theIdentifier, String theMSGSubject, String theMSG, String theCategory) {
        try {
            try {

            } catch (Exception e) {
                System.out.println("MBankingAPI.processSendEmail() Error message: " + e.getMessage());
            }

        } finally {}
    }
}
