package ke.skyworld.mbanking.mbankingapi;

import ke.skyworld.crypto.DataEncryption;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;
import ke.skyworld.lib.mbanking.utils.HTTPSClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MBankingAPIUtils {
    final static String strPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgI4n+ZCWsalONbDipWZUPVNXAPBEk7/gW7pKxTlpy0SHTvufQr2Sqp5xt+4iC5pUVCozjLa5iCAG77NbJy2b2ccmmV0yLkyFVh+0SyMUw7HyG5l38ydT28Qgb5fq/KXLrNWh7rziQo1PATh+ah1RNUqQ8K0+/f0YUFSI6brGC7v/At8YcDGgAK5Wc8pNMNw8KwUq4ydmfJrB4blOTUXmFLR+8TBTgIaSq17aQGKonDIDn8Fpo0W19l64ZtxufBdt9QUJQVTbaZlwEF8CgoT0YVNNA1fOe7WBw5v4WTKwiOYlhGIHrbvQoc2UTkCxlYETlOoNzoydvdzETlvCTzD4OQIDAQAB";
    final static String strPrivateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCAjif5kJaxqU41sOKlZlQ9U1cA8ESTv+BbukrFOWnLRIdO+59CvZKqnnG37iILmlRUKjOMtrmIIAbvs1snLZvZxyaZXTIuTIVWH7RLIxTDsfIbmXfzJ1PbxCBvl+r8pcus1aHuvOJCjU8BOH5qHVE1SpDwrT79/RhQVIjpusYLu/8C3xhwMaAArlZzyk0w3DwrBSrjJ2Z8msHhuU5NReYUtH7xMFOAhpKrXtpAYqicMgOfwWmjRbX2Xrhm3G58F231BQlBVNtpmXAQXwKChPRhU00DV857tYHDm/hZMrCI5iWEYgetu9ChzZROQLGVgROU6g3OjJ293MROW8JPMPg5AgMBAAECggEAKC9Ho6Vk1ag8wF/ImTdBgeH5QlvYs+64rTOwh7IItfd37GKPSAeGPztEBOC1V7coQR7n+kZr/Hc2c9s21SpWm167XBlbxEk7LM7ARvRyWzHWonX4ntNeTUYjdX6hf9Q7tI0bD0uP1onhdlo4eecsKWJjqDgfhrmEzid+nME7gxMRRpcsOTXhkYrsAgOXLkL8GQUZBIou2NhsTwmKzNkVz/gc4WD6b20LnS6TJdjHvjfy1DT+AEd3bdWyYUWsoyZQAio1MTpbPxvpGh49OOp60QenSilGYM2iLzERyD3PNc3Hx1bl+56Wq1OVD7phnNjsnX47hIo08EAm8VXHLq7VuQKBgQDD0nJYfiMrgMqBVV605Apo3Q2Cbu/DfqG0aZV3/GR3AD5+q5/b2llgpRvbLrgE7EmvvFddc73D41dMXzpdwtm9/+SU0jci2Qmmvv/UTqcXbvCWfh7JkO8rP8c19chu2r+YR2IMx48OytmQEsV/0H7mbuf39unaT2yySjTA9RGajwKBgQCoD8D3BkyaJU904qSQNEy8IuPdBKKQIn8MhrzRwyxtvfy52UXXcOYNod0fQRqDAly7AZUoSVrplebNygAdKN8TWPHgYmzqtOlbhlCEnOO9XLk7zwTgs6QJIJgwnVprBeMzoCMtoLaJyDq3SdvmOfFJvCDp5UKfgt4/iMeNQVrEtwKBgA2pNMjvo8x5I6d6KS09a2x9X1/mFVvyDZ3kb8T7GpcisTltB63ywaF4Y0UbMUNGqK1V2lJurKJpzcFKM2wvF7mljHDFaYtI0N+NG5PYGNgNqUMWcVdmgQjnXiJpjx4MrKkW8cQqd9R0WlEuvhB4nyG8QvqNgyrzt4WIn72GW0AJAoGAacfQqysZ2AQX6PgmoGVqzxge2CRstdAgq5+7BUSVmFV21vt8zEfRZU82QM/XghJgj4xFd+AECvZBGdJFFBV/o0veol8RMwG/x83YrD+b0LqmFJEO/ufTHbOYVzETkj1YbkwjGDsJ6dtPqcIhWN2rk7+H7/BPaNsUTGUpRS2Xli0CgYBv1IITYDpWFKSA7ntEV8AYC0IA7Qs4WXuM8sNGSETbUoetuzShjWwLLKKIoFYH9GyDICGhfnHBQbeeo6sFWiVwi1yBQoPaZfAf+26ZvxZsJTsgCKB4efzQ7DuSlSBekh/wbPA2rrNdeaT7M4cBnm+rccPGZJrTecd8eWqEvY99bw==";

    static String strUsername = null;
    static String strPassword = null;
    static String strPasswordType = null;
    static String strServiceURL = null;
    static String strAuthorizationParam_Base64 = null;
    public static String jsonHttpsPost(String theData)
    {
        String returnString = "";

        try {
            /*
            <OTHER_DETAILS>
                <CUSTOM_PARAMETERS>
                    ...
                    <ENDPOINT_PARAMETERS>
                        <ENDPOINT NAME="CBS">
                            <AUTHENTICATION_PARAMETERS>
                                <USERNAME>username</USERNAME>
                                <PASSWORD TYPE="CLEARTEXT">password</PASSWORD> <!-- CLEARTEXT/ENCRYPTED -->
                            </AUTHENTICATION_PARAMETERS>
                            <SERVICE_URLS>
                                <SERVICE_URL>https://localhost:44324/api/mbanking/ProcessRequest</SERVICE_URL>
                            </SERVICE_URLS>
                        </ENDPOINT>
                    </ENDPOINT_PARAMETERS>
                    ...
                </CUSTOM_PARAMETERS>
            </OTHER_DETAILS>

             //String strHttpsURL ="https://localhost:44324/api/mbanking/ProcessRequest";
            //String strHttpsURL ="https://172.17.11.23:88/api/mbanking/ProcessRequest";
            //String strHttpsURL ="https://localhost:88/api/mbanking/ProcessRequest";
             */

            String strUSSDClientXMLParameters = USSDLocalParameters.getClientXMLParameters();

            if(strUsername == null){
                Document docUSSDClientXMLParameters = MBankingXMLFactory.convertStringToDocument(strUSSDClientXMLParameters);
                strUsername = MBankingXMLFactory.getXPathValueFromXMLDocument("/OTHER_DETAILS/CUSTOM_PARAMETERS/ENDPOINT_PARAMETERS/ENDPOINT/AUTHENTICATION_PARAMETERS/USERNAME", docUSSDClientXMLParameters);
                strPassword = MBankingXMLFactory.getXPathValueFromXMLDocument("/OTHER_DETAILS/CUSTOM_PARAMETERS/ENDPOINT_PARAMETERS/ENDPOINT/AUTHENTICATION_PARAMETERS/PASSWORD", docUSSDClientXMLParameters);
                strPasswordType = MBankingXMLFactory.getXPathValueFromXMLDocument("/OTHER_DETAILS/CUSTOM_PARAMETERS/ENDPOINT_PARAMETERS/ENDPOINT/AUTHENTICATION_PARAMETERS/PASSWORD/@TYPE", docUSSDClientXMLParameters);
                strServiceURL = MBankingXMLFactory.getXPathValueFromXMLDocument("/OTHER_DETAILS/CUSTOM_PARAMETERS/ENDPOINT_PARAMETERS/ENDPOINT/SERVICE_URLS/SERVICE_URL", docUSSDClientXMLParameters);


                DataEncryption theDataEncryption = new DataEncryption();
                if( strPasswordType.equalsIgnoreCase(USSDConstants.PasswordType.ENCRYPTED.getValue())) {
                    String strPassword_ENCRYPTED = strPassword;
                    strPassword = theDataEncryption.decrypt(strPrivateKey, strPassword_ENCRYPTED) ;
                }else{
                    String strPassword_ENCRYPTED = theDataEncryption.encrypt(strPublicKey, strPassword);

                    Element elPassword = MBankingXMLFactory.getXPathElementFromXMLDocument("/OTHER_DETAILS/CUSTOM_PARAMETERS/ENDPOINT_PARAMETERS/ENDPOINT/AUTHENTICATION_PARAMETERS/PASSWORD", docUSSDClientXMLParameters);
                    elPassword.setTextContent(strPassword_ENCRYPTED);
                    elPassword.setAttribute("TYPE",USSDConstants.PasswordType.ENCRYPTED.getValue());
                    strUSSDClientXMLParameters = MBankingXMLFactory.convertDocumentToString(docUSSDClientXMLParameters,true);
                    MBankingDB.updateClientXMLParameters(USSDLocalParameters.getParametersID(), strUSSDClientXMLParameters);
                    USSDLocalParameters.setClientXMLParameters(strUSSDClientXMLParameters);
                }

                String strAuthorizationParam = strUsername+":"+strPassword;
                strAuthorizationParam_Base64 = new String( Base64.getEncoder().encode(strAuthorizationParam.getBytes()) );
            }

            HTTPSClient theHTTPSClient = new HTTPSClient();

            HashMap<String,String> hmRequestProperties = new HashMap<>();

            hmRequestProperties.put("Authorization","Basic " + strAuthorizationParam_Base64);
            hmRequestProperties.put("Content-Type","application/json");

            int intConnectionTimeoutSeconds = 30;
            theHTTPSClient.disableCertificateValidation("TLS");
            returnString = httpsPost(strServiceURL, theData, hmRequestProperties, intConnectionTimeoutSeconds);
            theHTTPSClient = null;

            //TODO: Comment on GO LIVE
            System.out.println();
            System.out.println("***************************************************************************************************");
            System.out.println("JSON Request = " + theData);
            System.out.println("**************");
            System.out.println("JSON Response = " + returnString);
            System.out.println("***************************************************************************************************");
            System.out.println();

        }catch (Exception e){
            System.out.println("Error on MBankingUtils.jsonHttpsPost(): "+ e.getMessage());
            returnString = null;
        }
        finally{

        }
        return returnString;
    }

    public static String serializeXMLDocNode(Node node) throws Throwable {
        // you may prefer to use single instances of Transformer, and
        // StringWriter rather than create each time. That would be up to your
        // judgement and whether your app is single threaded etc
        StreamResult xmlOutput = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), xmlOutput);
        return xmlOutput.getWriter().toString();
    }

    public static String httpsPost(String theHttpsURL, String theData, HashMap<String,String> theRequestProperties, int theConnectionTimeoutSeconds)
    {
        String returnString = "";
        URL theURL = null;
        HttpsURLConnection conn = null;
        InputStream ins = null;
        InputStreamReader isr = null;
        BufferedReader rd = null;
        try {

            theURL = new URL(theHttpsURL);
            conn = (HttpsURLConnection)theURL.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches (false);
            conn.setConnectTimeout(theConnectionTimeoutSeconds * 1000);
            conn.setReadTimeout(theConnectionTimeoutSeconds * 1000);
            if(theRequestProperties!=null){
                for(Map.Entry<String, String> entry : theRequestProperties.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    conn.addRequestProperty(key, value);
                }
            }
            conn.connect();

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
            wr.writeBytes(theData);
            wr.flush();
            wr.close();

            // Get the response
            ins = conn.getInputStream();
            isr = new InputStreamReader(ins);
            rd = new BufferedReader(isr);

            returnString = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                returnString = returnString + line;
            }

            rd.close();

        }catch (Exception e){
            System.out.println("Error on httpsPost(): "+ e.getMessage());
            returnString = null;
        }
        finally{
            conn.disconnect();
            theURL = null;
            conn = null;
            ins = null;
            isr = null;
            rd = null;
        }
        return returnString;
    }

}
