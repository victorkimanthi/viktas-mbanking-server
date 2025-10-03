package nav;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import utils.Crypto;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class NavisionUtils {
    public static NavisionLocalParams getNavisionLocalParameters() {
        try {
            String strFilePath = System.getProperty("user.dir")+ File.separator+ "navision_conf.xml";

            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(strFilePath)));
            String strLine;
            StringBuilder stringBuilder = new StringBuilder();

            while((strLine=bufferedReader.readLine())!= null){
                stringBuilder.append(strLine.trim());
            }

            String strConfig = stringBuilder.toString();

            InputSource source = new InputSource(new StringReader(strConfig));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strType = configXPath.evaluate("/CONFIG/CORE_BANKING/TYPE", xmlDocument, XPathConstants.STRING).toString();
            String strURL = configXPath.evaluate("/CONFIG/CORE_BANKING/URL", xmlDocument, XPathConstants.STRING).toString();
            String strUsername = configXPath.evaluate("/CONFIG/CORE_BANKING/USERNAME", xmlDocument, XPathConstants.STRING).toString();
            String strPassword = configXPath.evaluate("/CONFIG/CORE_BANKING/PASSWORD", xmlDocument, XPathConstants.STRING).toString();
            String strPasswordType = configXPath.evaluate("/CONFIG/CORE_BANKING/PASSWORD/@TYPE", xmlDocument, XPathConstants.STRING).toString();
            String strNamespaceURL = configXPath.evaluate("/CONFIG/CORE_BANKING/NAMESPACE_URL", xmlDocument, XPathConstants.STRING).toString();
            String strLocalPort = configXPath.evaluate("/CONFIG/CORE_BANKING/LOCAL_PORT", xmlDocument, XPathConstants.STRING).toString();

            String strEncryptionKey = "Vx@3GhTu*7nbHJg^)SYTDhs>pij?2H";

            if(strPasswordType.equalsIgnoreCase("CLEARTEXT")){
                // Get the root element
                NodeList nlCoreBanking= xmlDocument.getFirstChild().getChildNodes().item(0).getChildNodes();
                Node ndPassword = nlCoreBanking.item(3);

                Crypto crypto = new Crypto();
                String strEncryptedPassword = crypto.encrypt(strEncryptionKey, strPassword);
                ndPassword.setTextContent(strEncryptedPassword);
                ndPassword.getAttributes().getNamedItem("TYPE").setTextContent("ENCRYPTED");

                // write the content into xml file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

                DOMSource dOMSource = new DOMSource(format(xmlDocument));
                StreamResult result = new StreamResult(new File(strFilePath));
                transformer.transform(dOMSource, result);
            } else if(strPasswordType.equalsIgnoreCase("ENCRYPTED")){
                strPassword = new Crypto().decrypt(strEncryptionKey, strPassword);
            } else {
                System.err.println("NavisionUtils.getNavisionLocalParameters() Error. Unknown password type");
                return null;
            }

            NavisionLocalParams localParams = new NavisionLocalParams();
            localParams.setCoreBankingType(strType);
            localParams.setCoreBankingUrl(strURL);
            localParams.setCoreBankingUsername(strUsername);
            localParams.setCoreBankingPassword(strPassword);
            localParams.setCoreBankingNamespaceUrl(strNamespaceURL);
            localParams.setCoreBankingLocalPort(strLocalPort);

            return localParams;

        } catch (Exception e) {
            System.err.println("NavisionUtils.getNavisionLocalParameters() Error. "+e.getMessage());
        }

        return null;
    }

    public static Document format(Document theXMLDocument) {
        Document rVal = null;
        try{
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;

            transformer = tf.newTransformer();

            StringWriter writer = new StringWriter();

            transformer.transform(new DOMSource(theXMLDocument), new StreamResult(writer));

            String xmlString = writer.getBuffer().toString();

            xmlString = prettyFormat(xmlString, "4");

            System.out.println("XML String formatted: "+xmlString);

            InputSource source = new InputSource(new StringReader(xmlString));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            rVal = builder.parse(source);
        } catch (Exception e){
            System.err.println(new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static String prettyFormat(String input, String indent) {
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent);
            transformer.transform(xmlInput, new StreamResult(stringWriter));

            return stringWriter.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

