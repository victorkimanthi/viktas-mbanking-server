package ke.skyworld.mbanking.pesaapi;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.pesa.*;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.cbs.CBSAPI;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.xtreme.XTremeDBCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.UUID;

public class PESAAPI {
    public static void confirmPESA_IN(PESA thePESAIN, PESAINResponse thePESAINResponse){
        String strResponse = "";
        try{
            thePESAINResponse.setCategory("MPESA_C2B_DEPOSIT");

            String entryCode = UUID.randomUUID().toString().toUpperCase();
            String transactionID = thePESAIN.getSourceReference();
            String transaction = "Paybill";
            String description = "Paybill - " + thePESAIN.getSenderIdentifier() + " "+ thePESAIN.getSenderName();

            if(thePESAIN.getCommand().equals("PesaLink")){
                transaction = "Bank Deposit";
                description = "PL|IN|"+thePESAIN.getSourceIdentifier()+"|"+thePESAIN.getBeneficiaryIdentifier();
            }

            String strBeneficiaryAccount = thePESAIN.getReceiverAccount();
            HashMap<String, String> hmValue = XTremeDBCache.retrieve(strBeneficiaryAccount);
            if(hmValue != null) strBeneficiaryAccount = hmValue.get("value");

            HashMap<String,String> hmRVal =  CBSAPI.mobileMoneyDeposit(thePESAIN.getOriginatorID(), thePESAIN.getSourceReference(), thePESAIN.getSenderType(), thePESAIN.getSenderIdentifier(), thePESAIN.getSenderName(), thePESAIN.getReceiverType(), thePESAIN.getReceiverIdentifier(), strBeneficiaryAccount, thePESAIN.getTransactionAmount(),
                                                                    thePESAIN.getTransactionRemark(), thePESAIN.getSourceReference(),thePESAIN.getInitiatorApplication(),thePESAIN.getSourceApplication(),thePESAIN.getPesaDateCreated());


            String strTransactionStatus = hmRVal.get("transaction_status");
            String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
            String strTransactionDateTime = hmRVal.get("transaction_date_time");
            String strTransactionDestinationReference = hmRVal.get("transaction_destination_reference");

            strTransactionStatus = (strTransactionStatus!=null) ? strTransactionStatus : "";

            if( strTransactionStatus.equalsIgnoreCase(PESAConstants.PESAResponse.SUCCESS.getValue())){
                thePESAINResponse.setResponseCode(102);
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                thePESAINResponse.setResponseDescription("CBS RESPONSE: "+ strTransactionStatus + " - " + strTransactionStatusDescription);
            }
            else if( strTransactionStatus.equalsIgnoreCase(PESAConstants.PESAResponse.DUPLICATE.getValue()) ){
                thePESAINResponse.setResponseCode(102);
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.DUPLICATE.getValue());
                thePESAINResponse.setResponseDescription("CBS RESPONSE: "+ strTransactionStatus + " - " + strTransactionStatusDescription);
            }
            else{
                thePESAINResponse.setResponseCode(103);
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                thePESAINResponse.setResponseDescription("CBS RESPONSE: "+ strTransactionStatus + " - " + strTransactionStatusDescription);
            }

            strResponse = (transactionID != null) ? transactionID : "";
            thePESAINResponse.setBeneficiaryReference(strTransactionDestinationReference);

        }catch (Exception e){
            System.err.println("PESAAPI.confirmPESA_IN() ERROR : " + e.getMessage());

            thePESAINResponse.setResponseCode(103);
            thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            thePESAINResponse.setResponseDescription("System Exception error confirmPESA_IN(): "+e.getMessage()+". Transaction NOT Accepted");
        }
    }

    public static void validatePESA_IN(PESA thePESAIN, PESAINResponse thePESAINResponse){
        String strResponse = "";
        try{
            thePESAINResponse.setCategory(thePESAIN.getCategory());

            try {
                System.out.println("********************************************************");
                System.out.println("                 VALIDATE PESA IN");
                System.out.println("********************************************************");
                System.out.println("TraceID               : " + thePESAIN.getTraceID()+"|");
                System.out.println("OriginatorID          : " + thePESAIN.getOriginatorID()+"|");
                System.out.println("Command               : " + thePESAIN.getCommand()+"|");
                System.out.println("Category              : " + thePESAIN.getCategory()+"|");
                System.out.println("ProductID             : " + thePESAIN.getProductID()+"|");
                System.out.println("BatchReference        : " + thePESAIN.getBatchReference()+"|");
                System.out.println("TransactionRemark     : " + thePESAIN.getTransactionRemark()+"|");

                System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount()+"|");
                System.out.println("InitiatorIdentifier   : " + thePESAIN.getInitiatorIdentifier()+"|");
                System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount()+"|");
                System.out.println("InitiatorName         : " + thePESAIN.getInitiatorName()+"|");
                System.out.println("InitiatorReference    : " + thePESAIN.getInitiatorReference()+"|");

                System.out.println("SourceType            : " + thePESAIN.getSourceType()+"|");
                System.out.println("SourceIdentifier      : " + thePESAIN.getSourceIdentifier()+"|");
                System.out.println("SourceAccount         : " + thePESAIN.getSourceAccount()+"|");
                System.out.println("SourceName            : " + thePESAIN.getSourceName()+"|");
                System.out.println("SourceReference       : " + thePESAIN.getSourceReference()+"|");

                System.out.println("SenderType            : " + thePESAIN.getSenderType()+"|");
                System.out.println("SenderIdentifier      : " + thePESAIN.getSenderIdentifier()+"|");
                System.out.println("SenderAccount         : " + thePESAIN.getSenderAccount()+"|");
                System.out.println("SenderName            : " + thePESAIN.getSenderName()+"|");
                System.out.println("SenderReference       : " + thePESAIN.getSenderReference()+"|");

                System.out.println("ReceiverType          : " + thePESAIN.getReceiverType()+"|");
                System.out.println("ReceiverIdentifier    : " + thePESAIN.getReceiverIdentifier()+"|");
                System.out.println("ReceiverAccount       : " + thePESAIN.getReceiverAccount()+"|");
                System.out.println("ReceiverName          : " + thePESAIN.getReceiverName()+"|");
                System.out.println("ReceiverReference     : " + thePESAIN.getReceiverReference()+"|");

                System.out.println("BeneficiaryType       : " + thePESAIN.getBeneficiaryType()+"|");
                System.out.println("BeneficiaryIdentifier : " + thePESAIN.getBeneficiaryIdentifier()+"|");
                System.out.println("BeneficiaryAccount    : " + thePESAIN.getBeneficiaryAccount()+"|");
                System.out.println("BeneficiaryName       : " + thePESAIN.getBeneficiaryName()+"|");
                System.out.println("BeneficiaryReference  : " + thePESAIN.getBeneficiaryReference()+"|");
                System.out.println();
            } catch (Exception e){
                e.printStackTrace();
            }

            String strDestinationReference = UUID.randomUUID().toString().toUpperCase();
            String strBeneficiaryType = thePESAIN.getBeneficiaryType();
            String strSourceIdentifier = thePESAIN.getBeneficiaryAccount();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strSourceOnCBS = "Mobile";

            if (strBeneficiaryType.equals("NATIONAL_ID")) {
                strSourceOnCBS = "ID";
            } else if (strBeneficiaryType.equals("ACCOUNT") || strBeneficiaryType.equals("ACCOUNT_NO")) {
                strSourceOnCBS = "ACCOUNT";
            }

            Element elAccountElement = null;

            //Validate in TEST always
            if(thePESAIN.getProductID() == 9) {
                elAccountElement = getValidate_PESA_IN_Element_BUSINESS_ACC(thePESAIN, doc);
            } else {
                //Validate Business Shortcode deposits in PROD
                if(thePESAIN.getProductID() == 216 && thePESAIN.getReceiverIdentifier().equals("4114587")) { // 4114587 - Viktas SACCO - C2B - LIPA NA VIKTAS
                    elAccountElement = getValidate_PESA_IN_Element_BUSINESS_ACC(thePESAIN, doc);
                } else { // 873200 - Viktas SACCO - C2B
                    elAccountElement = getValidate_PESA_IN_Element_NO_VALIDATION(strSourceIdentifier, strSourceOnCBS, strBeneficiaryType, doc);
                }
            }

            if (elAccountElement != null) {
                doc.appendChild(elAccountElement);
                String strXMLData = fnTransformXMLDocument(doc);
                thePESAINResponse.setOtherDetails(strXMLData);
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                thePESAINResponse.setResponseDescription("Account Found - Validation Accepted.");
            } else {
                thePESAINResponse.setOtherDetails("<OTHER_DETAILS/>");
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_FAILED.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.FAILED.getValue());
                thePESAINResponse.setResponseDescription("Account NOT Found - Validation NOT Accepted.");
            }
            thePESAINResponse.setBeneficiaryReference(strDestinationReference);
            thePESAINResponse.setBeneficiaryApplication("CBS");
        }catch (Exception e){
            System.err.println("PESAAPI.validatePESA_IN() ERROR : " + e.getMessage());
            thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
            thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            thePESAINResponse.setResponseDescription("System Exception error validatePESA_IN(): " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static void confirmPESA_OUT(PESA thePESAOUT, RequestPESAResponse theRequestPESAResponse) {
        String strResponse = "";
        try {
            theRequestPESAResponse.setCategory("CONFIRM_B2C");

            //todo - Implement Integration to CBS
            //String strTransactionStatus = Navision.getPort().insertMpesaTransaction(entryCode, transactionID, transaction, description, accountNo, BigDecimal.valueOf(amount), phoneNo, "", "USSD", transactionID, "MBANKING");
            String strTransactionStatus = "SUCCESS";

            String strDestinationReference = UUID.randomUUID().toString().toLowerCase(); //todo: Get this from CBS
            String strSourceType = thePESAOUT.getSourceType();
            String strSourceIdentifier = thePESAOUT.getSourceIdentifier();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strSourceOnCBS = "Mobile";

            if (strSourceType.equals("NATIONAL_ID")) {
                strSourceOnCBS = "ID";
            } else if (strSourceType.equals("ACCOUNT")) {
                strSourceOnCBS = "ACCOUNT";
            }

            Element elPesaOtherDetails = getValidate_PESA_OUT_Element(strSourceIdentifier, strSourceOnCBS, doc);

            if (elPesaOtherDetails != null) {

                String strProviderResponse = "ERROR";
                if (strTransactionStatus.equalsIgnoreCase("SUCCESS") ||
                        strTransactionStatus.equalsIgnoreCase("TRANSACTION_EXISTS")) {

                    /*
                        OTP Amount Expiry Date
                        111222 500 2021-05-05 12:30:00
                        222333 100 2021-05-05 12:45:00
                        333444 800 2021-05-05 13:00:00
                        333444 800 2021-05-05 13:15:00
                     */

                    /*
                    <OTHER_DETAILS>
                        <PESA_OTHER_DETAILS>
                            <PASS_KEY_DETAILS>
                                <PASS_KEY>34567</PASS_KEY>
                            </PASS_KEY_DETAILS>
                            <KYC_DETAILS/>
                            <PROVIDER_OTHER_DETAILS/>
                        </PESA_OTHER_DETAILS>
                    </OTHER_DETAILS>

                    APPROVED - Everything is OK. OTP is Valid & OTP Amount is <= Amount requested & Balance Sufficient
                    INVALID_OTP - OTP is Wrong or OTP Amount > Amount Requested
                    EXPIRED_OTP - OTP has expired (You can use INVALID OTP)
                    INSUFFICIENT_BALANCE - Amount Requested is <= Amount tied to OTP but account does not have enough funds
                    UNKNOWN_IDENTIFIER - The Beneficiary (Mostly MSISDN) is NOT found
                    INVALID_IDENTIFIER_TYPE - The Beneficiary Identifier Type is NOT MSISDN/ACCOUNT_NO/ID_NUMBER
                    ERROR - Error
                     */

                    double lnAmount = thePESAOUT.getTransactionAmount();
                    String strPassKey = MBankingXMLFactory.getXPathValueFromXMLString("/PESA_OTHER_DETAILS/PASS_KEY_DETAILS/PASS_KEY", thePESAOUT.getPESAXMLData());
                    String strBeneficiary = thePESAOUT.getBeneficiaryIdentifier();

                    double lnApprovedAmount = 500;
                    String strApprovedPassKey = "111222";
                    String strApprovedBeneficiary = "254706405989";

                    String strOTPTime = "2021-05-11 15:30:00";
                    String strCurrentDateTime = MBankingDB.getDBDateTime();

                    if ((strOTPTime.compareTo(strCurrentDateTime) >= 0) && (lnAmount <= lnApprovedAmount) && (strPassKey.equals(strApprovedPassKey)) && (strBeneficiary.equals(strApprovedBeneficiary))) {

                        theRequestPESAResponse.setResponseCode(102);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());

                        strProviderResponse = "APPROVED";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);

                    } else if (lnAmount > lnApprovedAmount) {

                        theRequestPESAResponse.setResponseCode(103);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                        strProviderResponse = "INSUFFICIENT_BALANCE";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                    } else if (!strPassKey.equals(strApprovedPassKey)) {
                        theRequestPESAResponse.setResponseCode(103);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                        strProviderResponse = "INVALID_OTP";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                    } else if (!strBeneficiary.equals(strApprovedBeneficiary)) {
                        theRequestPESAResponse.setResponseCode(103);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                        strProviderResponse = "UNKNOWN_IDENTIFIER";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                    } else if (strCurrentDateTime.compareTo(strOTPTime) > 0) { //If Current time is past strOTPTime
                        theRequestPESAResponse.setResponseCode(103);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                        strProviderResponse = "EXPIRED_OTP";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                    } else {
                        theRequestPESAResponse.setResponseCode(103);
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                        strProviderResponse = "ERROR";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                    }

                } else {
                    theRequestPESAResponse.setResponseCode(103);
                    theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());

                    strProviderResponse = "ERROR";
                    theRequestPESAResponse.setResponseDescription("CBS RESPONSE: " + strTransactionStatus);
                }

                Element elProviderResponse = doc.createElement("PROVIDER_RESPONSE");
                elProviderResponse.setTextContent(strProviderResponse);
                elPesaOtherDetails.appendChild(elProviderResponse);

                doc.appendChild(elPesaOtherDetails);
                String strXMLData = fnTransformXMLDocument(doc);
                theRequestPESAResponse.setOtherDetails(strXMLData);

            } else {
                theRequestPESAResponse.setOtherDetails("<OTHER_DETAILS/>");

                theRequestPESAResponse.setResponseCode(104);
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.FAILED.getValue());
                theRequestPESAResponse.setResponseDescription("Account NOT Found - Validation NOT Accepted.");
            }

            theRequestPESAResponse.setBeneficiaryApplication("MBANKING_SERVER");
            theRequestPESAResponse.setBeneficiaryReference(strDestinationReference);

        } catch (Exception e) {
            System.err.println("PESAAPI.confirmPESA_OUT() ERROR : " + e.getMessage());

            theRequestPESAResponse.setResponseCode(103);
            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            theRequestPESAResponse.setResponseDescription("System Exception error confirmPESA_IN(): " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static void validatePESA_OUT(PESA thePESAOUT, RequestPESAResponse theRequestPESAResponse) {
        String strResponse = "";
        try {
            theRequestPESAResponse.setCategory("B2C_VALIDATE");

            String strDestinationReference = UUID.randomUUID().toString().toUpperCase(); //todo: Get this from CBS
            String strSourceType = thePESAOUT.getSourceType();
            String strSourceIdentifier = thePESAOUT.getSourceIdentifier();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strSourceOnCBS = "Mobile";

            if (strSourceType.equals("NATIONAL_ID")) {
                strSourceOnCBS = "ID";
            } else if (strSourceType.equals("ACCOUNT")) {
                strSourceOnCBS = "ACCOUNT";
            }

            Element elAccountElement = getValidate_PESA_OUT_Element(strSourceIdentifier, strSourceOnCBS, doc);

            if (elAccountElement != null) {
                doc.appendChild(elAccountElement);
                String strXMLData = fnTransformXMLDocument(doc);
                theRequestPESAResponse.setOtherDetails(strXMLData);

                theRequestPESAResponse.setResponseCode(102);
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                theRequestPESAResponse.setResponseDescription("Account Found - Validation Accepted.");
            } else {

                theRequestPESAResponse.setOtherDetails("<OTHER_DETAILS/>");

                theRequestPESAResponse.setResponseCode(104);
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.FAILED.getValue());
                theRequestPESAResponse.setResponseDescription("Account NOT Found - Validation NOT Accepted.");
            }

            theRequestPESAResponse.setBeneficiaryApplication("MBANKING_SERVER");
            theRequestPESAResponse.setBeneficiaryReference(strDestinationReference);

        } catch (Exception e) {
            System.err.println("PESAAPI.validatePESA_OUT() ERROR : " + e.getMessage());

            theRequestPESAResponse.setResponseCode(103);
            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            theRequestPESAResponse.setResponseDescription("System Exception error validatePESA_OUT(): " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static void processPESAResult(PESAResult thePESAResult, PESAResultResponse thePESAResultResponse){

        try {

            String strPesaType = "";

            String strOriginatorID = thePESAResult.getOriginatorID();

            String strCategory = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "pesa_category");

            String strDescription = "";
            String strDescriptionPrefix = "";


            String strReceiverType = "";
            String strReceiverIdentifier = "";
            String strReceiverAccount = "";
            String strBeneficiaryType = "";
            String strBeneficiaryIdentifier = "";
            String strBeneficiaryAccount = "";
            String strBeneficiaryName = "";
            String strBeneficiaryReference = thePESAResult.getBeneficiaryReference();

            if(thePESAResult.getResultCode() == 105){
                //Get MPESA Result Name
                if(thePESAResult.getPESAType().equals("PESA_OUT")){



                    switch (strCategory){
                        case "MPESA_WITHDRAWAL":
                        case "MOBILE_MONEY_WITHDRAWAL":
                        case "BILL_PAYMENT":
                        case "BANK_TRANSFER":

                            /**
                             * RESULT -OTHER DETAILS XML
                             * --------------------------
                             * <RESULT>
                             *     <BENEFICIARY TYPE="MSISDN">
                             *         <IDENTIFIER>254712747943</IDENTIFIER>
                             *         <ACCOUNT>254712747943</ACCOUNT>
                             *         <NAME>EMMANUEL WANG'OMBE</NAME>
                             *         <REFERENCE>THD46CF9VQ</REFERENCE>
                             *         <APPLICATION>MSISDN</APPLICATION>
                             *     </BENEFICIARY>
                             *     <TRANSACTION CURRENCY="KES">
                             *         <AMOUNT>10.00</AMOUNT>
                             *         <AVAILABLE_AMOUNT>3213294.80</AVAILABLE_AMOUNT>
                             *         <RESERVE_AMOUNT>2484.06</RESERVE_AMOUNT>
                             *     </TRANSACTION>
                             *     <PROVIDER_OTHER_DETAILS>
                             *         <RESULT_TYPE>0</RESULT_TYPE>
                             *         <RESULT_CODE>0</RESULT_CODE>
                             *         <ORIGINATOR_CONVERSATION_ID>SWG_b417ddcf-7834-11f0-8258-f2a2b77b3e48</ORIGINATOR_CONVERSATION_ID>
                             *         <CONVERSATION_ID>AG_20250813_203007a90e118ac9bb91</CONVERSATION_ID>
                             *         <REGISTERED>Y</REGISTERED>
                             *         <DATE_TIME>13.08.2025 14:00:21</DATE_TIME>
                             *     </PROVIDER_OTHER_DETAILS>
                             * </RESULT>
                             */
                            String resultOtherDetailsXml = thePESAResult.getOtherDetails();

                            strBeneficiaryType = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/BENEFICIARY/@TYPE", resultOtherDetailsXml);
                            strBeneficiaryIdentifier = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/BENEFICIARY/IDENTIFIER", resultOtherDetailsXml);
                            strBeneficiaryAccount = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/BENEFICIARY/ACCOUNT", resultOtherDetailsXml);
                            strBeneficiaryName = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/BENEFICIARY/NAME", resultOtherDetailsXml);
                            strBeneficiaryReference = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/BENEFICIARY/REFERENCE", resultOtherDetailsXml);
                            strDescription = strPesaType + " to " + strBeneficiaryName;

                            break;
                        default:
                            strDescription = strDescriptionPrefix;
                    }

                    strDescription = shortenName(strDescription);
                }

                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(thePESAResult.getOriginatorID(), thePESAResult.getOriginatorID(), thePESAResult.getResultName(),thePESAResult.getResultDescription(),
                                                                                strBeneficiaryType, strBeneficiaryIdentifier, strBeneficiaryName, strBeneficiaryName,
                                                                                thePESAResult.getBeneficiaryReference(), thePESAResult.getDateCreated());

                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                if(strResultTransactionStatus.equals("SUCCESS")){
                    thePESAResultResponse.setResponseCode(102);
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                    thePESAResultResponse.setResponseDescription("SUCCESS");
                } else if(strResultTransactionStatus.equals("TRANSACTION_EXISTS")) {
                    thePESAResultResponse.setResponseCode(102);
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                    thePESAResultResponse.setResponseDescription("TRANSACTION_EXISTS");
                }
                else{
                    thePESAResultResponse.setResponseCode(103);
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                    thePESAResultResponse.setResponseDescription(strResultTransactionStatus);
                }
            } else {
                String strApiRequestID = UUID.randomUUID().toString().toLowerCase();

                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(strApiRequestID, thePESAResult.getOriginatorID(), thePESAResult.getResultName(),thePESAResult.getResultDescription(),
                                                                                    strBeneficiaryType, strBeneficiaryIdentifier, strBeneficiaryName, strBeneficiaryName,
                                                                                    thePESAResult.getBeneficiaryReference(), thePESAResult.getDateCreated());

                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                if(strResultTransactionStatus.equals("SUCCESS")){
                    thePESAResultResponse.setResponseCode(102);
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                    thePESAResultResponse.setResponseDescription("Reversal Succeeded");
                } else {
                    thePESAResultResponse.setResponseCode(103);
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                    thePESAResultResponse.setResponseDescription("Transaction reversal failed");
                }
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.processPESAResult() ERROR : " + e.getMessage());
            thePESAResultResponse.setResponseCode(103);
            thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            thePESAResultResponse.setResponseDescription("System Exception error: " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static PesaParam getPesaParam(MBankingConstants.ApplicationType theApplicationType, PESAAPIConstants.PESA_PARAM_TYPE thePesaParamType) {
        PesaParam rVal = new PesaParam();
        try {
            String strPesaParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS";

            switch (thePesaParamType) {
                case MPESA_B2C: {
                    strPesaParamType += "/SAFARICOM/MPESA_B2C";
                    break;
                }
                case MPESA_C2B: {
                    strPesaParamType += "/SAFARICOM/MPESA_C2B";
                    break;
                }
                case MPESA_C2B_BUY_GOODS: {
                    strPesaParamType += "/SAFARICOM/MPESA_C2B_BUY_GOODS";
                    break;
                }
                case MPESA_B2B: {
                    strPesaParamType += "/SAFARICOM/MPESA_B2B";
                    break;
                }
                case MPESA_FLOAT_PURCHASE: {
                    strPesaParamType += "/SAFARICOM/MPESA_FLOAT_PURCHASE";
                    break;
                }
                case FAMILY_BANK_PESALINK: {
                    strPesaParamType += "/FAMILY_BANK/PESALINK";
                    break;
                }
                case AIRTIME: {
                    strPesaParamType += "/GLOBAL/AIRTIME";
                    break;
                }
            }

            String strProductId = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/PRODUCT_ID");
            String strSenderIdentifier = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_IDENTIFIER");
            String strSenderAccount = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_ACCOUNT");
            String strSenderName = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_NAME");

            rVal.setProductId(strProductId);
            rVal.setSenderIdentifier(strSenderIdentifier);
            rVal.setSenderAccount(strSenderAccount);
            rVal.setSenderName(strSenderName);
        } catch (Exception e) {
            System.err.println("PESADB.getPesaParam() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    private static String getBeneficiaryDetailsFromDescription(String strXML) {
        String strRval = "";
        try {
            if (!strXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strRval = configXPath.evaluate("/OTHER_DETAILS/ReceiverName", xmlDocument, XPathConstants.STRING).toString();
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getBeneficiaryDetailsFromDescription() ERROR : " + e.getMessage());
        }
        return strRval;
    }

    private static String shortenName(String theDescription) {
        StringBuilder rValPre = new StringBuilder();
        StringBuilder rVal = new StringBuilder();
        if (theDescription.length() > 50) {
            for (int i = 0; i < theDescription.split(" ").length - 1; i++) {
                rValPre.append(theDescription.split(" ")[i]);
                rValPre.append(" ");
                if (rValPre.toString().trim().length() < 50) {
                    rVal.append(theDescription.split(" ")[i]);
                    rVal.append(" ");
                } else {
                    break;
                }
            }
        } else {
            return theDescription;
        }

        return rVal.toString().trim();
    }

    public boolean pesa_C2B_Request(String theOriginatorID, String theInitiatorTraceId, String theReceiver, String theReceiverDetails, String theAccount,
                                    String theCurrency, double theAmount, String theCategory, String theReference, String theRequestApplication,
                                    String theSourceApplication) {

        boolean bRVal = false;

        PESA thePESA = new PESA();

        try {
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);

            long lnProductID = Long.parseLong(pesaParam.getProductId());
            String strSender = pesaParam.getSenderIdentifier();
            String strSenderDetails = pesaParam.getSenderName();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strPesaCommand = "CustomerPayBillOnline";
            String strDate = MBankingDB.getDBDateTime().trim();

            thePESA.setOriginatorID(theOriginatorID);
            thePESA.setProductID(lnProductID);
            thePESA.setCategory(theCategory);
            thePESA.setPESAStatusCode(10);
            thePESA.setPESAStatusDescription("New PESA");
            thePESA.setPESAStatusDate(strDate);

            thePESA.setInitiatorType("MSISDN");
            thePESA.setInitiatorIdentifier(theReceiver);
            thePESA.setInitiatorAccount(theReceiver);
            thePESA.setInitiatorName(theReceiver);
            thePESA.setInitiatorReference(theInitiatorTraceId);
            thePESA.setInitiatorApplication(theSourceApplication);
            thePESA.setInitiatorOtherDetails("<DATA/>");

            thePESA.setSourceType("MSISDN");
            thePESA.setSourceIdentifier(theReceiver);
            thePESA.setSourceAccount(theReceiver);
            thePESA.setSourceName(theReceiverDetails);
            thePESA.setSourceReference(theInitiatorTraceId);
            thePESA.setSourceApplication(theSourceApplication);
            thePESA.setSourceOtherDetails("<DATA/>");

            thePESA.setSenderType("SHORT_CODE");
            thePESA.setSenderIdentifier(strSender);
            thePESA.setSenderAccount(strSenderAccount);
            thePESA.setSenderName(strSenderDetails);
            thePESA.setSenderOtherDetails("<DATA/>");

            thePESA.setReceiverType("MSISDN");
            thePESA.setReceiverIdentifier(theReceiver);
            thePESA.setReceiverAccount(theAccount);
            thePESA.setReceiverName(theReceiverDetails);
            thePESA.setReceiverOtherDetails("<DATA/>");

            thePESA.setBeneficiaryType("MSISDN");
            thePESA.setBeneficiaryIdentifier(theReceiver);
            thePESA.setBeneficiaryAccount(theReceiver);
            thePESA.setBeneficiaryName(theReceiverDetails);
            thePESA.setBeneficiaryOtherDetails("<DATA/>");

            thePESA.setBatchReference(theOriginatorID);
            thePESA.setCorrelationReference(theInitiatorTraceId);
            thePESA.setCorrelationApplication(theSourceApplication);
            thePESA.setTransactionCurrency("KES");
            thePESA.setTransactionAmount(theAmount);
            thePESA.setTransactionRemark("C2B Payment Request by " + strSenderDetails + " to " + theReceiver);
            thePESA.setCategory(theCategory);

            thePESA.setPESAType(PESAConstants.PESAType.PESA_IN);
            thePESA.setPESAAction(PESAConstants.PESAAction.C2B);
            thePESA.setCommand(strPesaCommand);
            thePESA.setSensitivity(PESAConstants.Sensitivity.NORMAL);

            thePESA.setCategory(theCategory);
            thePESA.setPriority(100);
            thePESA.setSendCount(0);

            thePESA.setSourceReference(theReference);
            thePESA.setPESAXMLData("<OTHER_DETAILS/>");

            thePESA.setSchedulePesa(PESAConstants.Condition.NO);
            thePESA.setPesaDateScheduled(strDate);
            thePESA.setPesaDateCreated(strDate);
            thePESA.setLocalDateCreated(strDate);

            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");
            System.out.println("Originator ID                  :" + thePESA.getOriginatorID()+"|");
            System.out.println("PESA ID                        :" + thePESA.getPESAID()+"|");
            System.out.println("Server ID                      :" + thePESA.getServerID()+"|");
            System.out.println("Product ID                     :" + thePESA.getProductID()+"|");
            System.out.println("PESA Type                      :" + thePESA.getPESAType().toString()+"|");
            System.out.println("PESA Action                    :" + thePESA.getPESAAction().toString()+"|");

            System.out.println("Initiator Type                 :" + thePESA.getInitiatorType()+"|");
            System.out.println("Initiator Identifier           :" + thePESA.getInitiatorIdentifier()+"|");
            System.out.println("Initiator Account              :" + thePESA.getInitiatorAccount()+"|");
            System.out.println("Initiator Name                 :" + thePESA.getInitiatorName()+"|");
            System.out.println("Initiator Reference            :" + thePESA.getInitiatorReference()+"|");
            System.out.println("Initiator Application          :" + thePESA.getInitiatorApplication()+"|");
            System.out.println("Initiator Other Details        :" + thePESA.getInitiatorOtherDetails()+"|");

            System.out.println("Source Type                    :" + thePESA.getSourceType()+"|");
            System.out.println("Source Identifier              :" + thePESA.getSourceIdentifier()+"|");
            System.out.println("Source Account                 :" + thePESA.getSourceAccount()+"|");
            System.out.println("Source Name                    :" + thePESA.getSourceName()+"|");
            System.out.println("Source Reference               :" + thePESA.getSourceReference()+"|");
            System.out.println("Source Application             :" + thePESA.getSourceApplication()+"|");
            System.out.println("Source Other Details           :" + thePESA.getSourceOtherDetails()+"|");

            System.out.println("Sender Type                    :" + thePESA.getSenderType()+"|");
            System.out.println("Sender Identifier              :" + thePESA.getSenderIdentifier()+"|");
            System.out.println("Sender Account                 :" + thePESA.getSenderAccount()+"|");
            System.out.println("Sender Name                    :" + thePESA.getSenderName()+"|");
            System.out.println("Sender Other Details           :" + thePESA.getSenderOtherDetails()+"|");
            System.out.println("Receiver Type                  :" + thePESA.getReceiverType()+"|");
            System.out.println("Receiver Identifier            :" + thePESA.getReceiverIdentifier()+"|");
            System.out.println("Receiver Account               :" + thePESA.getReceiverAccount()+"|");
            System.out.println("Receiver Name                  :" + thePESA.getReceiverName()+"|");
            System.out.println("Receiver Other Details         :" + thePESA.getReceiverOtherDetails()+"|");
            System.out.println("Beneficiary Type               :" + thePESA.getBeneficiaryType()+"|");
            System.out.println("Beneficiary Identifier         :" + thePESA.getBeneficiaryIdentifier()+"|");
            System.out.println("Beneficiary Account            :" + thePESA.getBeneficiaryAccount()+"|");
            System.out.println("Beneficiary Name               :" + thePESA.getBeneficiaryName()+"|");
            System.out.println("Beneficiary Other Details      :" + thePESA.getBeneficiaryOtherDetails()+"|");

            System.out.println("Batch Reference                :" + thePESA.getBatchReference()+"|");
            System.out.println("Correlation Reference          :" + thePESA.getCorrelationReference()+"|");
            System.out.println("Correlation Application        :" + thePESA.getCorrelationApplication()+"|");

            System.out.println("Transaction Currency           :" + thePESA.getTransactionCurrency()+"|");
            System.out.println("Transaction Amount             :" + thePESA.getTransactionAmount()+"|");
            System.out.println("Transaction Remark             :" + thePESA.getTransactionRemark()+"|");

            System.out.println("Command                        :" + thePESA.getCommand()+"|");
            System.out.println("Sensitivity                    :" + thePESA.getSensitivity()+"|");
            System.out.println("Category                       :" + thePESA.getCategory()+"|");
            System.out.println("Priority                       :" + thePESA.getPriority()+"|");
            System.out.println("Send Count                     :" + thePESA.getSendCount()+"|");
            System.out.println("PESA XML Data                  :" + thePESA.getPESAXMLData()+"|");
            //System.out.println("Send Integrity Hash            :" + thePESA.getSendIntegrityHash()+"|);
            System.out.println("Schedule Pesa                  :" + thePESA.getSchedulePesa()+"|");
            System.out.println("Date Scheduled                 :" + thePESA.getPesaDateScheduled()+"|");
            System.out.println("General Flag                   :" + thePESA.getGeneralFlag()+"|");
            System.out.println("Transaction Date               :" + thePESA.getPesaDateCreated()+"|");
            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");

            //bRVal = PESAProcessor.sendC2BPaymentRequest(thePESA);

            PESAINRequestResponse thePESAINRequestResponse = PESAProcessor.sendPESAINPaymentRequest(thePESA);

            if (thePESAINRequestResponse.getResponseCode() == 500 || thePESAINRequestResponse.getResponseCode() == 102) {
                bRVal = true;
            }

            System.out.println("thePESAINRequestResponse.getResponseCode() : " + thePESAINRequestResponse.getResponseCode());
            System.out.println("thePESAINRequestResponse.getResponseName() : " + thePESAINRequestResponse.getResponseName());
            System.out.println("thePESAINRequestResponse.getResponseDescription(): " + thePESAINRequestResponse.getResponseDescription());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("PESAAPI.pesa_C2B_Request() ERROR : " + e.getMessage());
        }

        return bRVal;
    }

    public boolean pesa_C2B_BUY_GOODS_Request(String theOriginatorID, String theInitiatorTraceId, String theReceiver, String theReceiverDetails, String theAccount,
                                    String theCurrency, double theAmount, String theCategory, String theReference, String theRequestApplication,
                                    String theSourceApplication) {

        boolean bRVal = false;

        PESA thePESA = new PESA();

        try {
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B_BUY_GOODS);

            long lnProductID = Long.parseLong(pesaParam.getProductId());
            String strSender = pesaParam.getSenderIdentifier();
            String strSenderDetails = pesaParam.getSenderName();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strPesaCommand = "CustomerPayBillOnline";
            String strDate = MBankingDB.getDBDateTime().trim();

            thePESA.setOriginatorID(theOriginatorID);
            thePESA.setProductID(lnProductID);
            thePESA.setCategory(theCategory);
            thePESA.setPESAStatusCode(10);
            thePESA.setPESAStatusDescription("New PESA");
            thePESA.setPESAStatusDate(strDate);

            thePESA.setInitiatorType("MSISDN");
            thePESA.setInitiatorIdentifier(theReceiver);
            thePESA.setInitiatorAccount(theReceiver);
            thePESA.setInitiatorName(theReceiver);
            thePESA.setInitiatorReference(theInitiatorTraceId);
            thePESA.setInitiatorApplication(theSourceApplication);
            thePESA.setInitiatorOtherDetails("<DATA/>");

            thePESA.setSourceType("MSISDN");
            thePESA.setSourceIdentifier(theReceiver);
            thePESA.setSourceAccount(theReceiver);
            thePESA.setSourceName(theReceiverDetails);
            thePESA.setSourceReference(theInitiatorTraceId);
            thePESA.setSourceApplication(theSourceApplication);
            thePESA.setSourceOtherDetails("<DATA/>");

            thePESA.setSenderType("SHORT_CODE");
            thePESA.setSenderIdentifier(strSender);
            thePESA.setSenderAccount(strSenderAccount);
            thePESA.setSenderName(strSenderDetails);
            thePESA.setSenderOtherDetails("<DATA/>");

            thePESA.setReceiverType("MSISDN");
            thePESA.setReceiverIdentifier(theReceiver);
            thePESA.setReceiverAccount(theAccount);
            thePESA.setReceiverName(theReceiverDetails);
            thePESA.setReceiverOtherDetails("<DATA/>");

            thePESA.setBeneficiaryType("MSISDN");
            thePESA.setBeneficiaryIdentifier(theReceiver);
            thePESA.setBeneficiaryAccount(theReceiver);
            thePESA.setBeneficiaryName(theReceiverDetails);
            thePESA.setBeneficiaryOtherDetails("<DATA/>");

            thePESA.setBatchReference(theOriginatorID);
            thePESA.setCorrelationReference(theInitiatorTraceId);
            thePESA.setCorrelationApplication(theSourceApplication);
            thePESA.setTransactionCurrency("KES");
            thePESA.setTransactionAmount(theAmount);
            thePESA.setTransactionRemark("C2B Payment Request by " + strSenderDetails + " to " + theReceiver);
            thePESA.setCategory(theCategory);

            thePESA.setPESAType(PESAConstants.PESAType.PESA_IN);
            thePESA.setPESAAction(PESAConstants.PESAAction.C2B);
            thePESA.setCommand(strPesaCommand);
            thePESA.setSensitivity(PESAConstants.Sensitivity.NORMAL);

            thePESA.setCategory(theCategory);
            thePESA.setPriority(100);
            thePESA.setSendCount(0);

            thePESA.setSourceReference(theReference);
            thePESA.setPESAXMLData("<OTHER_DETAILS/>");

            thePESA.setSchedulePesa(PESAConstants.Condition.NO);
            thePESA.setPesaDateScheduled(strDate);
            thePESA.setPesaDateCreated(strDate);
            thePESA.setLocalDateCreated(strDate);

            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");
            System.out.println("Originator ID                  :" + thePESA.getOriginatorID()+"|");
            System.out.println("PESA ID                        :" + thePESA.getPESAID()+"|");
            System.out.println("Server ID                      :" + thePESA.getServerID()+"|");
            System.out.println("Product ID                     :" + thePESA.getProductID()+"|");
            System.out.println("PESA Type                      :" + thePESA.getPESAType().toString()+"|");
            System.out.println("PESA Action                    :" + thePESA.getPESAAction().toString()+"|");

            System.out.println("Initiator Type                 :" + thePESA.getInitiatorType()+"|");
            System.out.println("Initiator Identifier           :" + thePESA.getInitiatorIdentifier()+"|");
            System.out.println("Initiator Account              :" + thePESA.getInitiatorAccount()+"|");
            System.out.println("Initiator Name                 :" + thePESA.getInitiatorName()+"|");
            System.out.println("Initiator Reference            :" + thePESA.getInitiatorReference()+"|");
            System.out.println("Initiator Application          :" + thePESA.getInitiatorApplication()+"|");
            System.out.println("Initiator Other Details        :" + thePESA.getInitiatorOtherDetails()+"|");

            System.out.println("Source Type                    :" + thePESA.getSourceType()+"|");
            System.out.println("Source Identifier              :" + thePESA.getSourceIdentifier()+"|");
            System.out.println("Source Account                 :" + thePESA.getSourceAccount()+"|");
            System.out.println("Source Name                    :" + thePESA.getSourceName()+"|");
            System.out.println("Source Reference               :" + thePESA.getSourceReference()+"|");
            System.out.println("Source Application             :" + thePESA.getSourceApplication()+"|");
            System.out.println("Source Other Details           :" + thePESA.getSourceOtherDetails()+"|");

            System.out.println("Sender Type                    :" + thePESA.getSenderType()+"|");
            System.out.println("Sender Identifier              :" + thePESA.getSenderIdentifier()+"|");
            System.out.println("Sender Account                 :" + thePESA.getSenderAccount()+"|");
            System.out.println("Sender Name                    :" + thePESA.getSenderName()+"|");
            System.out.println("Sender Other Details           :" + thePESA.getSenderOtherDetails()+"|");
            System.out.println("Receiver Type                  :" + thePESA.getReceiverType()+"|");
            System.out.println("Receiver Identifier            :" + thePESA.getReceiverIdentifier()+"|");
            System.out.println("Receiver Account               :" + thePESA.getReceiverAccount()+"|");
            System.out.println("Receiver Name                  :" + thePESA.getReceiverName()+"|");
            System.out.println("Receiver Other Details         :" + thePESA.getReceiverOtherDetails()+"|");
            System.out.println("Beneficiary Type               :" + thePESA.getBeneficiaryType()+"|");
            System.out.println("Beneficiary Identifier         :" + thePESA.getBeneficiaryIdentifier()+"|");
            System.out.println("Beneficiary Account            :" + thePESA.getBeneficiaryAccount()+"|");
            System.out.println("Beneficiary Name               :" + thePESA.getBeneficiaryName()+"|");
            System.out.println("Beneficiary Other Details      :" + thePESA.getBeneficiaryOtherDetails()+"|");

            System.out.println("Batch Reference                :" + thePESA.getBatchReference()+"|");
            System.out.println("Correlation Reference          :" + thePESA.getCorrelationReference()+"|");
            System.out.println("Correlation Application        :" + thePESA.getCorrelationApplication()+"|");

            System.out.println("Transaction Currency           :" + thePESA.getTransactionCurrency()+"|");
            System.out.println("Transaction Amount             :" + thePESA.getTransactionAmount()+"|");
            System.out.println("Transaction Remark             :" + thePESA.getTransactionRemark()+"|");

            System.out.println("Command                        :" + thePESA.getCommand()+"|");
            System.out.println("Sensitivity                    :" + thePESA.getSensitivity()+"|");
            System.out.println("Category                       :" + thePESA.getCategory()+"|");
            System.out.println("Priority                       :" + thePESA.getPriority()+"|");
            System.out.println("Send Count                     :" + thePESA.getSendCount()+"|");
            System.out.println("PESA XML Data                  :" + thePESA.getPESAXMLData()+"|");
            //System.out.println("Send Integrity Hash            :" + thePESA.getSendIntegrityHash()+"|);
            System.out.println("Schedule Pesa                  :" + thePESA.getSchedulePesa()+"|");
            System.out.println("Date Scheduled                 :" + thePESA.getPesaDateScheduled()+"|");
            System.out.println("General Flag                   :" + thePESA.getGeneralFlag()+"|");
            System.out.println("Transaction Date               :" + thePESA.getPesaDateCreated()+"|");
            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");

            //bRVal = PESAProcessor.sendC2BPaymentRequest(thePESA);

            PESAINRequestResponse thePESAINRequestResponse = PESAProcessor.sendPESAINPaymentRequest(thePESA);

            if (thePESAINRequestResponse.getResponseCode() == 500 || thePESAINRequestResponse.getResponseCode() == 102) {
                bRVal = true;
            }

            System.out.println("thePESAINRequestResponse.getResponseCode() : " + thePESAINRequestResponse.getResponseCode());
            System.out.println("thePESAINRequestResponse.getResponseName() : " + thePESAINRequestResponse.getResponseName());
            System.out.println("thePESAINRequestResponse.getResponseDescription(): " + thePESAINRequestResponse.getResponseDescription());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("PESAAPI.pesa_C2B_Request() ERROR : " + e.getMessage());
        }

        return bRVal;
    }

    public static String fnTransformXMLDocument(Document xmlDocument)
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Element getValidate_PESA_IN_Element(String theAccount, String theSource, Document doc) {
        try {
            //todo - Implement Integration to CBS
            //String strAccountNumberXML = Navision.getPort().getAccountTransferRecipientXML(theAccount, theSource);
            String strAccountNumberXML = "<Account><AccountNo>5000000800000</AccountNo><AccountName>JAMES BOND</AccountName><Name>JAMES BOND</Name><MemberNo>0000800</MemberNo><PhoneNo>+254706405989</PhoneNo></Account>";
            /*
            <Account>
                <AccountNo>5000000800000</AccountNo>
                <AccountName>JAMES BOND</AccountName>
                <Name>JAMES BOND</Name>
                <MemberNo>0000800</MemberNo>
                <PhoneNo>+254706405989</PhoneNo>
            </Account>
             */

            /*
            <OTHER_DETAILS>
                <PESA_OTHER_DETAILS>
                    <KYC_DETAILS>
                        <RESPONSE>
                            <KYC TYPE="ACCOUNT_NO">
                                <IDENTIFIER>10101010101</IDENTIFIER>
                                <ACCOUNT>10101010101</ACCOUNT>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS>DETAILS</OTHER_DETAILS>
                            </KYC>
                        </RESPONSE>
                    </KYC_DETAILS>
                </PESA_OTHER_DETAILS>
            </OTHER_DETAILS>
             */


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strBeneficiaryType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Element getValidate_PESA_OUT_Element(String theAccount, String theSource, Document doc) {
        try {
            //todo - Implement Integration to CBS
            //String strAccountNumberXML = Navision.getPort().getAccountTransferRecipientXML(theAccount, theSource);
            String strAccountNumberXML = "<Account><AccountNo>5000000800000</AccountNo><AccountName>JAMES BOND</AccountName><Name>JAMES BOND</Name><MemberNo>0000800</MemberNo><PhoneNo>+254706405989</PhoneNo></Account>";
            /*
            <Account>
                <AccountNo>5000000800000</AccountNo>
                <AccountName>JAMES BOND</AccountName>
                <Name>JAMES BOND</Name>
                <MemberNo>0000800</MemberNo>
                <PhoneNo>+254706405989</PhoneNo>
            </Account>
             */

            /*

            <OTHER_DETAILS>
                <PESA_OTHER_DETAILS>
                    <KYC_DETAILS>
                        <RESPONSE>
                            <KYC TYPE="MSISDN">
                                <IDENTIFIER>254720000000</IDENTIFIER>
                                <ACCOUNT>254720000000</ACCOUNT>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                            <KYC TYPE="NATIONAL_ID">
                                <IDENTIFIER>1232131131</IDENTIFIER>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                            <KYC TYPE="ACCOUNT_NO">
                                <IDENTIFIER>10101010101</IDENTIFIER>
                                <ACCOUNT>10101010101</ACCOUNT>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                        </RESPONSE>
                    </KYC_DETAILS>
                </PESA_OTHER_DETAILS>
            </OTHER_DETAILS>
             */


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strBeneficiaryType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Element getValidate_PESA_IN_Element_NO_VALIDATION(String theAccount, String theSource, String strSourceType, Document doc) {
        try {
            /**
             * <Account>
             *     <AccountNo>500023100023222</AccountNo>
             *     <AccountName>FOSA SAVINGS A/C</AccountName>
             *     <Name>DEREK PRINCE MUTENDE</Name>
             *     <MemberNo>23122</MemberNo>
             *     <PhoneNo>254713000249</PhoneNo>
             * </Account>
             */
            String strAccountNumberXML = "" +
                    "<Account>" +
                    "    <AccountNo>500023100023222</AccountNo>" +
                    "    <AccountName>FOSA SAVINGS A/C</AccountName>" +
                    "    <Name>DEREK PRINCE MUTENDE</Name>" +
                    "    <MemberNo>23122</MemberNo>" +
                    "    <PhoneNo>254713000249</PhoneNo>" +
                    "</Account>";
            //String strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccount, theSource);;


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                /**
                 * <RESPONSE>
                 *     <KYC_DETAILS>
                 *         <RESPONSE>
                 *             <KYC TYPE="MSISDN/NATIONAL_ID">
                 *                 <IDENTIFIER>254713000249</IDENTIFIER>
                 *                 <ACCOUNT>500023100023222</ACCOUNT>
                 *                 <NAME>DEREK PRINCE MUTENDE</NAME>
                 *                 <OTHER_DETAILS />
                 *             </KYC>
                 *         </RESPONSE>
                 *     </KYC_DETAILS>
                 * </RESPONSE>
                 */
                elPesaOtherDetails = doc.createElement("RESPONSE");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strSourceType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Element getValidate_PESA_IN_Element_BUSINESS_ACC(PESA thePESA, Document doc) {
        try {

            /**
             * //RESPONSE
             * {
             *    "request_status": "SUCCESS/SHORT_CODE_NOT_FOUND/ERROR",
             *    "business_short_code": "500100",
             *    "business_name": "John's Hardware",
             *    "associated_account": {
             *      "account_name": "Salary Account",
             *      "account_label": "Salary Account (41-02392-0093-01)",
             *      "account_number": "41-02392-0093-01",
             *      "account_balance": 1600.00
             *    }
             *  }
             */
            HashMap<Object, Object> hmAPIRVal = CBSAPI.verifyBusinessShortCode(thePESA.getTraceID(), "MSISDN", thePESA.getInitiatorIdentifier(),"IMSI", "123456789101", "MSISDN", thePESA.getInitiatorIdentifier(), thePESA.getReceiverAccount());

            String requestStatus = String.valueOf(hmAPIRVal.get("request_status"));

            String strIdentifier = "";
            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if(requestStatus.equalsIgnoreCase("SUCCESS")) {
                Element elPesaOtherDetails = null;

                HashMap<String, String> associatedAccount = (HashMap<String, String>) hmAPIRVal.get("associated_account");

                strIdentifier = String.valueOf(hmAPIRVal.get("business_short_code"));
                strAccountNo = String.valueOf(associatedAccount.get("account_number"));
                strAccountName = String.valueOf(hmAPIRVal.get("business_name"));
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                /**
                 * <RESPONSE>
                 *     <KYC_DETAILS>
                 *         <RESPONSE>
                 *             <KYC TYPE="MSISDN/NATIONAL_ID">
                 *                 <IDENTIFIER>254713000249</IDENTIFIER>
                 *                 <ACCOUNT>500023100023222</ACCOUNT>
                 *                 <NAME>DEREK PRINCE MUTENDE</NAME>
                 *                 <OTHER_DETAILS />
                 *             </KYC>
                 *         </RESPONSE>
                 *     </KYC_DETAILS>
                 * </RESPONSE>
                 */
                elPesaOtherDetails = doc.createElement("RESPONSE");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", "ACCOUNT_NO");
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(strIdentifier);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);

                return elPesaOtherDetails;

            } else {
                strAccountStatus = "NOT_FOUND";
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
