package ke.skyworld.mbanking.mappapi;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.PESA;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.pesa.PESAProcessor;
import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.cbs.CBSAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPIUtils;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.mbanking.xtreme.XTremeDBCache;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.CON;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseStatus.ERROR;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseStatus.SUCCESS;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponsesDataType.TEXT;
import static ke.skyworld.lib.mbanking.register.RegisterConstants.IdentityType.NATIONAL_ID;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;
import static ke.skyworld.mbanking.ussdapi.USSDAPI.truncateString;

public class MAPPAPI {

    boolean blGroupBankingEnabled = false;

    private MAPPResponse setMAPPResponse(Node theRepsonseMSG, MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = new MAPPResponse();

        try {
            String strDateTime = MBankingDB.getDBDateTime();
            theMAPPResponse.setMessagesVersion("1.01");
            theMAPPResponse.setMessagesDateTime(strDateTime);
            theMAPPResponse.setSessionID(theMAPPRequest.getSessionID());
            theMAPPResponse.setMAPPType(theMAPPRequest.getMAPPType());

            theMAPPResponse.setMSG(theRepsonseMSG);
            theMAPPResponse.setDateCreated(strDateTime);
            theMAPPResponse.setIntegrityHash("");
        }catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+".setMAPPResponse() ERROR : " + e.getMessage());
        }


        return theMAPPResponse;
    }

    private void generateResponseMSGNode(Document doc, Element theElementData, MAPPRequest theMAPPRequest, MAPPConstants.ResponseAction theAction, MAPPConstants.ResponseStatus theStatus, String theCharge, String theTitle, MAPPConstants.ResponsesDataType theDataType){
        MAPPResponse theMAPPResponse = new MAPPResponse();

        try {
            /*
            <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='END' STATUS='FAILED' CHARGE='NO'>
                <TITLE>Login Failed</TITLE>
                <DATA TYPE='TEXT'>Invalid Mobile Number or PIN</DATA>
            </MSG>
             */
            //TEST

            Element elMSG = doc.createElement("MSG");
            doc.appendChild(elMSG);

                // set attribute SESSION_ID to MSG element
                Attr attrSessionID = doc.createAttribute("SESSION_ID");
                attrSessionID.setValue(Long.toString(theMAPPRequest.getSessionID()));
                elMSG.setAttributeNode(attrSessionID);

                // set attribute TYPE to MSG element
                Attr attrType = doc.createAttribute("TYPE");
                attrType.setValue(theMAPPRequest.getMAPPType().getValue());
                elMSG.setAttributeNode(attrType);

                // set attribute ACTION to MSG element
                Attr attrAction = doc.createAttribute("ACTION");
                attrAction.setValue(theAction.getValue());
                elMSG.setAttributeNode(attrAction);

                // set attribute STATUS to MSG element
                Attr attrStatus = doc.createAttribute("STATUS");
                attrStatus.setValue(theStatus.getValue());
                elMSG.setAttributeNode(attrStatus);

                // set attribute CHARGE to MSG element
                Attr attrCharge = doc.createAttribute("CHARGE");
                attrCharge.setValue(theCharge);
                elMSG.setAttributeNode(attrCharge);

                // set Element TITLE to MSG element
                Element elTitle = doc.createElement("TITLE");
                elTitle.setTextContent(theTitle);
                elMSG.appendChild(elTitle);

                // set Element TYPE to MSG element
                elMSG.appendChild(theElementData);

                    // set attribute CHARGE to MSG element
                    Attr attrDataType = doc.createAttribute("TYPE");
                    attrDataType.setValue(theDataType.getValue());
                    theElementData.setAttributeNode(attrDataType);

        }catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+".generateResponseMSGNode() ERROR : " + e.getMessage());
        }
    }

    static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    String getUserFullName(MAPPRequest theMAPPRequest, String strUserPhoneNumber){
        String strAccountName = "";
        try{
            //XPath configXPath =  XPathFactory.newInstance().newXPath();

            /*
            <Account><AccountNo>5000000163000</AccountNo><Name>MOSES MAGERO</Name></Account>
             */
            //todo - Implement Integration to CBS
            //String strAccountNumberXML = Navision.getPort().getAccountTransferRecipientXML(strUserPhoneNumber, "Mobile");
            //String strAccountNumberXML = "<Account><AccountNo>5000000163000</AccountNo><Name>MOSES MAGERO</Name></Account>";

            HashMap<Object, Object> hmRVal = getUserDetails(theMAPPRequest, "Mobile No", strUserPhoneNumber);
            HashMap<String, String> hmUserDetails = (HashMap<String, String>) hmRVal.get("user_details");

            String strTitle= "Account Details";

            String strCharge = "NO";
            String strAccountStatus = "NOT_FOUND";

            if(hmUserDetails != null && !hmUserDetails.isEmpty()){
                String requestStatus = String.valueOf(hmUserDetails.get("request_status"));
                if(requestStatus.equals("SUCCESS")){

                    strAccountName = String.valueOf(hmUserDetails.get("full_name"));
                    strAccountName = Utils.toTitleCase(strAccountName);
                    strAccountStatus = "FOUND";
                }
            }
        } catch (Exception e){
            System.out.println(this.getClass().getSimpleName()+".getUserDetails() ERROR: ");
        }
        return strAccountName;
    }

    public MAPPResponse userLogin(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strVersion = theMAPPRequest.getVersion();
            String strMessagesVersion = theMAPPRequest.getMessagesVersion();
            String strAppID = theMAPPRequest.getAppID();

            //System.out.println(strAppID);
            Node ndRequestMSG = theMAPPRequest.getMSG();
            printXmlFromNode(ndRequestMSG);

            String strNotificationID = configXPath.evaluate("NOTIFICATION_ID", ndRequestMSG).trim();
            if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                strPassword = configXPath.evaluate("PASSWORD", ndRequestMSG).trim();
            }

            boolean blOTPVerificationRequired = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION).isEnabled();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Mobile Banking";
            String strDescription = "Welcome to Mobile Banking. Please visit your nearest branch to activate your account for mobile banking.";
            String strDbLoginActionValidDate = "";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            boolean isUSSD = false;
            String strLoginStatus = "ERROR";
            String strLoginAttemptMessage = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";

            HashMap<String,String> hmLoginRVal = CBSAPI.userLogin(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID);

            if (!hmLoginRVal.isEmpty()) {
                strLoginStatus = hmLoginRVal.get("login_status");
                strDbLoginActionValidDate = hmLoginRVal.get("auth_action_valid_date");

                if (strLoginStatus.equals("INCORRECT_PIN")) {
                    int intLoginAttempts = Integer.parseInt(hmLoginRVal.get("login_attempts"));
                    int intCurrentLoginAttempts = intLoginAttempts+1;

                    HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
                    hmMSGPlaceholders.put("[MOBILE_NUMBER]", strUsername);
                    hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(intLoginAttempts));
                    hmMSGPlaceholders.put("[FIRST_NAME]", "Member");

                    String xml = MAPPLocalParameters.getClientXMLParameters();
                    HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(intLoginAttempts,
                            hmMSGPlaceholders, xml, MBankingConstants.AuthType.PASSWORD);

                    HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
                    HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

                    //Default Incorrect PIN message
                    strLoginAttemptMessage = "You have entered an incorrect username or password, please try again.";
                    String endSession = "NO";

                    //Check if action is needed
                    if (!currentAuthenticationAttemptsAction.isEmpty()) {
                        String strLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
                        String strLoginActionTag = currentAuthenticationAttemptsAction.get("NAME");

                        //Check action
                        switch (strLoginAction) {
                            case "SUSPEND": {
                                int intLoginActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
                                String strLoginActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
                                intLoginActionDuration = APIUtils.convertToSeconds(intLoginActionDuration, strLoginActionDurationUnit);
                                Date loginActionValidDate = APIUtils.add(intLoginActionDuration, Calendar.SECOND);
                                String strLoginActionValidDate = APIUtils.convertDateToDateString(loginActionValidDate);

                                //Persist Action to DB
                                HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                        "PASSWORD",  strLoginAction, strLoginActionValidDate, strLoginActionTag, APIUtils.getCurrentDateTime());
                                String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " + strLoginActionDurationUnit + "(S)";

                                if(!hmRValAuth.isEmpty()){
                                    String setAuthStatus = hmRValAuth.get("set_auth_security_parameters_status");
                                    if(setAuthStatus.equals("SUCCESS")){
                                        //Override Incorrect PIN message
                                        String strTryAgainIn = "Please try again in " + friendlyActionDuration;
                                        strLoginAttemptMessage = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;
                                    }
                                }
                                endSession = "YES";
                                break;
                            }

                            case "LOCK": {
                                //Persist Action to DB
                                HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                        "PASSWORD",  strLoginAction, null, strLoginActionTag, APIUtils.getCurrentDateTime());

                                if(!hmRValAuth.isEmpty()){
                                    String setAuthStatus = hmRValAuth.get("set_auth_security_parameters_status");
                                    if(setAuthStatus.equals("SUCCESS")){
                                        //Override Incorrect PIN message
                                        strLoginAttemptMessage = "Your mobile banking account has been LOCKED. Please visit one of our branches for assistance or contact us.";
                                    }
                                }
                                endSession = "YES";
                                break;
                            }

                            default: {
                                //Persist Action to DB
                                HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                        "PASSWORD",  strLoginAction, null, strLoginActionTag, APIUtils.getCurrentDateTime());
                            }
                        }
                    }

                    //Check future action
                    if (!futureAuthenticationAttemptsAction.isEmpty()) {
                        String futureLoginAction = futureAuthenticationAttemptsAction.get("ACTION");
                        String futureLoginActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
                        String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " + futureLoginActionDurationUnit + "(S)";
                        String attemptsRemainingToFutureLoginAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

                        String currentLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
                        if (currentLoginAction == null) currentLoginAction = "NONE";

                        //Override Incorrect PIN message
                        if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
                            if (endSession.equals("NO")) {
                                strLoginAttemptMessage = "You have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your mobile banking account is SUSPENDED for " + friendlyFutureActionDuration + ".";
                            }
                        } else if (futureLoginAction.equals("LOCK") && !currentLoginAction.equals("LOCK")) {
                            if (endSession.equals("NO")) {
                                strLoginAttemptMessage = "You have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your mobile banking account is LOCKED.";
                            }
                        }
                    }
                } else if (strLoginStatus.equals("SUCCESS")) {
                    //Reset Login Auth Parameters
                    HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                            "PASSWORD",  "NONE", null, null, APIUtils.getCurrentDateTime());
                }
            }

            Element elData = doc.createElement("DATA");

            boolean blLoginSuccessful = false;

            switch (strLoginStatus) {
                case "SUCCESS": {
                    strTitle = "Login Successful";
                    strDescription = "The login was successful";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    blLoginSuccessful = true;

                    if (blOTPVerificationRequired) {
                        generateOTP(theMAPPRequest);
                    }

                    break;
                }
                case "INVALID_IMEI":
                case "INVALID_APP_ID":
                case "MOBILE_APP_INACTIVE":
                case "INVALID_IMEI_WITH_KYC":
                case "INVALID_DEVICE_IDENTIFIER": {
                    strTitle = "Mobile App is Not Activated";
                    String strActivationInstructions = ""+
                            "To retrieve your mobile app activation code:"+
                            "<br/>1. Dial <b>*882#</b>"+
                            "<br/>2. Enter your Mobile Banking PIN"+
                            "<br/>3. Select <b>'My Account'</b>"+
                            "<br/>4. Select <b>'Mobile App'</b>"+
                            "<br/>5. Select <b>'ACTIVATE Mobile App'</b>"+
                            "<br/>6. Select <b>'Yes'</b>"+
                            "<br/>7. Wait for an SMS with the mobile app activation code"+
                            "<br/>8. Enter the activation code below then press <b>'Activate'</b>";

                    strDescription = "Your Mobile App is not activated. Tap 'ACTIVATE' below to activate the Mobile App.";

                    Element elActivationInstructions = doc.createElement("ACTIVATION_INSTRUCTIONS");
                    elActivationInstructions.setTextContent(strActivationInstructions);
                    elData.appendChild(elActivationInstructions);

                    enResponseAction = MAPPConstants.ResponseAction.CHALLENGE_LOGIN;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "MOBILE_APP_INACTIVE_WITH_KYC":
                case "MOBILEAPP_INACTIVE_WITH_KYC": {
                    String strMemberName = getUserFullName(theMAPPRequest, strUsername).split(" ")[0];
                    strTitle = "Mobile App is Not Activated@@@@@" + strMemberName;
                    String strActivationInstructions = "Please enter your details below";

                    strDescription = "Your Mobile App is not activated. Select OK to activate the Mobile App." + ":::::" + strActivationInstructions;

                    enResponseAction = MAPPConstants.ResponseAction.CHALLENGE_LOGIN;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    strTitle = "Login Failed";
                    strDescription = strLoginAttemptMessage;
                    if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                        strTitle = "Incorrect Password";
                        strDescription = "You have entered an incorrect password, please try again";
                    }
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                        enResponseAction = CON;
                    }
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "SUSPEND":
                case "SUSPENDED": {
                    strTitle = "Account Access Suspended";
                    Date loginActionValidDate = APIUtils.convertDateStringToDate(strDbLoginActionValidDate);
                    Date currentDate = APIUtils.getCurrentJavaUtilDateTime();
                    if (loginActionValidDate != null && currentDate.before(loginActionValidDate)) {
                        String actionDuration = APIUtils.getPrettyDateTimeDifferenceRoundedUp(currentDate, Objects.requireNonNull(loginActionValidDate));
                        String strTryAgainIn = "Please try again in " + actionDuration;
                        strDescription = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    } else {
                        //Reset Login Auth Parameters
                        HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                "PASSWORD",  "NONE", null, null, APIUtils.getCurrentDateTime());

                        if(!hmLoginRVal.isEmpty() && hmRValAuth.get("set_auth_security_parameters_status").equals("SUCCESS")){
                            strTitle = "Login Successful";
                            strDescription = "The login was successful";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                            blLoginSuccessful = true;

                            if (blOTPVerificationRequired) {
                                generateOTP(theMAPPRequest);
                            }
                        } else {
                            strTitle = "Login Failed";
                            strDescription = "An Error occurred, please try again";
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        }
                    }
                    break;
                }
                case "LOCK":
                case "LOCKED": {
                    strTitle = "Account Access Locked";
                    strDescription = "Sorry, your " + AppConstants.strSACCOName + " mobile banking account is LOCKED. Please contact us for assistance.";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "BLOCKED": {
                    strTitle = "Account Blocked";
                    strDescription = "Your account is blocked, please visit your nearest SACCO branch for assistance.";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "NOT_FOUND": {
                    strTitle = "Login Failed";

                    strDescription = "You have entered an incorrect username or password, please try again";

                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "ERROR": {
                    strTitle = "Login Failed";
                    strDescription = "An Error occurred, please try again";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                }
            }

            String strCharge = "NO";

            Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
            elDescription.setTextContent(strDescription);
            elData.appendChild(elDescription);


            if (blLoginSuccessful) {
                String strMemberFullName = getUserFullName(theMAPPRequest, strUsername);
                String strMemberName = strMemberFullName.split(" ")[0];

                Element elMemberData = doc.createElement("MEMBER_DATA");
                elMemberData.setAttribute("NAME", strMemberName);
                elMemberData.setAttribute("FULL_NAME", strMemberFullName);
                elMemberData.setAttribute("GENDER", "MALE");
                elData.appendChild(elMemberData);
            } else {
                Element elMemberData = doc.createElement("MEMBER_DATA");
                elMemberData.setAttribute("NAME", "Member");
                elMemberData.setAttribute("FULL_NAME", "Member");
                elMemberData.setAttribute("GENDER", "MALE");
                elData.appendChild(elMemberData);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return theMAPPResponse;
    }

    public APIUtils.OTP checkOTPRequirement(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE theOtpCheckStage){
        boolean blRval = false;
        APIUtils.OTP otp = new APIUtils.OTP(0, 0, "", "",false);
        otp.setEnabled(false);

        Node ndRequestMSG;
        XPath configXPath;
        Node ndOTP;
        try{
            ndRequestMSG = theMAPPRequest.getMSG();
            configXPath = XPathFactory.newInstance().newXPath();

            String strOTPID = "";
            int intOTPTTL = 0;
            String strOTPTTL = "";
            int intOTPLength = 0;
            String strOTPLength = "";

            ndOTP = (Node) configXPath.evaluate("OTP", ndRequestMSG, XPathConstants.NODE);
            if(ndOTP != null){
                strOTPID = configXPath.evaluate("@ID", ndOTP).trim();
                otp.setId(strOTPID);
                strOTPTTL = configXPath.evaluate("@TTL", ndOTP).trim();
                if(strOTPTTL != null && !strOTPTTL.equals("")){
                    intOTPTTL = Integer.parseInt(strOTPTTL);
                    otp.setTtl(intOTPTTL);
                }
                strOTPLength = configXPath.evaluate("@LENGTH", ndOTP).trim();
                if(strOTPLength != null && !strOTPLength.equals("")){
                    intOTPLength = Integer.parseInt(strOTPLength);
                    otp.setLength(intOTPLength);
                }
            }

            if(theOtpCheckStage == MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION){
                if(ndOTP != null && intOTPTTL != 0 && intOTPLength != 0){
                    otp.setEnabled(true);
                }
            } else if(theOtpCheckStage == MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION){
                if(ndOTP != null){
                    otp.setEnabled(true);
                }
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } finally {
            ndRequestMSG = null;
            configXPath = null;
            ndOTP = null;
        }
        return otp;
    }

    public MAPPResponse validateOTP(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            boolean blAddDataAction = false;

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strActivationCode = configXPath.evaluate("OTP", ndRequestMSG).trim();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Error";
            String strDescription = "An error occurred. Please try again after a few minutes.";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            String strStartKey = "";
            strStartKey = (String) InMemoryCache.retrieve(strUsername + strActivationCode);

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            Element elData = doc.createElement("DATA");


            //Get OTP details from DB
            HashMap<String, String> hmAuthSecurityRVal = CBSAPI.getAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN",
                    strUsername, strPassword, "APP_ID", strAppID, "OTP");

            if (!hmAuthSecurityRVal.isEmpty() && hmAuthSecurityRVal.get("request_status").equals("SUCCESS")) {
                String dbOTPFlag = hmAuthSecurityRVal.get("auth_flag");
                String strUserLoginAttemptAction = hmAuthSecurityRVal.get("auth_action");
                String strDBOTPAttempts = hmAuthSecurityRVal.get("auth_attempts");
                int dbOTPAttempts = Integer.parseInt(strDBOTPAttempts);
                String strDbOTPActionValidDate = hmAuthSecurityRVal.get("auth_action_valid_date");
                Date dbOTPActionValidDate = null;
                if(strDbOTPActionValidDate != null && !strDbOTPActionValidDate.isEmpty()){
                    APIUtils.convertDateStringToDate(strDbOTPActionValidDate);
                }

                //Increase otp attempts
                int otpAttempts = dbOTPAttempts + 1;

                boolean blIncorrectOTP = false;

                if (strUserLoginAttemptAction.equalsIgnoreCase("SUSPEND")) {
                    strTitle = "OTP Validation Suspended";
                /*String strTryAgainIn = "Please try again in " + APIUtils.millisToLongDHMS(dblDuration);

                strDescription = "Sorry, your account is SUSPENDED from validating one time password. " + strTryAgainIn;*/
                    strDescription = "Sorry, your account has been SUSPENDED from validating one time password.";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    if (strActivationCode.equalsIgnoreCase(strStartKey)) {
                        String strUserAccountStatus;
                        if (theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) {
                            HashMap<String, String> hmActivateMAPP = CBSAPI.activateMobileApp(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword, strAppID);

                            if (!hmActivateMAPP.isEmpty()) {
                                strUserAccountStatus = hmActivateMAPP.get("mobile_app_activation_status");
                            } else {
                                strUserAccountStatus = "ERROR";
                            }
                        } else {
                            strUserAccountStatus = "SUCCESS";
                        }

                        switch (strUserAccountStatus) {
                            case "SUCCESS": {
                                strTitle = "Activation Successful";
                                strDescription = "Mobile app account activation was successful";
                                if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                                    strTitle = "OTP Validation Successful";
                                    strDescription = "Your OTP validation was successful";
                                }
                                enResponseAction = CON;
                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                //Reset OTP details in Database
                                HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                        "OTP",  "NONE", null, null, APIUtils.getCurrentDateTime());
                                InMemoryCache.remove(strUsername);
                                InMemoryCache.remove(strUsername + strActivationCode);
                                break;
                            }
                            case "BLOCKED": {
                                strTitle = "Account Blocked";
                                strDescription = "Your account is blocked, please visit you nearest SACCO branch for assistance.";
                                break;
                            }
                            case "NOT_FOUND": {
                                strTitle = "Account Not Found";
                                strDescription = "An error occurred. Please try again after a few minutes.";
                                break;
                            }
                            default: {
                                strTitle = "Activation Failed";
                                if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                                    strTitle = "OTP Validation Failed";
                                }
                                strDescription = "An error occurred. Please try again after a few minutes.";
                                break;
                            }
                        }
                    } else {
                        //Set OTP Attempts
                        CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                "OTP", otpAttempts,  "NONE", null, null, APIUtils.getCurrentDateTime());

                        strTitle = "Incorrect Activation Code";
                        strDescription = "The activation code you entered is either incorrect or has expired. Please confirm the activation code and try again.";

                        if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                            strTitle = "Incorrect One Time Password";
                            strDescription = "You entered an incorrect/expired One Time Password";
                        }

                        HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
                        hmMSGPlaceholders.put("[MOBILE_NUMBER]", strUsername);
                        hmMSGPlaceholders.put("[OTP_ATTEMPTS]", String.valueOf(otpAttempts));
                        hmMSGPlaceholders.put("[FIRST_NAME]", getUserFullName(theMAPPRequest, strUsername));

                        String xml = MAPPLocalParameters.getClientXMLParameters();
                        HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(otpAttempts,
                                hmMSGPlaceholders, xml, MBankingConstants.AuthType.OTP);

                        HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
                        HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

                        String endSession = "NO";

                        if (!currentAuthenticationAttemptsAction.isEmpty()) {

                            String resetOTP = currentAuthenticationAttemptsAction.get("RESET_OTP");
                            String otpAction = currentAuthenticationAttemptsAction.get("ACTION");
                            String otpActionTag = currentAuthenticationAttemptsAction.get("NAME");

                            //Check action
                            switch (otpAction) {
                                case "SUSPEND": {
                                    enResponseAction = MAPPConstants.ResponseAction.END;
                                    int otpActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
                                    String otpActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
                                    otpActionDuration = APIUtils.convertToSeconds(otpActionDuration, otpActionDurationUnit);
                                    Date otpActionValidDate = APIUtils.add(otpActionDuration, Calendar.SECOND);
                                    String strOTPActionValidDate = APIUtils.convertDateToDateString(otpActionValidDate);

                                    if (resetOTP.equals("YES")) {
                                        //remove OTP
                                        InMemoryCache.remove(strUsername);
                                    }

                                    //Persist Action to DB
                                    String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " + otpActionDurationUnit + "(S)";
                                    CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                            "OTP", otpAttempts,  otpAction, strOTPActionValidDate, otpActionTag, APIUtils.getCurrentDateTime());

                                    //Override Incorrect PIN message
                                    strTitle = "Account Suspended";
                                    String strTryAgainIn = "Please try again in " + friendlyActionDuration;
                                    strDescription = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;
                                    endSession = "YES";
                                    break;
                                }

                                case "LOCK": {
                                    enResponseAction = MAPPConstants.ResponseAction.END;
                                    if (resetOTP.equals("YES")) {
                                        //remove OTP
                                        InMemoryCache.remove(strUsername);
                                    }

                                    //Persist Action to DB
                                    CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                            "OTP", otpAttempts,  otpAction, null, otpActionTag, APIUtils.getCurrentDateTime());

                                    //Override Incorrect PIN message
                                    strTitle = "Account Locked";
                                    strDescription = "Your mobile banking account has been LOCKED. Please visit one of our branches for assistance or contact us.";
                                    endSession = "YES";
                                    break;
                                }

                                default: {
                                    //Persist Action to DB
                                    CBSAPI.setAuthSecurityParameters(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                                            "OTP", otpAttempts,  otpAction, null, otpActionTag, APIUtils.getCurrentDateTime());

                                    if (resetOTP.equals("YES")) {
                                        elData.setAttribute("ACTION", "REQUEST_OTP");
                                    }
                                }
                            }
                        }

                        //Check future action
                        if (!futureAuthenticationAttemptsAction.isEmpty()) {
                            String futureOTPAction = futureAuthenticationAttemptsAction.get("ACTION");
                            String futureOTPActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
                            String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " + futureOTPActionDurationUnit + "(S)";
                            String attemptsRemainingToFutureOTPAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

                            String currentOTPAction = currentAuthenticationAttemptsAction.get("ACTION");
                            if (currentOTPAction == null) currentOTPAction = "NONE";
                            String resetOTP = currentAuthenticationAttemptsAction.get("RESET_OTP");

                            //Override Incorrect PIN message
                            if (futureOTPAction.equals("SUSPEND") && !currentOTPAction.equals("SUSPEND")) {
                                strTitle = ((theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) ? "Incorrect Activation Code" : "Incorrect One Time Password");

                                if (endSession.equals("NO")) {
                                    strDescription = "You have " + attemptsRemainingToFutureOTPAction + " attempt(s) before your mobile banking account is SUSPENDED for " + friendlyFutureActionDuration + ".";
                                }
                            } else if (futureOTPAction.equals("LOCK") && !currentOTPAction.equals("LOCK")) {
                                strTitle = ((theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) ? "Incorrect Activation Code" : "Incorrect One Time Password");

                                if (endSession.equals("NO")) {
                                    strDescription = "You have " + attemptsRemainingToFutureOTPAction + " attempt(s) before your mobile banking account is LOCKED.";
                                }
                            }
                        }
                    }
                }
            }

            String strCharge = "NO";
            elData.setTextContent(strDescription);

            if (blAddDataAction) {
                elData.setAttribute("ACTION", "REQUEST_OTP");
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse generateOTP(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strSessionID = String.valueOf(lnSessionID);
            String strTraceID = getTraceID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION);

            int intOTPTTL = 0;
            int intOTPLength = 0;
            String strOTPID = "";

            if(otp.isEnabled()){
                intOTPTTL = otp.getTtl();
                intOTPLength = otp.getLength();
                strOTPID = otp.getId();
            }

            String strAppSignature = configXPath.evaluate("APP_SIGNATURE", ndRequestMSG).trim();
            if(strAppSignature == null){
                strAppSignature = "";
            }

            String strOneTImePIN = Utils.generateRandomString(intOTPLength);

            InMemoryCache.remove(strUsername+strOneTImePIN);
            InMemoryCache.store(strUsername+strOneTImePIN, strOneTImePIN, intOTPTTL);


            SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
            Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (intOTPTTL * 1000));

            String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
            String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


            String strMSG = "Dear Member,\n" + strOneTImePIN + " is your One Time Password(OTP) generated at " + strTimeGenerated + ". This OTP is valid up to " + strExpiryDate + ".\n" + strOTPID + (!strAppSignature.equals("") ? ("\n" + strAppSignature) : "");

            String strCharge = "YES";

            int intMSGSent = fnSendSMS(strUsername, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "ONE_TIME_PASSWORD", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);


            String strTitle = "OTP Generated and Sent Successfully";
            String strResponseText = "Your One Time Password was generated and sent successfully.";

            if (intMSGSent <= 0) {
                strTitle = "OTP Generation Failed";
                strResponseText = "There was an error sending your One Time Password. Please try again";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse activateMobileAppWithKYC(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + ":" + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strActivationCode = configXPath.evaluate("ACTIVATION_CODE", ndRequestMSG).trim();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "";
            String strDescription = "";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            String strUserAccountStatus = "ERROR";

            HashMap<String, String> hmActivateMAPP = CBSAPI.activateMobileAppWithKYC(getTraceID(theMAPPRequest), "MSISDN", strUsername,
                    strPassword, strAppID, NATIONAL_ID.getValue(), strActivationCode);

            if (!hmActivateMAPP.isEmpty()) {
                strUserAccountStatus = hmActivateMAPP.get("mobile_app_activation_status");
            } else {
                strUserAccountStatus = "ERROR";
            }

            switch (strUserAccountStatus) {
                case "SUCCESS": {
                    strTitle = "Activation Successful";
                    strDescription = "Mobile app account activation was successful";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "ERROR": {
                    strTitle = "Account Blocked";
                    strDescription = "Your account is blocked, please visit you nearest SACCO branch for assistance.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    strTitle = "Incorrect ID Number";
                    strDescription = "The ID Number you entered is incorrect or has expired. Please confirm the activation code and try again.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                case "NOT_FOUND": {
                    strTitle = "Account Not Found";
                    strDescription = "An error occurred. Please try again after a few minutes.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                default: {
                    strTitle = "Activation Failed";
                    strDescription = "An error occurred. Please try again after a few minutes.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
            }

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strDescription);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse registerMember(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try{
            System.out.println("registerMember");
            XPath configXPath =  XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();

            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strName = configXPath.evaluate("NAME", ndRequestMSG).trim();
            String strPhoneNumber = configXPath.evaluate("PHONE_NUMBER", ndRequestMSG).trim();
            String strNationalIDNumber = configXPath.evaluate("NATIONAL_ID_NUMBER", ndRequestMSG).trim();
            String strDateOfBirth = configXPath.evaluate("DATE_OF_BIRTH", ndRequestMSG).trim();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            Date dtMemberDateOfBirth = format.parse(strDateOfBirth);

            GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
            calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
            XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

            //todo - Implement Integration to CBS
            //String strNewMemberRegistrationStatus = Navision.getPort().registerVirtualMember(strName, strNationalIDNumber, strPhoneNumber, xmlGregCalMemberDateOfBirth, strUsername, strEntryNumber);
            String strNewMemberRegistrationStatus = "SUCCESS";
            switch (strNewMemberRegistrationStatus){
                case "SUCCESS":{
                    NodeList nlMemberImages = ((NodeList) configXPath.evaluate("PASSPORT_SIZE_IMAGES/IMAGE", ndRequestMSG, XPathConstants.NODESET));
                    NodeList nlNationalIDImages = ((NodeList) configXPath.evaluate("NATIONAL_ID_IMAGES/IMAGE", ndRequestMSG, XPathConstants.NODESET));

                    //todo - Implement Integration to CBS
                    //String strImagesPath = Navision.getPort().getVirtualMemberRegistrationImagesPath();
                    String strImagesPath = "/tmp";

                    for (int i = 0; i < nlMemberImages.getLength(); i++) {
                        String strImageName = configXPath.evaluate("@NAME", nlMemberImages.item(i)).trim();
                        String strImageType = configXPath.evaluate("@TYPE", nlMemberImages.item(i)).trim();
                        String strImageData = configXPath.evaluate("@DATA", nlMemberImages.item(i)).trim();

                        String strImagesPathForPhotographs = strImagesPath+"\\photographs\\"+strImageName+"."+strImageType;
                        APIUtils.fnCreateFileFromBase64(strImageData, strImagesPathForPhotographs);
                        //todo - Implement Integration to CBS
                        //Navision.getPort().updateVirtualMemberRegistration(strImageName, strImagesPathForPhotographs.replace("\\\\", "\\"), strEntryNumber, "Member Photographs");
                    }

                    for (int i = 0; i < nlNationalIDImages.getLength(); i++) {
                        String strImageName = configXPath.evaluate("@NAME", nlNationalIDImages.item(i)).trim();
                        String strImageType = configXPath.evaluate("@TYPE", nlNationalIDImages.item(i)).trim();
                        String strImageData = configXPath.evaluate("@DATA", nlNationalIDImages.item(i)).trim();

                        String strImagesPathForIDs = strImagesPath+"\\ids\\"+strImageName+"."+strImageType;
                        APIUtils.fnCreateFileFromBase64(strImageData, strImagesPathForIDs);
                        //todo - Implement Integration to CBS
                        //Navision.getPort().updateVirtualMemberRegistration(strImageName, strImagesPathForIDs.replace("\\\\", "\\"), strEntryNumber, "National ID");
                    }

                    strTitle= "Request Received Successfully";
                    strResponseText = "Your member registration was received successfully.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "ERROR":{
                    strTitle= "ERROR: Register New Member";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle= "ERROR: Register New Member";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                }
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse getHomePageAddons(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try{
            System.out.println("getHomePageAddons");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            String strTitle;
            String strResponseText;

            String strCharge = "NO";

            String strNewMemberRegistrationStatus = "SUCCESS";
            Element elData = doc.createElement("DATA");

            switch (strNewMemberRegistrationStatus){
                case "SUCCESS":{
                    strTitle= "Request Received Successfully";

                    elData.setAttribute("TYPE", "ELEMENT");


                    Element elAddons = doc.createElement("ADD_ONS");
                    elData.appendChild(elAddons);

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "HOME");
                        elAddon.setAttribute("TAB", "HOME");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        Element elCard = createCardElement(doc, "AGM Announcement", "We will be having an AGM on 4th January 2021. Kindly plan to attend.", MAPPAPIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.CONTACT_US.getValue());
                        elButtons.appendChild(elButton);
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "TRANSACT");
                        elAddon.setAttribute("TAB", "TRANSACT");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        Element elCard = createCardElement(doc, "Launch of B2B Services", "We have launched Bank to Bank transfer services and you can now send money from SACCO to Bank.", MAPPAPIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.BANK_TRANSFER.getValue());
                        elButtons.appendChild(elButton);
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "ACCOUNTS");
                        elAddon.setAttribute("TAB", "MY_ACCOUNT");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        {
                            Element elCard = createCardElement(doc, "Total FOSA Accounts", "12345", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total BOSA Accounts", "5000", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        Element elList = doc.createElement("LIST");
                        {
                            elList.setAttribute("TYPE", "ACCOUNTS");
                            elAddon.appendChild(elList);
                            Element elCategories = doc.createElement("CATEGORIES");
                            elList.appendChild(elCategories);
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "All Accounts");
                                elCategory.setAttribute("NAME", "ALL");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "BOSA");
                                elCategory.setAttribute("NAME", "BOSA");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "FOSA");
                                elCategory.setAttribute("NAME", "FOSA");
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Savings Accounts");
                            elItems.setAttribute("CATEGORIES", "ALL,FOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "23893", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Deposit Contribution");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "456655", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Shares");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "563456", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "LOANS");
                        elAddon.setAttribute("TAB", "LOANS");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        {
                            Element elCard = createCardElement(doc, "Total Outstanding Loans", "12345", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total Guaranteed Loans", "5000", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        Element elList = doc.createElement("LIST");
                        {
                            elList.setAttribute("TYPE", "LOAND");
                            elAddon.appendChild(elList);
                            Element elCategories = doc.createElement("CATEGORIES");
                            elList.appendChild(elCategories);
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "My Loans");
                                elCategory.setAttribute("NAME", "MY_LOANS");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "Guaranteed Loans");
                                elCategory.setAttribute("NAME", "GUARANTEED_LOANS");
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Normal Loan");
                            elItems.setAttribute("CATEGORIES", "MY_LOANS");
                            {
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.PAY_LOAN.getValue());
                                elButtons.appendChild(elButton);
                            }
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.LOAN_STATEMENT.getValue());
                                elButtons.appendChild(elButton);
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Normal Loan");
                            elItems.setAttribute("CATEGORIES", "GUARANTEED_LOANS");
                            {
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                        }
                    }


                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "ERROR":{
                    strTitle= "ERROR";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    elData.setTextContent(strResponseText);
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle= "ERROR";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    elData.setTextContent(strResponseText);
                }
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    Element createCardElement(Document theDocument, String theLabel, String theValue, MAPPAPIConstants.CardValueType theType, float theFontSize){
        Element rVal = theDocument.createElement("CARD");
        try {
            rVal.setAttribute("LABEL", theLabel);
            rVal.setAttribute("VALUE", theValue);
            rVal.setAttribute("TYPE", theType.getValue());
            rVal.setAttribute("FONT_SIZE", String.valueOf(theFontSize));
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    Element createItemElement(Document theDocument, String theLabel, String theValue, MAPPAPIConstants.CardValueType theType){
        Element rVal = theDocument.createElement("ITEM");
        try {
            rVal.setAttribute("LABEL", theLabel);
            rVal.setAttribute("VALUE", theValue);
            rVal.setAttribute("TYPE", theType.getValue());
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public MAPPResponse getBankAccounts(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType, boolean theForWithdrawal, String theAction) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{5-04-00010-02=Salary Acc (5-04-00010-02), 4-61-90010-01=Micro-cred (4-61-90010-01)}*/
            LinkedHashMap<String, String> accounts = null;

            switch (theAccountType.getValue()) {
                case "FOSA": {
                    accounts = getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.FOSA.getValue());
                    break;
                }

                case "BOSA": {
                    accounts = getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.BOSA.getValue());
                    break;
                }

                case "ALL": {
                    accounts = getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.FOSA.getValue());
                    accounts.putAll(getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.BOSA.getValue()));
                    break;
                }

                default: {
                    accounts = getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.FOSA.getValue());
                }

            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);


            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }

            if (theAction.equalsIgnoreCase("GET_TRANSACTION_ACCOUNTS_AND_DEPOSIT_SERVICES")) {
                Element elServices = doc.createElement("SERVICES");
                elData.appendChild(elServices);

                //create element SERVICE and append to element SERVICES
                Element elServiceMpesa = doc.createElement("SERVICE");
                elServiceMpesa.setAttribute("ID", "MPESA");
                elServiceMpesa.setTextContent("Safaricom M-PESA");
                elServices.appendChild(elServiceMpesa);

                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMaximum();

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(strMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(strMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);
            }
            /*Start of Account Statement Duration Changes*/
            else {
                /*Prerequisites*/
                /*Add the following block of xml code to mapp client parameters XML under */
                /*OTHER_DETAILS / CUSTOM_PARAMETERS / SERVICE_CONFIGS*/

                /*<CONFIGURATION>
                    <ACCOUNT_STATEMENT>
                        <STATEMENT_PERIODS>
                            <PERIOD NAME="CUSTOM" LABEL="Custom Period" STATUS="ACTIVE" START_DATE="MONTH_START" END_DATE="MONTH_END" MAXIMUM_TRANSACTIONS="100"/>
                            <PERIOD NAME="1WEEK" LABEL="Past 1 Week" STATUS="ACTIVE" START_DATE="TODAY-7D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="50"/>
                            <PERIOD NAME="2WEEKS" LABEL="Past 2 Weeks" STATUS="ACTIVE" START_DATE="TODAY-14D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="75"/>
                            <PERIOD NAME="1MONTHS" LABEL="Past 1 Month" STATUS="ACTIVE" START_DATE="TODAY-30D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="100"/>
                            <PERIOD NAME="3MONTHS" LABEL="Past 3 Months" STATUS="ACTIVE" START_DATE="TODAY-90D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="250"/>
                            <PERIOD NAME="6MONTHS" LABEL="Past 6 Months" STATUS="ACTIVE" START_DATE="TODAY-183D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="500"/>
                            <PERIOD NAME="YTD" LABEL="This Year To Date" STATUS="ACTIVE" START_DATE="TODAY-YTD" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="750"/>
                            <PERIOD NAME="1YEAR" LABEL="Past 1 Year" STATUS="ACTIVE" START_DATE="TODAY-365D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="1000"/>
                        </STATEMENT_PERIODS>
                    </ACCOUNT_STATEMENT>
                </CONFIGURATION>*/
                Element elStatementConfiguration = doc.createElement("STATEMENT_CONFIGURATION");
                elStatementConfiguration.setAttribute("DEFAULT", "CUSTOM");

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

                /*Added the function below under APIUtils*/
                LinkedList<HashMap<String, String>> llHmStatementPeriods = APIUtils.getStatementPeriods(MBankingConstants.ApplicationType.MAPP);

                llHmStatementPeriods.forEach(hmStatementPeriods -> {
                    String strName = hmStatementPeriods.get("NAME");
                    String strLabel = hmStatementPeriods.get("LABEL");
                    String strStartDate = hmStatementPeriods.get("START_DATE");
                    String strEndDate = hmStatementPeriods.get("END_DATE");
                    String strMaximumTransactions = hmStatementPeriods.get("MAXIMUM_TRANSACTIONS");

                    long lnEndDate = System.currentTimeMillis();
                    long lnStartDate = lnEndDate;
                    long lnMillisecondsInDay = 86400000;

                    if (strStartDate.matches("(TODAY-)+(\\d{1,})+(D)") && strEndDate.matches("^TODAY$")) {
                        String strDays = "";

                        Pattern ptPattern = Pattern.compile("(?!TODAY)(-)\\d{1,}(?=D)");
                        Matcher mtMatcher = ptPattern.matcher(strStartDate);
                        if (mtMatcher.find()) {
                            strDays = mtMatcher.group();
                            long lnDays = Long.parseLong(strDays);
                            lnStartDate = lnEndDate + (lnDays * lnMillisecondsInDay);
                        }
                    }

                    if (strStartDate.matches("^MONTH_START$") && strEndDate.matches("^MONTH_END$")) {
                        LocalDate ldToday = LocalDate.now();
                        lnStartDate = ldToday.withDayOfMonth(1).toEpochDay() * lnMillisecondsInDay;
                        lnEndDate = ldToday.withDayOfMonth(ldToday.lengthOfMonth()).toEpochDay() * lnMillisecondsInDay;
                    }

                    if (strStartDate.matches("^TODAY-YTD$") && strEndDate.matches("^TODAY$")) {
                        LocalDate ldToday = LocalDate.now();
                        lnStartDate = ldToday.withDayOfYear(1).toEpochDay() * lnMillisecondsInDay;
                        lnEndDate = ldToday.toEpochDay() * lnMillisecondsInDay;
                    }

                    strStartDate = String.valueOf(lnStartDate);
                    strEndDate = String.valueOf(lnEndDate);

                    Element elStatementPeriod = doc.createElement("PERIOD");
                    elStatementPeriod.setAttribute("LABEL", strLabel);
                    elStatementPeriod.setAttribute("NAME", strName);
                    elStatementPeriod.setAttribute("START_DATE", strStartDate);
                    elStatementPeriod.setAttribute("END_DATE", strEndDate);
                    elStatementPeriod.setAttribute("MAXIMUM_TRANSACTIONS", strMaximumTransactions);
                    elStatementConfiguration.appendChild(elStatementPeriod);
                });

                elData.appendChild(elStatementConfiguration);
            }
            /*Start of Account Statement Duration Changes*/

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse getATMCards(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            String strCardsXML = ""+
                    "<ATM_CARDS>"+
                    "<CARD><ID>01</ID><NAME>9235808234587239</NAME></CARD>"+
                    "<CARD><ID>02</ID><NAME>3249058234598079</NAME></CARD>"+
                    "</ATM_CARDS>";

             /*
             //Response from CBS is:
                <ATM_CARDS>
                    <CARD><ID>01</ID><NAME>9235808234587239</NAME></CARD>
                    <CARD><ID>02</ID><NAME>3249058234598079</NAME></CARD>
                </ATM_CARDS>
             */

            InputSource source = new InputSource(new StringReader(strCardsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/ATM_CARDS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts</TITLE>
                    <DATA TYPE='LIST'>
                        <CARDS>
                            <CARD ID='123456' NAME='123456' />
                            <CARD ID='123457' NAME='123457' />
                        </CARDS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            Element elAccounts = doc.createElement("CARDS");
            elData.appendChild(elAccounts);

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("ID", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("NAME", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("CARD");
                elAccounts.appendChild(elAccount);

                elAccount.setAttribute("ID", strAccountNo);
                elAccount.setAttribute("NAME", strAccountName);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        }catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccounts(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = null;

            switch (theAccountType.getValue()) {
                case "FOSA": {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                    break;
                }

                default: {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                }

            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);

            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }


            double dblUtilityETopUplMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
            double dblUtilityETopUplMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(dblUtilityETopUplMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(dblUtilityETopUplMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndMobileMoneyServices(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = null;

            switch (theAccountType.getValue()) {
                case "FOSA": {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                    break;
                }

                default: {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                }

            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_SERVICES");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elServices = doc.createElement("SERVICES");
            elAccountsAndServices.appendChild(elServices);

            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", accountNumber);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            //create element SERVICE and append to element SERVICES
            Element elServiceMpesa = doc.createElement("SERVICE");
            elServiceMpesa.setAttribute("ID", "MPESA");
            elServiceMpesa.setTextContent("Safaricom M-PESA");
            elServices.appendChild(elServiceMpesa);

            //Airtel Money
            //create element SERVICE and append to element SERVICES
            //Element elServiceAirtelMoney = doc.createElement("SERVICE");
            //elServiceAirtelMoney.setAttribute("ID", "AIRTEL");
            //elServiceAirtelMoney.setTextContent("Airtel Money");
            //elServices.appendChild(elServiceAirtelMoney);

            //Equitel Money
            //create element SERVICE and append to element SERVICES
            //Element elServiceEquitelMoney = doc.createElement("SERVICE");
            //elServiceEquitelMoney.setAttribute("ID", "EQUITEL");
            //elServiceEquitelMoney.setTextContent("Equitel Money");
            //elServices.appendChild(elServiceEquitelMoney);

            //ATM Withdrawal
            //create element SERVICE and append to element SERVICES
            //Element elServiceATM = doc.createElement("SERVICE");
            //elServiceATM.setAttribute("ID", "ATM");
            //elServiceATM.setTextContent("Withdraw Via ATM");
            //elServices.appendChild(elServiceATM);

            //Agent Withdrawal
            //create element SERVICE and append to element SERVICES
            //Element elServiceAgent = doc.createElement("SERVICE");
            //elServiceAgent.setAttribute("ID", "AGENT");
            //elServiceAgent.setTextContent("Withdraw Via Agent");
            //elServices.appendChild(elServiceAgent);


            double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
            double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(dblWithdrawalMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(dblWithdrawalMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndBanks(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{5-04-00010-02=Salary Acc (5-04-00010-02), 4-61-90010-01=Micro-cred (4-61-90010-01)}*/
            LinkedHashMap<String, String> accounts = null;

            switch (theAccountType.getValue()) {
                case "FOSA": {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                    break;
                }
                default: {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                }

            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_BANKS");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elBanks = doc.createElement("BANKS");
            elAccountsAndServices.appendChild(elBanks);


            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", accountNumber);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE);
            for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                Element elBank2 = doc.createElement("BANK");
                elBank2.setAttribute("PAYBILL_NO", serviceProviderAccount.getProviderAccountIdentifier());
                elBank2.setTextContent(serviceProviderAccount.getProviderAccountLongTag());
                elBanks.appendChild(elBank2);
            }

            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            SPManager spManager = new SPManager(strIntegritySecret);
            String strAccounts = spManager.getAllUserAccountsByProviders(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE, SPManagerConstants.UserIdentifierType.MSISDN, strUsername);
            strAccounts = strAccounts.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
            strAccounts = trimXML(strAccounts);

            if (!strAccounts.equals("<ACCOUNTS/>")) {
                InputSource sourceForPaybillAccounts = new InputSource(new StringReader(strAccounts));
                DocumentBuilderFactory builderFactoryForPaybillAccounts = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForPaybillAccounts = builderFactoryForPaybillAccounts.newDocumentBuilder();
                Document xmlDocumentForPaybillAccounts = builderForPaybillAccounts.parse(sourceForPaybillAccounts);
                XPath configXPathForPaybillAccounts = XPathFactory.newInstance().newXPath();

                NodeList nlPayBillAccounts = ((NodeList) configXPathForPaybillAccounts.evaluate("/ACCOUNTS/ACCOUNT", xmlDocumentForPaybillAccounts, XPathConstants.NODESET));

                Element elAccountsForPaybill = doc.createElement("ACCOUNTS_FOR_PAYBILL");
                for (int i = 0; i < nlPayBillAccounts.getLength(); i++) {
                    Element elSingleAccountsForPaybill = doc.createElement("PAYBILL_ACCOUNT");
                    elSingleAccountsForPaybill.setAttribute("NAME", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NAME").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("NUMBER", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NUMBER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("TYPE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_IDENTIFIER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("PROVIDER_ACCOUNT_CODE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_CODE").getTextContent());
                    elAccountsForPaybill.appendChild(elSingleAccountsForPaybill);
                }
                elAccountsAndServices.appendChild(elAccountsForPaybill);
            }

            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndPaybillServices(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = null;

            if (bFOSA) {
                accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
            } else {
                accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_PAYBILL_SERVICES");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elServices = doc.createElement("PAYBILL_SERVICES");
            elAccountsAndServices.appendChild(elServices);

            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", accountNumber);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            //create element SERVICE and append to element SERVICES
            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.UTILITY_CODE);
            Element elService;
            for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                elService = doc.createElement("SERVICE");
                elService.setAttribute("PAYBILL_NO", serviceProviderAccount.getProviderAccountIdentifier());
                elService.setAttribute("REF_NAME", serviceProviderAccount.getProviderAccountTypeTag());
                elService.setTextContent(serviceProviderAccount.getProviderAccountName());
                elServices.appendChild(elService);
            }

            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            SPManager spManager = new SPManager(strIntegritySecret);
            String strAccounts = spManager.getAllUserAccountsByProviders(SPManagerConstants.ProviderAccountType.UTILITY_CODE, SPManagerConstants.UserIdentifierType.MSISDN, strUsername);
            strAccounts = strAccounts.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
            strAccounts = trimXML(strAccounts);

            if (!strAccounts.equals("<ACCOUNTS/>")) {
                InputSource sourceForPaybillAccounts = new InputSource(new StringReader(strAccounts));
                DocumentBuilderFactory builderFactoryForPaybillAccounts = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForPaybillAccounts = builderFactoryForPaybillAccounts.newDocumentBuilder();
                Document xmlDocumentForPaybillAccounts = builderForPaybillAccounts.parse(sourceForPaybillAccounts);
                XPath configXPathForPaybillAccounts = XPathFactory.newInstance().newXPath();

                NodeList nlPayBillAccounts = ((NodeList) configXPathForPaybillAccounts.evaluate("/ACCOUNTS/ACCOUNT", xmlDocumentForPaybillAccounts, XPathConstants.NODESET));

                Element elAccountsForPaybill = doc.createElement("ACCOUNTS_FOR_PAYBILL");
                for (int i = 0; i < nlPayBillAccounts.getLength(); i++) {
                    Element elSingleAccountsForPaybill = doc.createElement("PAYBILL_ACCOUNT");
                    elSingleAccountsForPaybill.setAttribute("NAME", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NAME").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("NUMBER", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NUMBER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("TYPE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_IDENTIFIER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("PROVIDER_ACCOUNT_CODE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_CODE").getTextContent());
                    elAccountsForPaybill.appendChild(elSingleAccountsForPaybill);
                }
                elAccountsAndServices.appendChild(elAccountsForPaybill);
            }


            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static String trimXML(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MAPPResponse getTransferAccounts(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> fromAccounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");;
            LinkedHashMap<String, String> toAccounts = getBankAccounts(theMAPPRequest, "ALL");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Transfer Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elFromAccounts = doc.createElement("FROM_ACCOUNTS");
            elData.appendChild(elFromAccounts);

            Element elToAccountTypes = doc.createElement("TO_ACCOUNT_TYPES");
            Element elAccountTypeMy = doc.createElement("ACCOUNT_TYPE");
            elAccountTypeMy.setTextContent("MY Account");
            elAccountTypeMy.setAttribute("TYPE_ID", "MY_ACCOUNT");
            elToAccountTypes.appendChild(elAccountTypeMy);

            Element elAccountTypeOther = doc.createElement("ACCOUNT_TYPE");
            elAccountTypeOther.setTextContent("OTHER Account");
            elAccountTypeOther.setAttribute("TYPE_ID", "OTHER_ACCOUNT");
            elToAccountTypes.appendChild(elAccountTypeOther);
            elData.appendChild(elToAccountTypes);

            Element elToAccounts = doc.createElement("TO_ACCOUNTS");
            elData.appendChild(elToAccounts);

            for (String accountNumber : fromAccounts.keySet()) {
                String strAccountName = fromAccounts.get(accountNumber);

                Element elAccount = doc.createElement("FROM_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elFromAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }

            for (String accountNumber : toAccounts.keySet()) {
                String strAccountName = toAccounts.get(accountNumber);

                Element elAccount = doc.createElement("TO_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elToAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }

            //Option for Transfer to Other Account
            /*Element elOtherAccount = doc.createElement("TO_ACCOUNT");
            elOtherAccount.setTextContent("OTHER Account");
            elOtherAccount.setAttribute("NO", "OTHER");
            elToAccounts.appendChild(elOtherAccount);*/

            //Option for Transfer to M-PESA
            /*Element elMpesaAccount = doc.createElement("TO_ACCOUNT");
            elMpesaAccount.setTextContent("Withdraw to M-Pesa");
            elMpesaAccount.setAttribute("NO", "MPESA");
            elToAccounts.appendChild(elMpesaAccount);*/

            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoans(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            HashMap<String, HashMap<String, String>> loansInService = getLoansInService(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (loansInService != null && !loansInService.isEmpty()) {
                enDataType = MAPPConstants.ResponsesDataType.LIST;

                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);


                for (String loanTypeCode : loansInService.keySet()) {
                    String strLoanNo = loansInService.get(loanTypeCode).get("id");
                    String strLoanName = loansInService.get(loanTypeCode).get("type");
                    String strLoanBalance = loansInService.get(loanTypeCode).get("balance");

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setTextContent(strLoanName);
                    elLoan.setAttribute("SERIAL_NO", strLoanNo);
                    elLoan.setAttribute("AMOUNT", strLoanBalance);
                    elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                    elLoan.setAttribute("BALANCE", strLoanBalance);
                    elLoans.appendChild(elLoan);
                }
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse addLoanGuarantors(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            NodeList nlGuarantors = ((NodeList) configXPath.evaluate("LOAN_AND_GUARANTORS/GUARANTORS/GUARANTOR", ndRequestMSG, XPathConstants.NODESET));
            String strLoanEntryNumber = configXPath.evaluate("LOAN_AND_GUARANTORS/LOAN_ENTRY_NO", ndRequestMSG).trim();

            boolean blErrorOccured = false;

            for (int i = 0; i < nlGuarantors.getLength(); i++) {
                String strPhoneNumber = nlGuarantors.item(i).getTextContent().trim();
                //todo - Implement Integration to CBS
                //String strAdGuarantorResponse = Navision.getPort().addMobileLoanGuarantor(Integer.parseInt(strLoanEntryNumber), strPhoneNumber);
                String strAdGuarantorResponse = "SUCCESS";
                if (!strAdGuarantorResponse.equals("SUCCESS")) {
                    blErrorOccured = true;
                }
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";
            String strResponseText = "An error occurred. Please try again after a few minutes.";

            if (!blErrorOccured) {
                strTitle = "Guarantors Added Successfully";
                strResponseText = "You loan guarantors have been added successfully. Please contact the guarantors so that they can approve guarantorship.";
                strCharge = "YES";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
            } else {
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                strTitle = "ERROR: Add Loan Guarantors";
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse getMemberLoansWithPendingGuarantors(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            //todo - Implement Integration to CBS
            //String strLoansXML = Navision.getPort().getLoanPendingGuarantor(strUsername);
            String strLoansXML = "<Loans><Product><LoanNo>BLN-55740</LoanNo><ProductType>School Fees Loan</ProductType><LoanBalance>17,163.07</LoanBalance></Product><Product><LoanNo>BLN-63695</LoanNo><ProductType>BELA Loan</ProductType><LoanBalance>19,969.31</LoanBalance></Product></Loans>";

             /*
             //Response from NAV is:

            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <ProductType>School Fees Loan</ProductType>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <ProductType>BELA Loan</ProductType>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */


            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Loans</TITLE>
                    <DATA TYPE='LIST'>
                        <LOANS>
                            <LOAN NO='123456'>Moses Savings Acct</LOAN>
                            <LOAN NO='123457'>Moses Shares Acct</LOAN>
                        </LOANS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoansXML.equals("NULL")) {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlLoans = ((NodeList) configXPath.evaluate("Loan", xmlDocument, XPathConstants.NODESET));

                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);


                for (int i = 0; i < nlLoans.getLength(); i++) {
                    String strLoanNo = configXPath.evaluate("EntryNo", nlLoans.item(i)).trim();
                    String strLoanName = configXPath.evaluate("ProductName", nlLoans.item(i)).trim();
                    String strRequestedAmount = configXPath.evaluate("RequestedAmount", nlLoans.item(i)).trim();
                    String strLoanStatus = configXPath.evaluate("LoanStatus", nlLoans.item(i)).trim();

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setAttribute("ENTRY_NO", strLoanNo);
                    elLoan.setAttribute("PRODUCT_NAME", strLoanName);
                    elLoan.setAttribute("REQUESTED_AMOUNT", strRequestedAmount);
                    elLoan.setAttribute("STATUS", strLoanStatus);
                    elLoans.appendChild(elLoan);

                    NodeList nlGuarantors = ((NodeList) configXPath.evaluate("Loan/Guarantors/GuarantorDetail", xmlDocument, XPathConstants.NODESET));
                    Element elGuarantors = doc.createElement("GUARANTORS");

                    for (int j = 0; j < nlGuarantors.getLength(); j++) {
                        String strGuarantorName = APIUtils.titleCase(configXPath.evaluate("GuarantorName", nlGuarantors.item(j)).trim());
                        String strPhoneNo = configXPath.evaluate("PhoneNo", nlGuarantors.item(j)).trim();
                        String strMemberNo = configXPath.evaluate("MemberNo", nlGuarantors.item(j)).trim();
                        String strLoanGuarantorStatus = configXPath.evaluate("LoanStatus", nlGuarantors.item(j)).trim();

                        Element elGuarantor = doc.createElement("GUARANTOR");
                        elGuarantor.setAttribute("NAME", strGuarantorName);
                        elGuarantor.setAttribute("MEMBER_NO", strMemberNo);
                        elGuarantor.setAttribute("PHONE_NO", strPhoneNo);
                        elGuarantor.setAttribute("APPROVAL_STATUS", strLoanGuarantorStatus);
                        elGuarantors.appendChild(elGuarantor);
                    }
                    elLoan.appendChild(elGuarantors);
                }
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        }catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoansWithPaymentDetails(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            //todo: Add sample HashMap as documentation
            HashMap<String, HashMap<String, String>> loansInService = getLoansInService(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (loansInService != null && !loansInService.isEmpty()) {

                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);

                for (String loanTypeCode : loansInService.keySet()) {
                    String strLoanNo = loansInService.get(loanTypeCode).get("id");
                    String strLoanName = loansInService.get(loanTypeCode).get("type");
                    String strLoanBalance = loansInService.get(loanTypeCode).get("balance");

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setTextContent(strLoanName);
                    elLoan.setAttribute("SERIAL_NO", strLoanNo);
                    elLoan.setAttribute("AMOUNT", strLoanBalance);
                    elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                    elLoan.setAttribute("BALANCE", strLoanBalance);
                    elLoans.appendChild(elLoan);
                }

                Element elRepaymentOptions = doc.createElement("REPAYMENT_OPTIONS");
                //if it is not enabled then the default repayment option is savings account
                elRepaymentOptions.setAttribute("ENABLED", "TRUE");


                LinkedHashMap<String, String> FOSAAccounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");

                for (String account : FOSAAccounts.keySet()) {
                    Element elRepaymentOption1 = doc.createElement("OPTION");
                    elRepaymentOption1.setAttribute("VALUE", account);
                    elRepaymentOption1.setAttribute("TYPE", "ACCOUNT");
                    elRepaymentOption1.setTextContent(FOSAAccounts.get(account));
                    elRepaymentOptions.appendChild(elRepaymentOption1);
                }

                /*Element elRepaymentOption1 = doc.createElement("OPTION");
                elRepaymentOption1.setAttribute("VALUE", "SAVINGS_ACCOUNT");
                elRepaymentOption1.setAttribute("TYPE", "ACCOUNT");
                elRepaymentOption1.setTextContent("Savings Account");
                elRepaymentOptions.appendChild(elRepaymentOption1);*/

                Element elRepaymentOption2 = doc.createElement("OPTION");
                elRepaymentOption2.setAttribute("VALUE", "MPESA");
                elRepaymentOption2.setAttribute("TYPE", "MPESA");
                elRepaymentOption2.setTextContent("Safaricom M-Pesa");
                elRepaymentOptions.appendChild(elRepaymentOption2);

                elData.appendChild(elRepaymentOptions);

                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMaximum();

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(strMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(strMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getLoanTypes(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();


            HashMap<String, HashMap<String, String>> loanTypes = getXTremeLoanTypes(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elLoans = doc.createElement("LOANS");
            elData.appendChild(elLoans);


            for (String loanType : loanTypes.keySet()) {
                HashMap<String, String> hmLoanType = loanTypes.get(loanType);

                String strLoanTypeName = hmLoanType.get("name");
                String strLoanTypeID = hmLoanType.get("id");
                String strLoanTypeCode = hmLoanType.get("code");
                String strRequiresGuarantors = "FALSE"; /*strLoanId.equals("450") ? "TRUE" : "FALSE";*//*configXPath.evaluate("RequiresGuarantors", nlLoans.item(i)).trim();*/

                Element elLoan = doc.createElement("LOAN_TYPE");
                elLoan.setTextContent(strLoanTypeName);
                elLoan.setAttribute("ID", strLoanTypeID);
                elLoan.setAttribute("NAME", strLoanTypeName);
                elLoan.setAttribute("REQUIRES_GUARANTORS", strRequiresGuarantors);
                elLoans.appendChild(elLoan);
            }

            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            System.out.println();
            System.out.println("MSG Node for MAPP Get Loan Types:");
            System.out.println(APIUtils.nodeToString(ndResponseMSG));
            System.out.println();

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountBalanceEnquiry(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='ACCOUNT_BALANCE' VERSION='1.01'>
                      <ACCOUNT_NO>123456</ACCOUNT_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            String strProduct = "";
            String strDate = "";
            String strBookBalance = "";
            String strAvailableBalance = "";

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            String strAccountBalanceEnquiryStatus = "ERROR";
            HashMap<String, Object> hmRVal =  CBSAPI.singleAccountBalanceEnquiry(getTraceID(theMAPPRequest), strTransactionID,"MSISDN", strUsername, strPassword,"APP_ID", strAppID, "ALL", strAccountNo);

            try{
                strAccountBalanceEnquiryStatus = (String) hmRVal.get("request_status");
            }catch (Exception e){}

            if(!hmRVal.isEmpty() && strAccountBalanceEnquiryStatus.equals("SUCCESS")){
                strProduct = String.valueOf(hmRVal.get("account_name"));
                strDate = APIUtils.getCurrentDateTime();
                strBookBalance = "KES "+Utils.formatDouble(String.valueOf(hmRVal.get("account_balance")), "#,##0.00");
                strAvailableBalance = strBookBalance;
            }

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            if (!strProduct.equals("")) {
                strTitle = strProduct;
                strResponseText = "Your account balance is: <b>" + strBookBalance + "</b>" + "<br/>Available balance: <b>" + strAvailableBalance + "</b>";
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Account Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Account Balance</TITLE>
                    <DATA TYPE='TEXT'>Your account balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanBalanceEnquiry(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strCharge = "NO";
            String strLoanBalance = "";
            String strLoanName = "";

            HashMap<String, String> loanBalance = getLoansInService(theMAPPRequest, strLoanNo);
            if (!loanBalance.isEmpty()) {
                strLoanBalance = "KES "+Utils.formatDouble(loanBalance.get("balance"), "#,##0.00");
                strLoanName = loanBalance.get("type");
            }

            String strTitle = "";
            String strResponseText = "";

            if (!strLoanBalance.equals("")) {
                strTitle = "Loan Balance for: " + strLoanName;
                strResponseText = "Your loan balance is: <b>" + strLoanBalance + "</b>";
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Loan Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.END;

            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: NOT Implemented
    public MAPPResponse disableATMCard(MAPPRequest theMAPPRequest){

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath =  XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strATMCardID =  configXPath.evaluate("ATM_CARD_ID", ndRequestMSG).trim();
            String strAction =  configXPath.evaluate("ACTION", ndRequestMSG).trim();

            String strResponse = "SUCCESS";

            String strTitle= "";
            String strResponseText = "";

            if(strResponse.equals("SUCCESS")) {
                strTitle= "ATM Card Disabled";
                strResponseText = "Your request to disable ATM card "+strATMCardID+" was received successfully. You will receive an SMS confirmation shortly";
            } else {
                strTitle= "ERROR: Disable ATM Card";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.CON;

            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, "YES", strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        }catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse checkLoanLimit(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strPassword = theMAPPRequest.getPassword();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strLoanName = configXPath.evaluate("LOAN_TYPE_NAME", ndRequestMSG).trim();
            String strLoanLimit = "ERROR";

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
            HashMap<String,String> hmRVal = CBSAPI.checkLoanQualification(getTraceID(theMAPPRequest), "MSISDN", strMobileNumber, strPassword,"APP_ID", strAppID, strTransactionID, strLoanName, strLoanNo);

            String strCheckLoanQualificationStatus = hmRVal.get("request_status");

            if (strCheckLoanQualificationStatus.equals("SUCCESS")) {
                strLoanLimit = USSDAPIConstants.TransactionReturnVal.SUCCESS.getValue();
            }

            String strCharge = "NO";
            String strTitle = "";
            String strResponseText = "";

            if (strLoanLimit.equals("SUCCESS")) {
                strTitle = "Request Received Successfully";
                strResponseText = "Your loan application request was received successfully. You will receive an SMS with your loan limit.";
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Check Loan Limit";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.END;
            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse mobileMoneyWithdrawal(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {

                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());

                long lnSessionID = theMAPPRequest.getSessionID();

                String strTraceID = getTraceID(theMAPPRequest);

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strSourceAccount = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strRecipientMobileNumber = configXPath.evaluate("MOBILE_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                String strReceiverName = strMemberName;
                if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                    strReceiverName = strMemberName;
                } else {
                    strReceiverName = strRecipientMobileNumber;
                }

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                strRecipientMobileNumber = APIUtils.sanitizePhoneNumber(strRecipientMobileNumber);

                /*if (strRecipientMobileNumber.equalsIgnoreCase("INVALID_MOBILE_NUMBER")) {
                    strTitle = "ERROR: Withdrawal Failed";
                    strResponseText = "The format of the mobile number you entered is invalid (" + strEnteredMobileNumber + ")</br>Please use the format 07XX XXX XXX";
                    enResponseAction = MAPPConstants.ResponseAction.CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {

                }*/

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                }
                else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                }
                else if (Double.parseDouble(strAmount) > dblWithdrawalMax) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                }
                else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

                    String strTransaction = "Withdrawal Request";

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);

                    long getProductID = Long.parseLong(pesaParam.getProductId());

                    int intPriority = 200;
                    String strCategory = "MOBILE_MONEY_WITHDRAWAL";
                    String strAPICategory = "MPESA_WITHDRAWAL";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strTransactionID);
                    pesa.setProductID(getProductID);
                    pesa.setCategory(strCategory);
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(theMAPPRequest.getTraceID());
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strSourceAccount);
                    pesa.setSourceAccount(strSourceAccount);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceReference(strTransactionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    pesa.setReceiverName(strReceiverName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    pesa.setBeneficiaryName(strReceiverName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    //String strTransactionDescription = "Cash Withdrawal by " + strUsername + " to " + strRecipientMobileNumber;
                    String  strTransactionDescription = "Cash Withdrawal by "+strUsername+" - "+strMemberName+ " to "+strRecipientMobileNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strTransactionID);
                    pesa.setCorrelationReference(theMAPPRequest.getTraceID());
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2C);
                    pesa.setCommand("BusinessPayment");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setCategory(strCategory);
                    pesa.setPriority(intPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceReference(strTransactionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setLocalDateCreated(strDate);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strSourceAccount, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");

                    System.out.println("Withdrawal Request Result:" + strTransactionStatus);

                    switch (strTransactionStatus) {
                        case "SUCCESS": {
                            String strMSG = "";
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

                            if (PESAProcessor.sendPESA(pesa) > 0) {
                                strAmount = Utils.formatAmount(strAmount);
                                strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
                                strCharge = "YES";
                                strTitle = "Request for Withdrawal";
                                strResponseText = "Your request to withdraw <b>KES " + strAmount + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {

                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");
                                strAmount = Utils.formatAmount(strAmount);

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
                                    strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
                                }else{
                                    strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN": {
                            strTitle = "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT": {
                            strTitle = "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE": {
                            strTitle = "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS": {
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED": {
                            strTitle = "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default: {
                            System.err.println("DEFAULT ON SWITCH -> " + this.getClass().getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + strTransactionStatus);
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }

                    /*strTitle = "ERROR: Currently Unavailable";
                    strResponseText = "This service is currently unavailable, please try again later.";

                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    enResponseAction = MAPPConstants.ResponseAction.END;*/
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }

        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse buyAirtime(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS){
                String strUsername = theMAPPRequest.getUsername();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());
                String strPassword = theMAPPRequest.getPassword();

                String strTraceID = getTraceID(theMAPPRequest);

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strSourceAccountNo =  configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strRecipientMobileNumber =  configXPath.evaluate("MOBILE_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                String strReceiverName = strMemberName;
                if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                    strReceiverName = strMemberName;
                } else {
                    strReceiverName = strRecipientMobileNumber;
                }

                strRecipientMobileNumber = APIUtils.sanitizePhoneNumber(strRecipientMobileNumber);

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                double dblUtilityETopUpMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum());
                double dblUtilityETopUpMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "Please enter a valid amount for airtime purchase";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblUtilityETopUpMin) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblUtilityETopUpMin), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(Double.parseDouble(strAmount) > dblUtilityETopUpMax){
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblUtilityETopUpMax), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

                    String strTransaction = "Airtime Request";

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.AIRTIME);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    String strCategory = "AIRTIME_PURCHASE";
                    String strAPICategory = "AIRTIME_PURCHASE";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    int intPriority = 200;

                    pesa.setOriginatorID(strTransactionID);
                    pesa.setProductID(getProductID);
                    pesa.setCategory(strCategory);
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(theMAPPRequest.getTraceID());
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strSourceAccountNo);
                    pesa.setSourceAccount(strSourceAccountNo);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceReference(strTransactionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SKY_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    pesa.setReceiverName(strReceiverName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    pesa.setBeneficiaryName(strReceiverName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    //String strTransactionDescription = "Airtime Purchase by "+strRecipientMobileNumber;
                    String  strTransactionDescription = "Airtime Purchase by "+strUsername+" - "+strMemberName+ " to "+strRecipientMobileNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strTransactionID);
                    pesa.setCorrelationReference(theMAPPRequest.getTraceID());
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2C);
                    pesa.setCommand("E-TOPUP");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setPriority(intPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceReference(strTransactionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setLocalDateCreated(strDate);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");

                    System.out.println("Buy Airtime Request Result:"+strTransactionStatus);

                    switch (strTransactionStatus){
                        case "SUCCESS":{
                            String strMSG = "";
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");
                            strAmount = Utils.formatAmount(strAmount);

                            if(PESAProcessor.sendPESA(pesa) > 0){
                                strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
                                strCharge = "YES";
                                strTitle= "Request for Airtime Top-up";
                                strResponseText = "Your request to top up airtime of <b>KES "+strAmount+"</b><br/>For :<b>+"+strUsername+"</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {

                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
                                    strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
                                }else{
                                    strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN":{
                            strTitle= "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT":{
                            strTitle= "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL":{
                            strTitle= "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE":{
                            strTitle= "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS":{
                            strTitle= "ERROR: Airtime Purchase Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED":{
                            strTitle= "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default:{
                            System.err.println("DEFAULT ON SWITCH: "+this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + strTransactionStatus);
                            strTitle= "ERROR: Airtime Purchase Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }

                    /*strTitle = "ERROR: Currently Unavailable";
                    strResponseText = "This service is currently unavailable, please try again later.";

                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    enResponseAction = MAPPConstants.ResponseAction.END;*/
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse payBill(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());

                String strTraceID = getTraceID(theMAPPRequest);

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strSourceAccountNo =  configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strPaybillNo =  configXPath.evaluate("PAYBILL_NO", ndRequestMSG).trim();
                String strPaybillName =  configXPath.evaluate("PAYBILL_NAME", ndRequestMSG).trim();
                String strBillAccountNumber = configXPath.evaluate("BILL_ACCOUNT_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(Double.parseDouble(strAmount) > dblWithdrawalMax ){
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

                    String strTransaction = "Utility Request";

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    int intPriority = 200;
                    String strCategory = "BILL_PAYMENT";
                    String strAPICategory = "BILL_PAYMENT";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strTransactionID);
                    pesa.setProductID(getProductID);
                    pesa.setCategory(strCategory);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(theMAPPRequest.getTraceID());
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strSourceAccountNo);
                    pesa.setSourceAccount(strSourceAccountNo);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceReference(strTransactionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("SHORT_CODE");
                    pesa.setReceiverIdentifier(strPaybillNo);
                    pesa.setReceiverAccount(strPaybillNo);
                    pesa.setReceiverName(strPaybillName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strUsername);
                    pesa.setBeneficiaryAccount(strBillAccountNumber);
                    pesa.setBeneficiaryName(strMemberName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    //String strTransactionDescription = "B2B Bill Payment to "+strPaybillName;
                    String  strTransactionDescription = "B2B Bill Payment by "+strUsername+" - "+strMemberName+ " to "+strPaybillName + " - " + strBillAccountNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strTransactionID);
                    pesa.setCorrelationReference(theMAPPRequest.getTraceID());
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("BusinessPayBill");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setPriority(intPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceReference(strTransactionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setLocalDateCreated(strDate);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");


                    System.out.println("CBS Request Result:"+strTransactionStatus);

                    switch (strTransactionStatus){
                        case "SUCCESS":{
                            String strMSG = "";
                            strAmount = Utils.formatAmount(strAmount);
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

                            if(PESAProcessor.sendPESA(pesa) > 0){
                                strMSG = "Dear member, your Bill Payment request of KES " + strAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
                                strCharge = "YES";
                                strTitle= "Pay Bill Payment";
                                strResponseText = "Your payment of <b>KES "+strAmount+"</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
                                    strMSG = "Dear member, your Bill Payment request of KES " + strAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
                                }else{
                                    strMSG = "Dear member, your Bill Payment request of KES " + strAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN":{
                            strTitle= "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT":{
                            strTitle= "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL":{
                            strTitle= "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE":{
                            strTitle= "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS":{
                            strTitle= "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED":{
                            strTitle= "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default:{
                            System.err.println("DEFAULT ON SWITCH -> "+this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + strTransactionStatus);
                            strTitle= "ERROR: Pay Bill Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }

                    /*strTitle = "ERROR: Currently Unavailable";
                    strResponseText = "This service is currently unavailable, please try again later.";

                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    enResponseAction = MAPPConstants.ResponseAction.END;*/
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse bankTransferViaB2B(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());

                String strTraceID = getTraceID(theMAPPRequest);

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strSourceAccountNo =  configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strBank =  configXPath.evaluate("BANK", ndRequestMSG).trim();
                String strBankName =  configXPath.evaluate("BANK_NAME", ndRequestMSG).trim();
                String strReceiverBankAccountNumber =  configXPath.evaluate("BANK_ACCOUNT_NO", ndRequestMSG).trim();
                String strReceiverBankAccountName = configXPath.evaluate("BANK_ACCOUNT_NAME", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(Double.parseDouble(strAmount) > dblWithdrawalMax){
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

                    String strTransaction = "Bank Transfer Request";

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    int intPriority = 200;
                    String strCategory = "BANK_TRANSFER";
                    String strAPICategory = "BANK_TRANSFER";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strTransactionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setCategory(strCategory);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(theMAPPRequest.getTraceID());
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strSourceAccountNo);
                    pesa.setSourceAccount(strSourceAccountNo);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceReference(strTransactionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("SHORT_CODE");
                    pesa.setReceiverIdentifier(strBank);
                    pesa.setReceiverAccount(strReceiverBankAccountNumber);
                    pesa.setReceiverName(strBankName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strUsername);
                    pesa.setBeneficiaryAccount(strReceiverBankAccountNumber);
                    pesa.setBeneficiaryName(strReceiverBankAccountName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    String strTransactionDescription = "B2B Bank transfer to "+strBankName+ " A/C "+strReceiverBankAccountNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strTransactionID);
                    pesa.setCorrelationReference(theMAPPRequest.getTraceID());
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("BusinessPayBill");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setCategory(strCategory);
                    pesa.setPriority(intPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceReference(strTransactionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setLocalDateCreated(strDate);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");

                    System.out.println("CBS Request Result:"+strTransactionStatus);

                    switch (strTransactionStatus){
                        case "SUCCESS":{
                            String strMSG = "";
                            strAmount = Utils.formatAmount(strAmount);
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

                            if(PESAProcessor.sendPESA(pesa) > 0){
                                strCharge = "YES";
                                strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;;
                                strTitle= "Bank Transfer";
                                strResponseText = "Your request to transfer <b>KES "+strAmount+"</b> to has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
                                    strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;;
                                }else{
                                    strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN":{
                            strTitle= "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT":{
                            strTitle= "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL":{
                            strTitle= "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE":{
                            strTitle= "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS":{
                            strTitle= "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED":{
                            strTitle= "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default:{
                            System.err.println("DEFAULT ON SWITCH -> "+this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + strTransactionStatus);
                            strTitle= "ERROR: Bank Transfer Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }

                    /*strTitle = "ERROR: Currently Unavailable";
                    strResponseText = "This service is currently unavailable, please try again later.";

                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    enResponseAction = MAPPConstants.ResponseAction.END;*/
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    //FIXME: Pick Sender & Receiver other details from DB
    public MAPPResponse bankTransferViaPesaLink(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());

                String strTraceID = getTraceID(theMAPPRequest);

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strSourceAccountNo =  configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strBank =  configXPath.evaluate("BANK", ndRequestMSG).trim();
                String strBankName =  configXPath.evaluate("BANK_NAME", ndRequestMSG).trim();
                String strReceiverBankAccountNumber =  configXPath.evaluate("BANK_ACCOUNT_NO", ndRequestMSG).trim();
                String strReceiverBankAccountName = configXPath.evaluate("BANK_ACCOUNT_NAME", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                String strBankOtherDetails = "";
                switch(strBank){
                    case "400200": {
                        //Co-operative Bank
                        strBankOtherDetails = "<DATA><BANK_CODE>11</BANK_CODE><BRANCH_CODE>000</BRANCH_CODE></DATA>";
                        break;
                    }
                    case "522522": {
                        //KCB
                        strBankOtherDetails = "<DATA><BANK_CODE>01</BANK_CODE><BRANCH_CODE>094</BRANCH_CODE></DATA>";
                        break;
                    }
                    case "247247": {
                        //Equity Bank
                        strBankOtherDetails = "<DATA><BANK_CODE>68</BANK_CODE><BRANCH_CODE>000</BRANCH_CODE></DATA>";
                        break;
                    }
                    case "329329": {
                        //Standard Chartered Bank
                        strBankOtherDetails = "<DATA><BANK_CODE>02</BANK_CODE><BRANCH_CODE>015</BRANCH_CODE></DATA>";
                        break;
                    }
                    case "303030": {
                        //ABSA Bank
                        strBankOtherDetails = "<DATA><BANK_CODE>03</BANK_CODE><BRANCH_CODE>001</BRANCH_CODE></DATA>";
                        break;
                    }
                }

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(Double.parseDouble(strAmount) > dblWithdrawalMax){
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,##0.00");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

                    String strTransaction = "Bank Transfer Request";

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.FAMILY_BANK_PESALINK);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    int intPriority = 200;
                    String strCategory = "BANK_TRANSFER";
                    String strAPICategory = "BANK_TRANSFER";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strTransactionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(theMAPPRequest.getTraceID());
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strSourceAccountNo);
                    pesa.setSourceAccount(strSourceAccountNo);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceReference(strTransactionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("ACCOUNT_NO");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    //TODO: Set this in XML
                    pesa.setSenderOtherDetails(strBankOtherDetails);

                    pesa.setReceiverType("ACCOUNT_NO");
                    pesa.setReceiverIdentifier(strReceiverBankAccountNumber);
                    pesa.setReceiverAccount(strReceiverBankAccountNumber);
                    pesa.setReceiverName(strReceiverBankAccountName);
                    //TODO: For test only, change later
                    pesa.setReceiverOtherDetails(strBankOtherDetails);

                    pesa.setBeneficiaryType("ACCOUNT_NO");
                    pesa.setBeneficiaryIdentifier(strReceiverBankAccountNumber);
                    pesa.setBeneficiaryAccount(strReceiverBankAccountNumber);
                    pesa.setBeneficiaryName(strReceiverBankAccountName);
                    pesa.setBeneficiaryOtherDetails(strBankOtherDetails);

                    String strTransactionDescription = "PesaLink Bank transfer to "+strBankName+ " A/C "+strReceiverBankAccountNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strTransactionID);
                    pesa.setCorrelationReference(theMAPPRequest.getTraceID());
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("PESALINK");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setCategory(strCategory);
                    pesa.setPriority(intPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceReference(strTransactionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setLocalDateCreated(strDate);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");

                    System.out.println("CBS Request Result:"+strTransactionStatus);

                    switch (strTransactionStatus){
                        case "SUCCESS":{
                            String strMSG = "";
                            strAmount = Utils.formatAmount(strAmount);
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

                            if(PESAProcessor.sendPESA(pesa) > 0){
                                strCharge = "YES";
                                strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;;
                                strTitle= "Bank Transfer";
                                strResponseText = "Your request to transfer <b>KES "+strAmount+"</b> to has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
                                    strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;;
                                }else{
                                    strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN":{
                            strTitle= "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT":{
                            strTitle= "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL":{
                            strTitle= "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE":{
                            strTitle= "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS":{
                            strTitle= "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED":{
                            strTitle= "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default:{
                            System.err.println("DEFAULT ON SWITCH -> "+this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + strTransactionStatus);
                            strTitle= "ERROR: Bank Transfer Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse depositMoney(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);
            String strSender = pesaParam.getSenderIdentifier();
            /*
            <MSG SESSION_ID='12234' ORG_ID='12' TYPE='MOBILE_BANKING' ACTION='DEPOSIT_MONEY' VERSION='1.01'>"+
                <AMOUNT ACCOUNT_NO='1234567890'>1000</AMOUNT>
            </MSG>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strAccountNo =  configXPath.evaluate("AMOUNT/@ACCOUNT_NO", ndRequestMSG).trim();
            String strAmount =  configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
            BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

            String strReceiver = strUsername;
            String strReceiverDetails = strReceiver;
            double lnAmount = Utils.stringToDouble(strAmount);
            String strReference = strUsername;

            boolean blPesaStkPushStatus = false;

            PESAAPI thePESAAPI = new PESAAPI();
            blPesaStkPushStatus = thePESAAPI.pesa_C2B_Request(
                    strTransactionID,
                    theMAPPRequest.getTraceID(),
                    strReceiver,
                    strReceiverDetails,
                    strAccountNo,
                    "KES",
                    lnAmount,
                    "DEPOSIT",
                    strReference,
                    "MAPP",
                    "MBANKING"
            );

            String strResponseText = "";
            String strTitle= "";
            String strCharge = "NO";

            if(blPesaStkPushStatus){
                strTitle = "Deposit Request";
                strResponseText = "You will be prompted by M-PESA for payment<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strAccountNo + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";
            } else {
                strTitle = "ERROR: Deposit Request";
                strResponseText = "Use the details below to pay via M-PESA<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strAccountNo + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";

                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            //End USSD.

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanRepayment(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("loanRepayment");
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);
            String strSender = pesaParam.getSenderIdentifier();
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='PAY_LOAN' VERSION='1.01'>
                    <AMOUNT LOAN_SERIAL_NO='12345'>123456</AMOUNT>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            Node ndRequestMSG = theMAPPRequest.getMSG();

            System.out.println();
            System.out.println(MBankingAPIUtils.serializeXMLDocNode(ndRequestMSG));
            System.out.println();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanId =  configXPath.evaluate("AMOUNT/@LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strAmount =  configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
            String strRepaymentOption =  configXPath.evaluate("REPAYMENT_OPTION", ndRequestMSG).trim();

            System.out.println("Payment option: "+strRepaymentOption);

            switch (strRepaymentOption){
                case "MPESA": {
                    String strReceiver = strUsername;
                    String strReceiverDetails = strReceiver;
                    double lnAmount = Utils.stringToDouble(strAmount);
                    //String strReference = strUsername;

                    boolean blPesaStkPushStatus = false;

                    //Generate temp account to send to M-PESA
                    String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmssSSS");
                    strTempAccount = APIUtils.convertToBase36(strTempAccount);
                    XTremeDBCache.store(strTempAccount, strLoanId);

                    PESAAPI thePESAAPI = new PESAAPI();
                    blPesaStkPushStatus = thePESAAPI.pesa_C2B_Request(
                            strTransactionID,
                            theMAPPRequest.getTraceID(),
                            strReceiver,
                            strReceiverDetails,
                            strTempAccount,
                            "KES",
                            lnAmount,
                            "LOAN_REPAYMENT",
                            strTransactionID,
                            "MAPP",
                            "MBANKING"
                    );

                    String strResponseText = "";
                    String strTitle= "";
                    String strCharge = "NO";

                    if(blPesaStkPushStatus){
                        strTitle = "Deposit Request";
                        strResponseText = "You will be prompted by M-PESA for payment<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strLoanId + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";
                    } else {
                        strTitle = "ERROR: Deposit Request";
                        strResponseText = "Use the details below to pay via M-PESA<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strLoanId + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";

                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    }

                    //End USSD.

                    Element elData = doc.createElement("DATA");
                    elData.setTextContent(strResponseText);

                    generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                    //Response
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                    theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
                    break;
                }
                //case "Savings Account": {
                default: {

                    String strTransactionReference = strTransactionID;
                    String strSourceAccount = strRepaymentOption;
                    String strDestinationAccount = strLoanId;

                    String strTraceID = getTraceID(theMAPPRequest);
                    String strTransactionDescription = "Loan Repayment. Source A/C: "+strSourceAccount+" - Destination A/C: "+strDestinationAccount;

                    String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

                    HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                            strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strTransactionID,
                            "MBANKING_SERVER", "MAPP", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);
                    String strRequestStatus = hmRVal.get("transaction_status");
                    String strRequestStatusDescription = hmRVal.get("transaction_status_description");
                    String strFundsTransferStatus = strRequestStatus;

                    String strTitle = "";
                    String strResponseText = "";

                    String strCharge = "NO";

                    switch (strFundsTransferStatus) {
                        case "SUCCESS": {
                            strTitle= "Transaction Accepted";
                            strResponseText = "Your loan repayment request has been accepted successfully. Kindly wait as it is being processed";
                            strCharge = "YES";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                            break;
                        }
                        case "ERROR": {
                            strTitle= "Transaction Error";
                            strResponseText = "An error occurred while making your request for funds transfer. Please try again.";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle= "Insufficient Balance";
                            strResponseText = "Error, you do not have sufficient balance in your account to complete this request";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        case "ACCOUNT_NOT_FOUND":
                        case "ACC_NOT_FOUND": {
                            strTitle= "Account Not Found";
                            strResponseText = "Error, your account could not be found, please try again";
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        default: {
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                            strTitle= "ERROR: Loan Repayment";
                            strResponseText = "An error occurred. Please try again after a few minutes.";
                        }
                    }

                    Element elData = doc.createElement("DATA");
                    elData.setTextContent(strResponseText);

                    generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                    //Response
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                    theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
                    break;
                }
            }
        } catch (Exception e){
            System.out.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"()");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fundsTransfer(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try{
            System.out.println("fundsTransfer");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath =  XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                //Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = theMAPPRequest.getAppID();

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                  // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                String strFromAccountNo = configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strToAccountNo = configXPath.evaluate("TO_ACCOUNT_NO", ndRequestMSG).trim();
                String strToOption = configXPath.evaluate("TRANSFER_OPTION", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                String strAccountNo = strToAccountNo;

                //TODO: Ask Isaac for best way

                if(!(strToOption.equals("Account") || strToOption.equals("Account Number"))){
                    HashMap<Object, Object> accountDetails = getUserDetails(theMAPPRequest, strToOption, strAccountNo);

                    HashMap<String, HashMap <String, String>>  hmIFTDestAccounts = (HashMap<String, HashMap <String, String>>) accountDetails.get("accounts");
                    HashMap<String, String>  hmMemberDetails = (HashMap<String, String>) accountDetails.get("user_details");

                    if (hmMemberDetails != null && !hmMemberDetails.isEmpty()) {
                        strAccountNo = hmIFTDestAccounts.entrySet().iterator().next().getValue().get("number");
                    }
                }

                //END

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                String strTransactionReference = strTransactionID;
                String strSourceAccount = strFromAccountNo;
                String strDestinationAccount = strAccountNo;

                String strTraceID = getTraceID(theMAPPRequest);
                String strTransactionDescription = "Internal Funds Transfer. Source A/C: "+strSourceAccount+" - Destination A/C: "+strDestinationAccount;

                String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

                HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                        strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strTransactionID,
                        "MBANKING_SERVER", "MAPP", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);
                String strRequestStatus = hmRVal.get("transaction_status");
                String strRequestStatusDescription = hmRVal.get("transaction_status_description");

                String strFundsTransferStatus = strRequestStatus;

                String strTitle= "";
                String strResponseText = "";

                String strCharge = "NO";

                switch (strFundsTransferStatus) {
                    case "SUCCESS": {
                        strTitle= "Transaction Accepted";
                        strResponseText = "Your funds transfer request has been accepted successfully. Kindly wait as it is being processed";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        break;
                    }
                    case "ERROR": {
                        strTitle= "Transaction Error";
                        strResponseText = "An error occurred while making your request for funds transfer. Please try again.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "INSUFFICIENT_BAL": {
                        strTitle= "Insufficient Balance";
                        strResponseText = "Error, you do not have sufficient balance in your account to complete this request";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "ACC_NOT_FOUND": {
                        strTitle= "Account Not Found";
                        strResponseText = "Error, your account could not be found, please try again";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        strTitle= "ERROR: Funds Transfer";
                        strResponseText = "An error occurred. Please try again after a few minutes.";
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountStatement(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim()+" 00:00:00";
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim()+" 23:59:59";
            String statementType = "FULL_STATEMENT";
            int intMaxNumberOfTransactions = 100;
            String strAccountMiniStatementStatus = "ERROR";
            MAPPConstants.AccountType accountType = MAPPConstants.AccountType.FOSA;

            if(!strAccountNo.contains("-")){
                accountType = MAPPConstants.AccountType.BOSA;
            }

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
            HashMap<Object, Object> hmAccountStatementRVal = CBSAPI.accountMiniStatement(getTraceID(theMAPPRequest), strTransactionID,"MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                    statementType, intMaxNumberOfTransactions, strStartDate, strEndDate, accountType.getValue(), strAccountNo);

            LinkedHashMap<String, HashMap <String, String>>  hmAccountStatementTransactions = (LinkedHashMap<String, HashMap <String, String>>) hmAccountStatementRVal.get("transactions");
            HashMap<String, String>  hmAccountStatementDetails = (HashMap<String, String>) hmAccountStatementRVal.get("request_details");

            strAccountMiniStatementStatus = hmAccountStatementDetails.get("request_status");

            String strTitle = "Account Statement";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element elData = doc.createElement("DATA");
            String strCharge = "NO";

            if(strAccountMiniStatementStatus.equals("SUCCESS")){
                String strAvailableBalance = "KES "+Utils.formatDouble(hmAccountStatementDetails.get("available_balance"), "#,##0.00");
                if (hmAccountStatementTransactions != null && !hmAccountStatementTransactions.isEmpty()) {
                    strCharge = "YES";

                    Element elBalance = doc.createElement("BALANCE");
                    elBalance.setTextContent(strAvailableBalance);
                    elData.appendChild(elBalance);

                    Element elTable = doc.createElement("TABLE");
                    elData.appendChild(elTable);

                    Element elTrHeading = doc.createElement("TR");
                    elTable.appendChild(elTrHeading);

                    Element elThHeading1 = doc.createElement("TH");
                    elThHeading1.setTextContent("Description");
                    elTrHeading.appendChild(elThHeading1);

                    Element elThHeading2 = doc.createElement("TH");
                    elThHeading2.setTextContent("Amount");
                    elTrHeading.appendChild(elThHeading2);

                    Element elThHeading3 = doc.createElement("TH");
                    elThHeading3.setTextContent("Date");
                    elTrHeading.appendChild(elThHeading3);

                    Element elThHeading4 = doc.createElement("TH");
                    elThHeading4.setTextContent("Reference");
                    elTrHeading.appendChild(elThHeading4);

                    Element elThHeading5 = doc.createElement("TH");
                    elThHeading5.setTextContent("Running Bal");
                    elTrHeading.appendChild(elThHeading5);

                    for (String index : hmAccountStatementTransactions.keySet()) {
                        HashMap<String, String> hmTransaction = hmAccountStatementTransactions.get(index);
                        String strDate = hmTransaction.get("transaction_date_time");
                        String strDesc = hmTransaction.get("transaction_description");
                        String strAmount = "KES "+Utils.formatDouble(hmTransaction.get("transaction_amount"), "#,##0.00");
                        String strReference = hmTransaction.get("transaction_reference");
                        String strBalance = "KES "+Utils.formatDouble(hmTransaction.get("running_balance"), "#,##0.00");

                        Element elTrBody = doc.createElement("TR");
                        elTable.appendChild(elTrBody);

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strDesc);
                        elTrBody.appendChild(elTDBody1);

                        Element elTDBody2 = doc.createElement("TD");
                        elTDBody2.setTextContent(strAmount);
                        elTrBody.appendChild(elTDBody2);

                        Element elTDBody3 = doc.createElement("TD");
                        elTDBody3.setTextContent(strDate);
                        elTrBody.appendChild(elTDBody3);

                        Element elTDBody4 = doc.createElement("TD");
                        elTDBody4.setTextContent(strReference);
                        elTrBody.appendChild(elTDBody4);

                        Element elTDBody5 = doc.createElement("TD");
                        elTDBody5.setTextContent(strBalance);
                        elTrBody.appendChild(elTDBody5);
                    }
                } else {
                    strCharge = "NO";
                    strTitle = "No Statements Found";
                    elData.setTextContent("You do not have any statements within this time period");
                }
            } else {
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                strCharge = "NO";
                strTitle= "ERROR: Account Statement";
                elData.setTextContent("An error occurred. Please try again after a few minutes.");
            }


             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */


            // Root element - MSG


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse changePassword(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try{
            System.out.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath =  XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strNewPassword =  configXPath.evaluate("NEW_PASSWORD", ndRequestMSG).trim();

            HashMap<String,String> hmRVal = CBSAPI.changeUserPIN(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strNewPassword);
            String strChangePinStatus = hmRVal.get("change_pin_status");
            String strChangePinStatusDescription = hmRVal.get("change_pin_status_description");

            String strTitle= "";
            String strResponseText = "";

            String strCharge = "NO";

            switch (strChangePinStatus) {
                case "SUCCESS": {
                    strTitle= "Password Changed Successfully";
                    strResponseText = "Your password has been changed successfully. You will be redirected to the login page.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "INVALID_NEW_PIN": {
                    strTitle= "Invalid New Password";
                    strResponseText = "Please ensure new PIN and its confirmation match and try again";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                case "INCORRECT_PIN": {
                    strTitle= "Incorrect PIN";
                    strResponseText = "Error, the PIN you have entered as current PIN is incorrect, please try again";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    strTitle= "Account Not Found";
                    strResponseText = "Error, your account could not be found, please try again";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle= "ERROR: Change Password";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                }
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse encryptText(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strClearText = configXPath.evaluate("CLEARTEXT", ndRequestMSG).trim();
            String strTimestamp = configXPath.evaluate("TIMESTAMP", ndRequestMSG).trim();

            String strEncryptedText = strClearText;

            strEncryptedText = crypto.encrypt(APIUtils.ENCRYPTION_KEY + strTimestamp, strClearText);

            String strTitle = strTitle = "Text Encrypted Successfully";
            String strResponseText = strResponseText = "Text was encrypted successfully.";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            Element elEncrypted = doc.createElement("ENCRYPTED");
            elEncrypted.setTextContent(strEncryptedText);
            elData.appendChild(elEncrypted);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse decryptText(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strEncrypted = configXPath.evaluate("ENCRYPTED", ndRequestMSG).trim();
            String strTimestamp = configXPath.evaluate("TIMESTAMP", ndRequestMSG).trim();

            String strDecryptedText = strEncrypted;

            strDecryptedText = crypto.decrypt(APIUtils.ENCRYPTION_KEY + strTimestamp, strEncrypted);

            String strTitle = strTitle = "Text Encrypted Successfully";
            String strResponseText = strResponseText = "Text was encrypted successfully.";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            String[] arStrDecryptedText = strDecryptedText.split("\\|");
            //FUNDS_TRANSFER|254722554433|JOHN DOE|100.00|1593884595611
            //QR_CODE_TYPE|PHONE_NO|FULL_NAME|AMOUNT|ACCOUNT
            //Amount should not have commas

            String strName = arStrDecryptedText[2];
            String strAccountNumber = "";
            String strAccountName = " ";
            String strPhoneNumber = arStrDecryptedText[1];
            String strAmount = arStrDecryptedText[3];
            Element elAccountDetails = null;

            String strType = arStrDecryptedText[0];
            switch (strType) {
                case "CASH_WITHDRAWAL":
                case "BUY_AIRTIME": {
                    elAccountDetails = getAccountElement(theMAPPRequest, strPhoneNumber, "Mobile", doc, "ENCRYPTION");
                    break;
                }
                case "DEPOSIT_MONEY":
                case "FUNDS_TRANSFER": {
                    strAccountNumber = arStrDecryptedText[4];
                    elAccountDetails = getAccountElement(theMAPPRequest, strAccountNumber, "ACCOUNT", doc, "ENCRYPTION");
                    break;
                }
                default: {
                    elAccountDetails = getAccountElement(theMAPPRequest, strPhoneNumber, "Mobile", doc, "ENCRYPTION");
                    break;
                }
            }

            if (elAccountDetails != null) {
                strName = elAccountDetails.getAttribute("NAME");
                strAccountNumber = elAccountDetails.getAttribute("ACCOUNT_NO");
                strAccountName = elAccountDetails.getAttribute("ACCOUNT_NAME");
                strPhoneNumber = elAccountDetails.getAttribute("PHONE_NO");
            }

            strDecryptedText = strType + "|" + strPhoneNumber + "|" + strName + "|" + strAmount + "|" + strAccountNumber + "|" + strAccountName;

            Element elEncrypted = doc.createElement("DECRYPTED");
            elEncrypted.setTextContent(strDecryptedText);
            elData.appendChild(elEncrypted);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".decryptText() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse addOrDeleteUtilityAndPaybillAccount(MAPPRequest theMAPPRequest, String theAction) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"()");

            XPath configXPath =  XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strProviderAccountCode =  configXPath.evaluate("PROVIDER_ACCOUNT_CODE", ndRequestMSG).trim();
            String strName =  configXPath.evaluate("ACCOUNT_NAME", ndRequestMSG).trim();
            String strNumber =  configXPath.evaluate("ACCOUNT_NUMBER", ndRequestMSG).trim();

            String strServiceProviderID = null;
            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            LinkedList<LinkedHashMap<String, String>> linkedHashMapLinkedList = new SPManager(strIntegritySecret).getSPAccounts(SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES);

            for (LinkedHashMap<String, String> stringStringLinkedHashMap : linkedHashMapLinkedList) {
                if (stringStringLinkedHashMap.get("provider_account_identifier").equalsIgnoreCase(strProviderAccountCode)) {
                    strServiceProviderID = stringStringLinkedHashMap.get("provider_account_code");
                    break;
                }
            }

            boolean blFundsTransferStatus;

            if(theAction.equalsIgnoreCase("ADD")){
                long lnFundsTransferStatus = new SPManager(strIntegritySecret).createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN, strUsername, strServiceProviderID, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO, strNumber, strName);
                blFundsTransferStatus = lnFundsTransferStatus > 0;
            } else {
                blFundsTransferStatus = new SPManager(strIntegritySecret).removeUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN, strUsername, strServiceProviderID, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO, strNumber);
            }

            String strTitle= "";
            String strResponseText = "";

            String strCharge = "NO";

            if (blFundsTransferStatus) {
                strTitle = "Success";
                strResponseText = "Success.";
                strCharge = "YES";
            } else {
                strTitle = "Error";
                strResponseText = "Error";
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberName(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try{
            System.out.println("getMemberName");

            XPath configXPath =  XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.OBJECT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strOption =  configXPath.evaluate("OPTION", ndRequestMSG).trim();
            String strAccount =  configXPath.evaluate("ACCOUNT", ndRequestMSG).trim();

            String strAccountNumberXML = "";
            String strSource = "Mobile";

            if(strOption.equals("ID Number")){
                strSource = "ID";
            } else if (strOption.equals("Account Number")) {
                strSource = "ACCOUNT";
            } else if (strOption.equals("Member Number")) {
                strSource = "MEMBER_NO";
            } else {
                strAccount = APIUtils.sanitizePhoneNumber(strAccount);
            }

            String strTitle = "Account Details";

            String strCharge = "NO";
            Element elAccountDetails = getAccountElement(theMAPPRequest, strAccount, strSource, doc, "GET_MEMBER_NAME");

            Element elData = doc.createElement("DATA");

            elData.appendChild(elAccountDetails);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public Element getAccountElement(MAPPRequest theMAPPRequest, String theAccount, String theSource, Document doc, String theCategory) {
        try {
            //theSource (from MAPPAPI) -> Mobile / ID Number / Account Number / Member Number
            //theSource (to XTremeAPI) -> MEMBER_NUMBER / ID_NUMBER / ACCOUNT_NUMBER / MOBILE_NUMBER
            switch (theSource) {
                case "ID": {
                    theSource = "NATIONAL_ID";
                    break;
                }

                case "ACCOUNT": {
                    theSource = "ACCOUNT_NUMBER";
                    break;
                }

                case "Member Number": {
                    theSource = "MEMBER_NUMBER";
                    break;
                }

                case "Mobile":
                default: {
                    theSource = "MSISDN";
                    break;
                }
            }
            HashMap<Object, Object> accountDetails = getUserDetails(theMAPPRequest, theSource, theAccount);

            HashMap<String, HashMap <String, String>>  hmIFTDestAccounts = (HashMap<String, HashMap <String, String>>) accountDetails.get("accounts");
            HashMap<String, String>  hmMemberDetails = (HashMap<String, String>) accountDetails.get("user_details");

            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strIDNumber = "";
            String strAccountStatus = "NOT_FOUND";

            if (hmMemberDetails != null && !hmMemberDetails.isEmpty()) {
                strAccountStatus = "FOUND";
                strAccountNo = hmIFTDestAccounts.entrySet().iterator().next().getValue().get("number");
                strAccountType = hmIFTDestAccounts.entrySet().iterator().next().getValue().get("type_name");
                strAccountMemberNo = hmMemberDetails.get("member_number");
                strAccountName = hmMemberDetails.get("full_name");
                strPhoneNo = hmMemberDetails.get("identifier");
                strIDNumber = hmMemberDetails.get("identity");
                strAccountName = Utils.toTitleCase(strAccountName);
            }

            if(theCategory != null){
                if(theCategory.equals("VALIDATE_PESA_IN")){
                    elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                    Element elKYCDetails = doc.createElement("KYC_DETAILS");
                    elPesaOtherDetails.appendChild(elKYCDetails);

                    Element elKYCResponse = doc.createElement("RESPONSE");
                    elKYCDetails.appendChild(elKYCResponse);

                    Element elKYC = doc.createElement("KYC");
                    elKYC.setAttribute("TYPE", theSource);
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
                } else {
                    elPesaOtherDetails = doc.createElement("ACCOUNT");
                    elPesaOtherDetails.setAttribute("STATUS", strAccountStatus);
                    elPesaOtherDetails.setAttribute("ACCOUNT_NO", strAccountNo);
                    elPesaOtherDetails.setAttribute("ACCOUNT_NAME", strAccountType);
                    elPesaOtherDetails.setAttribute("NAME", strAccountName);
                    elPesaOtherDetails.setAttribute("MEMBER_NO", strAccountMemberNo);
                    elPesaOtherDetails.setAttribute("PHONE_NO", strPhoneNo);
                }
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MAPPResponse applyLoan(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("applyLoan");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus =MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                //Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = theMAPPRequest.getAppID();
                long lnSessionID = theMAPPRequest.getSessionID();
                String strGUID = UUID.randomUUID().toString();

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                String strLoanID = configXPath.evaluate("LOAN_TYPE_ID", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                String strLoanApplicationMaximum = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMaximum();

                NodeList nlGuarantors = ((NodeList) configXPath.evaluate("GUARANTORS", ndRequestMSG, XPathConstants.NODESET));

                for (int i = 0; i < nlGuarantors.getLength(); i++) {
                    String strName = configXPath.evaluate("GUARANTOR/@NAME", nlGuarantors.item(i)).trim();
                    String strMobileNumber = configXPath.evaluate("GUARANTOR/@MOBILE_NUMBER", nlGuarantors.item(i)).trim();
                    //todo: Add Guarantors here
                    //Navision.getPort().addMobileLoanGuarantor(Integer.parseInt(strLoanID), strMobileNumber);
                }

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());

                String strEntryNo = UUID.randomUUID().toString().toUpperCase();
                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strTitle = "";
                String strResponseText = "";

                String strCharge = "NO";

                String strLoanApplicationStatus = "ERROR";
                String strLoanApplicationStatusDescription = "ERROR";

                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
                String strRequestApplication = "MBANKING_SERVER";
                String strSourceApplication = "MAPP";
                String strTransactionDateTime = APIUtils.getCurrentDateTime();

                HashMap<String,String> hmRVal = CBSAPI.loanApplication(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strTransactionID,
                        strLoanID, strAmount, strTransactionID, strRequestApplication, strSourceApplication, strTransactionDateTime);
                strLoanApplicationStatus = hmRVal.get("request_status");

                switch (strLoanApplicationStatus) {
                    case "SUCCESS": {
                        strTitle = "Request Received Successfully";
                        strResponseText = "Your loan application request was received successfully. You will receive an SMS once the loan has been approved.";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        break;
                    }
                    case "INCORRECT_PIN": {
                        strTitle = "Incorrect PIN";
                        strResponseText = "Error, the PIN you have entered as current PIN is incorrect, please try again";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "LOAN_APPLICATION_EXISTS": {
                        strTitle = "Loan Already Exists";
                        strResponseText = "The loan you applied for already exists, please repay the current loan to apply for another one.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        strTitle = "ERROR: Apply Loan";
                        strResponseText = "An error occurred. Please try again after a few minutes.";
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanStatement(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();
            int intMaxNumberOfTransactions = 100;
            String statementType = "FULL_STATEMENT";

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim()+" 00:00:00";
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim()+" 23:59:59";

            String strLoanMinistatementStatus = "ERROR";

            HashMap<Object, Object> hmLoanStatementsRVal = CBSAPI.loanMiniStatement(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword, "APP_ID", strAppID,
                    statementType, intMaxNumberOfTransactions, strStartDate, strEndDate, strLoanNo);

            LinkedHashMap<String, HashMap<String, String>> hmLoanStatementTransactions = (LinkedHashMap<String, HashMap<String, String>>) hmLoanStatementsRVal.get("transactions");
            HashMap<String, String> hmLoanStatementDetails = (HashMap<String, String>) hmLoanStatementsRVal.get("request_details");

            strLoanMinistatementStatus = hmLoanStatementDetails.get("request_status");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Statement";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (strLoanMinistatementStatus.equals("SUCCESS")) {
                String strLoanBalance = "KES "+Utils.formatDouble(hmLoanStatementDetails.get("loan_balance"), "#,##0.00");
                if (hmLoanStatementTransactions != null && !hmLoanStatementTransactions.isEmpty()) {
                    Element elBalance = doc.createElement("BALANCE");
                    elBalance.setTextContent(strLoanBalance);
                    elData.appendChild(elBalance);

                    Element elTable = doc.createElement("TABLE");
                    elData.appendChild(elTable);


                    Element elTrHeading = doc.createElement("TR");
                    elTable.appendChild(elTrHeading);

                    Element elThHeading1 = doc.createElement("TH");
                    elThHeading1.setTextContent("Description");
                    elTrHeading.appendChild(elThHeading1);

                    Element elThHeading2 = doc.createElement("TH");
                    elThHeading2.setTextContent("Amount");
                    elTrHeading.appendChild(elThHeading2);

                    Element elThHeading3 = doc.createElement("TH");
                    elThHeading3.setTextContent("Date");
                    elTrHeading.appendChild(elThHeading3);

                    Element elThHeading4 = doc.createElement("TH");
                    elThHeading4.setTextContent("Ref");
                    elTrHeading.appendChild(elThHeading4);

                    Element elThHeading5 = doc.createElement("TH");
                    elThHeading5.setTextContent("Balance");
                    elTrHeading.appendChild(elThHeading5);

                    for (String index : hmLoanStatementTransactions.keySet()) {
                        HashMap<String, String> hmTransaction = hmLoanStatementTransactions.get(index);
                        String strDate = hmTransaction.get("transaction_date_time");
                        String strDesc = hmTransaction.get("transaction_description");
                        String strAmount = "KES "+Utils.formatDouble(hmTransaction.get("transaction_amount"), "#,##0.00");
                        String strReference = hmTransaction.get("transaction_reference");
                        String strBalance = "KES "+Utils.formatDouble(hmTransaction.get("running_balance"), "#,##0.00");

                        Element elTrBody = doc.createElement("TR");
                        elTable.appendChild(elTrBody);

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strDesc);
                        elTrBody.appendChild(elTDBody1);

                        Element elTDBody2 = doc.createElement("TD");
                        elTDBody2.setTextContent(strAmount);
                        elTrBody.appendChild(elTDBody2);

                        Element elTDBody3 = doc.createElement("TD");
                        elTDBody3.setTextContent(strDate);
                        elTrBody.appendChild(elTDBody3);

                        Element elTDBody4 = doc.createElement("TD");
                        elTDBody4.setTextContent(strReference);
                        elTrBody.appendChild(elTDBody4);

                        Element elTDBody5 = doc.createElement("TD");
                        elTDBody5.setTextContent(strBalance);
                        elTrBody.appendChild(elTDBody5);
                    }
                } else {
                    strCharge = "NO";
                    strTitle = "No Statements Found";
                    elData.setTextContent("No loan transactions found for the specified loan");
                }
            } else {
                strCharge = "NO";
                strTitle= "ERROR: Loan Statement";
                elData.setTextContent("An error occurred. Please try again after a few minutes.");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanGuarantors(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();


            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());

            String strLoansXML = "";

            System.out.println("NAV Returned: " + strLoansXML);

            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlTransactions = ((NodeList) configXPath.evaluate("Loan/Security", xmlDocument, XPathConstants.NODESET));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Guarantors";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent("Sorry, this service is NOT available.");

            /*String strLoanBalanceXML = Navision.getPort().getMemberLoanListMobileApp(strUsername);
            InputSource sourceForBalance = new InputSource(new StringReader(strLoanBalanceXML));
            DocumentBuilderFactory builderFactoryForBalance = DocumentBuilderFactory.newInstance();
            DocumentBuilder builderForBalance = builderFactoryForBalance.newDocumentBuilder();
            Document xmlDocumentForBalance = builderForBalance.parse(sourceForBalance);

            NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocumentForBalance, XPathConstants.NODESET));

            String strLoanBalance = "";

            for (int i = 0; i < nlLoans.getLength(); i++) {
                String strLoanId = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                String strLoanBalanceForLoan = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                if (strLoanId.equals(strLoanNo)) {
                    strLoanBalance = strLoanBalanceForLoan;
                    break;
                }
            }

            Element elBalance = doc.createElement("BALANCE");
            elBalance.setTextContent(strLoanBalance);
            elData.appendChild(elBalance);

            Element elTable = doc.createElement("TABLE");
            elData.appendChild(elTable);


            Element elTrHeading = doc.createElement("TR");
            elTable.appendChild(elTrHeading);

            Element elThHeading1 = doc.createElement("TH");
            elThHeading1.setTextContent("Name");
            elTrHeading.appendChild(elThHeading1);

            Element elThHeading2 = doc.createElement("TH");
             elThHeading2.setTextContent("Amount Guaranteed");
            elTrHeading.appendChild(elThHeading2);

            Element elThHeading4 = doc.createElement("TH");
            elThHeading4.setTextContent("Mobile No");
            elTrHeading.appendChild(elThHeading4);

            Element elThHeading3 = doc.createElement("TH");
            elThHeading3.setTextContent("Loan No");
            elTrHeading.appendChild(elThHeading3);

            Element elThHeading5 = doc.createElement("TH");
            elThHeading5.setTextContent("Current Commitment");
            elTrHeading.appendChild(elThHeading5);

            Element elThHeading6 = doc.createElement("TH");
            elThHeading6.setTextContent("Type");
            elTrHeading.appendChild(elThHeading6);

            for (int i = 0; i < nlTransactions.getLength(); i++) {
                String strName = configXPath.evaluate("Name", nlTransactions.item(i)).trim();
                String strAmountGuaranteed = configXPath.evaluate("AmountGuaranteed", nlTransactions.item(i)).trim();
                String strLoanNumber = configXPath.evaluate("LoanNo", nlTransactions.item(i)).trim();
                String strMobileNo = configXPath.evaluate("MobileNo", nlTransactions.item(i)).trim();
                String strCurrentCommitment = configXPath.evaluate("CurrentCommitment", nlTransactions.item(i)).trim();
                String strType = configXPath.evaluate("Type", nlTransactions.item(i)).trim();


                Element elTrBody = doc.createElement("TR");
                elTable.appendChild(elTrBody);

                Element elTDBody1 = doc.createElement("TD");
                elTDBody1.setTextContent(strName);
                elTrBody.appendChild(elTDBody1);

                Element elTDBody2 = doc.createElement("TD");
                elTDBody2.setTextContent("KES "+strAmountGuaranteed);
                elTrBody.appendChild(elTDBody2);

                Element elTDBody4 = doc.createElement("TD");
                elTDBody4.setTextContent(strMobileNo);
                elTrBody.appendChild(elTDBody4);

                Element elTDBody3 = doc.createElement("TD");
                elTDBody3.setTextContent(strLoanNumber);
                elTrBody.appendChild(elTDBody3);

                Element elTDBody5 = doc.createElement("TD");
                elTDBody5.setTextContent("KES "+strCurrentCommitment);
                elTrBody.appendChild(elTDBody5);

                Element elTDBody6 = doc.createElement("TD");
                elTDBody6.setTextContent(strType);
                elTrBody.appendChild(elTDBody6);
            }*/

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loansGuaranteed(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());

            String strLoansXML = "";
            if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                strLoansXML = "";
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Guarantors";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent("Sorry, this service is NOT available.");
            /*System.out.println("NAV Returned: " + strLoansXML);

            if (strLoansXML.equalsIgnoreCase("") || strLoansXML.equalsIgnoreCase("NULL")) {
                if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                    elData.setTextContent("There were no loan found");
                } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                    elData.setTextContent("There were no loan guarantorship requests found");
                }
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            } else {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlTransactions = ((NodeList) configXPath.evaluate("/", xmlDocument, XPathConstants.NODESET));
                NodeList nlTransaction = ((NodeList) configXPath.evaluate("/", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                    nlTransactions = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET));
                    nlTransaction = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                    nlTransactions = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET));
                    nlTransaction = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                }
            */

            /*<Security>
                <Loan>
                    <LoanNo>BLN-50367</LoanNo>
                    <Loanee>Abdalla Said Aden</Loanee>
                    <MobileNo>+254725683351</MobileNo>
                    <LoanType>Development Loan</LoanType>
                    <GuarantorType>Guarantor</GuarantorType>
                    <IssuedDate>02/24/16</IssuedDate>
                    <EndDate>04/24/20</EndDate>
                    <Status>Performing</Status>
                    <LoanAmount>300,000</LoanAmount>
                    <Installments>48</Installments>
                    <LoanBalance>148,770</LoanBalance>
                    <DefaultedAmount>0</DefaultedAmount>
                    <AmountGuaranteed>0</AmountGuaranteed>
                    <CurrentCommitment>0</CurrentCommitment>
                </Loan>
            </Security>*/

            /*

                Element elTable = doc.createElement("TABLE");
                elData.appendChild(elTable);


                Element elTrHeading = doc.createElement("TR");
                elTable.appendChild(elTrHeading);

                for (int k = 0; k < nlTransaction.getLength(); k++) {
                    String strHeadingName = nlTransactions.item(0).getChildNodes().item(k).getNodeName();
                    strHeadingName = splitCamelCase(strHeadingName);
                    Element elThHeading1 = doc.createElement("TH");
                    elThHeading1.setTextContent(strHeadingName);
                    elTrHeading.appendChild(elThHeading1);
                }

                for (int i = 0; i < nlTransactions.getLength(); i++) {
                    Element elTrBody = doc.createElement("TR");
                    elTable.appendChild(elTrBody);

                    for (int j = 0; j < nlTransactions.item(i).getChildNodes().getLength(); j++) {
                        String strBodyValue = "";

                        if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                            strBodyValue = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET)).item(i).getChildNodes().item(j).getTextContent();//.item(j).getNodeValue();
                        } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                            strBodyValue = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET)).item(i).getChildNodes().item(j).getTextContent();//.item(j).getNodeValue();
                        }

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strBodyValue);
                        elTrBody.appendChild(elTDBody1);
                    }
                }
            }*/

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse updateLoanGuarantorStatus(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strStatus = configXPath.evaluate("STATUS", ndRequestMSG).trim();

            boolean blApproved = strStatus.equalsIgnoreCase("APPROVED");

            String strNavResponse = "";

            String strTitle = "Guarantorship";
            String strResponseText = "";

            String strCharge = "NO";

            /*if (strNavResponse.equals("SUCCESS")) {
                strTitle = strStatus.equals("APPROVED") ? "Guarantorship Approved" : "Guarantorship Rejected";
                strResponseText = strStatus.equals("APPROVED") ? "Your request to <b>approve</b> loan guarantorship was received successfully" : "Your request to <b>reject</b> loan guarantorship was received successfully";
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                strCharge = "YES";
            } else {
                strTitle = "ERROR";
                strResponseText = "An error occurred. Please try again after a few minutes.";
            }*/

            Element elData = doc.createElement("DATA");
            elData.setTextContent("Sorry, this service is NOT available.");
            //elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static String getResponseStatus(String strXML) {
        String strStatus = "";
        try {
            if (!strXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlResponse = ((NodeList) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                strStatus = nlResponse.item(0).getTextContent();
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getResponseStatus() ERROR : " + e.getMessage());
        }
        return strStatus;
    }

    public MAPPAmountLimitParam getParam(MAPPAPIConstants.MAPP_PARAM_TYPE theMAPPParamType) {
        MAPPAmountLimitParam rVal = new MAPPAmountLimitParam();
        try {
            String strMAPPParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/AMOUNT_LIMITS";

            switch (theMAPPParamType) {
                case CASH_WITHDRAWAL: {
                    strMAPPParamType += "/CASH_WITHDRAWAL";
                    break;
                }
                case AIRTIME_PURCHASE: {
                    strMAPPParamType += "/AIRTIME_PURCHASE";
                    break;
                }
                case PAY_BILL: {
                    strMAPPParamType += "/PAY_BILL";
                    break;
                }
                case EXTERNAL_FUNDS_TRANSFER: {
                    strMAPPParamType += "/EXTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case INTERNAL_FUNDS_TRANSFER: {
                    strMAPPParamType += "/INTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case DEPOSIT: {
                    strMAPPParamType += "/DEPOSIT";
                    break;
                }
                case APPLY_LOAN: {
                    strMAPPParamType += "/APPLY_LOAN";
                    break;
                }
                case PAY_LOAN: {
                    strMAPPParamType += "/PAY_LOAN";
                    break;
                }
            }

            String strMinimum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MAPP, strMAPPParamType + "/MIN_AMOUNT");
            String strMaximum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MAPP, strMAPPParamType + "/MAX_AMOUNT");

            rVal.setMinimum(strMinimum);
            rVal.setMaximum(strMaximum);
        } catch (Exception e) {
            System.err.println("MAPPAPI.getParam() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public HashMap<Object, Object> getUserDetails(MAPPRequest theMAPPRequest, String identifierType, String identifier){
        HashMap<Object, Object> hmRVal = null;
        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strPassword = theMAPPRequest.getPassword();

            if(identifierType.equalsIgnoreCase("Mobile No") || identifierType.equals("MSISDN")){
                identifierType = "MSISDN";
            }else if(identifierType.equalsIgnoreCase("ID Number") || identifierType.equals("ID") || identifierType.equals("NATIONAL_ID")){
                identifierType = "NATIONAL_ID";
            }else if(identifierType.equalsIgnoreCase("Member Number") || identifierType.equals("MEMBER_NUMBER")){
                identifierType = "MEMBER_NUMBER";
            }else if(identifierType.equalsIgnoreCase("Account Number") || identifierType.equals("Account") || identifierType.equals("ACCOUNT") || identifierType.equals("ACCOUNT_NUMBER")){
                identifierType = "ACCOUNT_NUMBER";
            }else{
                identifierType = "MSISDN";
            }

            hmRVal = CBSAPI.getUserDetails(getTraceID(theMAPPRequest), "MSISDN", strMobileNumber, strPassword,"APP_ID", strAppID, identifierType, identifier);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return hmRVal;
    }

    private LinkedHashMap<String, String> getBankAccounts(MAPPRequest theMAPPRequest, String accountType) {
        LinkedHashMap<String, String> accounts = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strAccountType = accountType;
            String strPassword = theMAPPRequest.getPassword();

            LinkedHashMap<String, LinkedHashMap <String, String>> bankAccounts =  CBSAPI.getBankAccounts(getTraceID(theMAPPRequest),
                    "MSISDN", strMobileNumber, strPassword,"APP_ID", strAppID, strAccountType);

            for(String bankAccountNumber : bankAccounts.keySet()){
                accounts.put(bankAccountNumber, bankAccounts.get(bankAccountNumber).get("label"));
            }


        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return accounts;
    }

    public HashMap<String, HashMap<String, String>> getLoansInService(MAPPRequest theMAPPRequest) {
        HashMap<String, HashMap<String, String>> loans = new HashMap<>();
        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strPassword = theMAPPRequest.getPassword();

            String strGetLoans = "ERROR";
            HashMap<String, Object> hmRVal =  CBSAPI.getLoansInService(getTraceID(theMAPPRequest), "MSISDN", strMobileNumber, strPassword,"APP_ID", strAppID);

            try{
                strGetLoans = (String) hmRVal.get("request_status");
                loans = (HashMap<String, HashMap<String, String>>) hmRVal.get("loans");
            }catch (Exception e){}

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public HashMap<String, String> getLoansInService(MAPPRequest theMAPPRequest, String theLoanNo) {
        HashMap<String, String> loanInService = new HashMap<>();
        HashMap<String, HashMap<String, String>> loansInService = getLoansInService(theMAPPRequest);
        try {
            for (String loanTypeCode : loansInService.keySet()) {
                String strLoanNo = loansInService.get(loanTypeCode).get("id");
                if (strLoanNo.equals(theLoanNo)){
                    loanInService = loansInService.get(loanTypeCode);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loanInService;
    }

    public HashMap<String, HashMap <String, String>>  getXTremeLoanTypes(MAPPRequest theMAPPRequest) {
        HashMap <String, String> loan_details = new HashMap<>();
        HashMap<String, HashMap <String, String>> loan_types = new HashMap<>();
        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strPassword = theMAPPRequest.getPassword();

            System.out.println("Calling CBS to get MAPP loatypes...");
            HashMap<Object, Object> hmRVal = CBSAPI.getLoanTypes(getTraceID(theMAPPRequest), "MSISDN", strMobileNumber, strPassword,"APP_ID", strAppID);

            loan_details = (HashMap <String, String>) hmRVal.get("loan_details");

            if(loan_details!=null){
                if(loan_details.get("request_status").equalsIgnoreCase("SUCCESS")){
                    loan_types = (HashMap<String, HashMap <String, String>>) hmRVal.get("loan_types");
                }
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Loan Types Gotten:");
        System.out.println(loan_types);
        return loan_types;
    }

    public MAPPResponse mobileMoneyWithdrawalFloatPurchase(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;
        System.out.println("HERE");

        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {

                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
//                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                long lnSessionID = theMAPPRequest.getSessionID();
                String strAppID = String.valueOf(theMAPPRequest.getAppID());

                String strTraceID = theMAPPRequest.getTraceID();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMemberName = getUserFullName(theMAPPRequest, strUsername);
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);
                String strDateTime = MBankingDB.getDBDateTime().trim();
                int intPESAPriority = 200;

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                /* <MSG ACTION="CASH_WITHDRAWAL" PARAMETERS_VERSION="1.20082" PRODUCT_ID="5" SEQ="5" SERVER_ID="100201" SESSION_ID="43417315" SESSION_KEY="9880cfc1-a4ee-11f0-ba41-f2a2b77b3e48" TRACE_ID="9880f569-a4ee-11f0-ba41-f2a2b77b3e48" TYPE="MOBILE_BANKING">
 [INFO]:  [2025-10-09 09:04:21.440]  <FROM_ACCOUNT_NO>1003957</FROM_ACCOUNT_NO>
 [INFO]:  [2025-10-09 09:04:21.440]  <AGENT_NUMBER>093019</AGENT_NUMBER>
 [INFO]:  [2025-10-09 09:04:21.440]  <AGENT_NAME>test</AGENT_NAME>
 [INFO]:  [2025-10-09 09:04:21.440]  <STORE_NUMBER>1526</STORE_NUMBER>
 [INFO]:  [2025-10-09 09:04:21.440]  <AMOUNT>10</AMOUNT>
 [INFO]:  [2025-10-09 09:04:21.440]  </MSG>
*/

                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();

                String strAccountFrom = configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strAgentNumber = configXPath.evaluate("AGENT_NUMBER", ndRequestMSG).trim();
                String strAgentName = configXPath.evaluate("AGENT_NAME", ndRequestMSG).trim();
                String strStoreNumber = configXPath.evaluate("STORE_NUMBER", ndRequestMSG).trim();

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

           /*     strRecipientMobileNumber = APIUtils.sanitizePhoneNumber(strRecipientMobileNumber);

                if (strRecipientMobileNumber.equals("INVALID_MOBILE_NUMBER")) {
                    return null;
                }*/

               /* double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());
*/
                double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.MPESA_FLOAT_PURCHASE).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.MPESA_FLOAT_PURCHASE).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Float Purchase";
                    strResponseText = "Please enter a valid amount for Float Purchase";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Float Purchase";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) > dblWithdrawalMax) {
                    strTitle = "ERROR: Float Purchase";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {

                    //TODO: Confirm if a new Transaction Type should be created in CBD
                    String strTransaction = "Float Purchase Request";
                    String strDate = MBankingDB.getDBDateTime().trim();

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_FLOAT_PURCHASE);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    String strCategory = "FLOAT_PURCHASE";
                    String strAPICategory = "FLOAT_PURCHASE";

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    PESA pesa = new PESA();

                    pesa.setOriginatorID(strMAPPSessionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setCategory(strCategory);
                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDateTime);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strMobileNumber);
                    pesa.setInitiatorAccount(strMobileNumber);
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strAccountFrom);
                    pesa.setSourceAccount(strAccountFrom);
                    pesa.setSourceName(strMemberName);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("TILL_NUMBER");
                    pesa.setReceiverIdentifier(strAgentNumber);
                    pesa.setReceiverAccount(strAgentNumber);
                    pesa.setReceiverName(strAgentName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("TILL_NUMBER");
                    pesa.setBeneficiaryIdentifier(strStoreNumber);
                    pesa.setBeneficiaryAccount(strStoreNumber);
                    pesa.setBeneficiaryName(strAgentName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    String strTransactionDescription = "MPESA Float Purchase to "+strAgentName+ " Agent No. "+strAgentNumber+" - Store No. "+strStoreNumber;
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("BusinessDeposit");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

                    pesa.setCategory(strCategory);
                    pesa.setPriority(intPESAPriority);
                    pesa.setSendCount(0);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setPESAXMLData("<OTHER_DETAILS/>");

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDateTime);
                    pesa.setPesaDateCreated(strDateTime);
                    pesa.setLocalDateCreated(strDateTime);

                    HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID, strMAPPSessionID,
                            pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
                            pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
                            pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                            strAccountFrom, strAmount, strAPICategory, strTransactionDescription, strTraceID, "MBANKING_SERVER", "MAPP", strDate);

                    String strTransactionStatus = hmRVal.get("transaction_status");
                    String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
                    String strTransactionDateTime = hmRVal.get("transaction_date_time");

                    System.out.println("Withdrawal Request Result:" + strTransactionStatus);

                    switch (strTransactionStatus) {
                        case "SUCCESS": {
                            String strMSG = "";
                            String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

                            if (PESAProcessor.sendPESA(pesa) > 0) {
                                strAmount = Utils.formatAmount(strAmount);
                                strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strMAPPSessionID;
                                strCharge = "YES";
                                strTitle = "Request for Withdrawal";
                                strResponseText = "Your request to withdraw <b>KES " + strAmount + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {

                                HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strMAPPSessionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
                                        pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
                                        "", strDate);

                                String strResultTransactionStatus = hmRValResult.get("transaction_status");
                                String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
                                String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");
                                strAmount = Utils.formatAmount(strAmount);

                                if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
//                                    strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
                                }else{
//                                    strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
                                }

                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;
                            }
                            break;
                        }
                        case "INCORRECT_PIN": {
                            strTitle = "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT": {
                            strTitle = "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE": {
                            strTitle = "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS": {
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED": {
                            strTitle = "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default: {
                            System.err.println("DEFAULT ON SWITCH -> " + this.getClass().getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + strTransactionStatus);
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }

                    /*strTitle = "ERROR: Currently Unavailable";
                    strResponseText = "This service is currently unavailable, please try again later.";

                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    enResponseAction = MAPPConstants.ResponseAction.END;*/
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse validateBusinessShortCode(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;
        HashMap<Object, Object> hmAPIRVal = null;

        /*
        *<MSG ACTION="VALIDATE_BUSINESS_SHORT_CODE" PARAMETERS_VERSION="1.00052" PRODUCT_ID="2" SEQ="8" SERVER_ID="100201" SESSION_ID="38088614" SESSION_KEY="27df2b33-5364-11f0-816d-f2a2b77b3e48" TRACE_ID="27df465c-5364-11f0-816d-f2a2b77b3e48" TYPE="MOBILE_BANKING">
            <BUSINESS_SHORT_CODE>2100014</BUSINESS_SHORT_CODE>
            <IDENTIFIER TYPE="SHORTCODE">2100014</IDENTIFIER>
        </MSG>
        *
         */

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strUsername = theMAPPRequest.getUsername();
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strTraceID = getTraceID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = TEXT;
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strTitle = "Business Short Code";
            String strResponseText = "";
            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            String strBusinessAccount = configXPath.evaluate("BUSINESS_SHORT_CODE", ndRequestMSG).trim();
            hmAPIRVal = CBSAPI.verifyBusinessShortCode(strTraceID, "MSISDN", strUsername,"APP_ID", strAppID, "MSISDN", strUsername, strBusinessAccount);

            String requestStatus = String.valueOf(hmAPIRVal.get("request_status"));

            if(requestStatus.equals("SUCCESS")){
                elData.setTextContent(String.valueOf(hmAPIRVal.get("business_name")));
            }else {
                enResponseStatus = ERROR;
                elData.setTextContent("Invalid business account.");
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, enResponseStatus, strCharge, strTitle, enDataType);

            // Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return theMAPPResponse;
    }

    public MAPPResponse lipaNa(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = String.valueOf(theMAPPRequest.getAppID());

            long lnSessionID = theMAPPRequest.getSessionID();

            String strTraceID = getTraceID(theMAPPRequest);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            // String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);
            String strSender = pesaParam.getSenderIdentifier();

            MAPPConstants.ResponsesDataType enDataType = TEXT;
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strTitle = "Lipa Na Vikash";
            String strResponseText = "";
            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            /*<?xml version='1.0'?>
            <MESSAGES DATE_TIME="2025-06-27 18:06:44" VERSION="1.06">
                <LOGIN APP_ID="6a1e6980-535c-11f0-b823-a73607613bcd" PASSWORD="1212" USERNAME="254790342037" />
                <MSG ACTION="LIPA_NA" PARAMETERS_VERSION="1.00052" PRODUCT_ID="2"
                    SESSION_ID="38090783" SESSION_KEY="3fffc9f8-5368-11f0-816d-f2a2b77b3e48" TYPE="MOBILE_BANKING">
                    <ACCOUNT_NO>68260200481901</ACCOUNT_NO>
                    <SHOP_NUMBER>2000011</SHOP_NUMBER>
                    <SHOP_NAME>Speedtech Connections</SHOP_NAME>
                    <AMOUNT>100</AMOUNT>
                </MSG>
            </MESSAGES>
             */
            String strBusinessShortCode = configXPath.evaluate("BUSINESS_SHORT_CODE", ndRequestMSG).trim();
            String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
            String strPaymentOption = configXPath.evaluate("PAYMENT_OPTION", ndRequestMSG).trim();


            String strDepositMinimum = getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMinimum();
            String strDepositMaximum = getParam(ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMinimum();

            double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
            double dblDepositMaximum = Double.parseDouble(strDepositMaximum);


            double dblAmountEntered = Double.parseDouble(strAmount);

            if (dblAmountEntered < dblDepositMinimum || dblAmountEntered > dblDepositMaximum) {
                if (dblAmountEntered < dblDepositMinimum) {
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(strDepositMinimum, "#,###.##");
                    enResponseStatus = ERROR;
                }

                if (dblAmountEntered > dblDepositMaximum) {
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(strDepositMaximum, "#,###.##");
                    enResponseStatus = ERROR;
                }
            }  else {
                strResponseText = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + "\n" + "A/C: " + strBusinessShortCode + "\n" + "Amount: KES " + strAmount + "\n";
                String strOriginatorID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, lnSessionID, theMAPPRequest.getSequence());

                Thread worker = new Thread(() -> {
                    PESAAPI thePESAAPI = new PESAAPI();
                    thePESAAPI.pesa_C2B_BUY_GOODS_Request(
                            strOriginatorID,
                            theMAPPRequest.getTraceID(),
                            String.valueOf(theMAPPRequest.getUsername()),
                            String.valueOf(theMAPPRequest.getUsername()),
                            strBusinessShortCode,
                            "KES",
                            dblAmountEntered,
                            "BUY_GOODS",
                            strTraceID,
                            "MAPP",
                            "MBANKING_SERVER");

                });
                worker.start();
            }
           /* else if (strPaymentOption.equals("SAVINGS")) {
                String strSourceAccount = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strDestinationAccount = strBusinessShortCode;

                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                String strTransactionReference = strTransactionID;

                String strTransactionDescription = "Internal Funds Transfer. Source A/C: "+strSourceAccount+" - Destination A/C: "+strDestinationAccount;

                String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

                HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                        strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strTransactionID,
                        "MBANKING_SERVER", "MAPP", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);
                String strRequestStatus = hmRVal.get("transaction_status");
                String strRequestStatusDescription = hmRVal.get("transaction_status_description");

                String strFundsTransferStatus = strRequestStatus;

                strCharge = "NO";

                switch (strFundsTransferStatus) {
                    case "SUCCESS": {
                        strTitle= "Transaction Accepted";
                        strResponseText = "Your funds transfer request has been accepted successfully. Kindly wait as it is being processed";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        break;
                    }
                    case "ERROR": {
                        strTitle= "Transaction Error";
                        strResponseText = "An error occurred while making your request for funds transfer. Please try again.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "INSUFFICIENT_BAL": {
                        strTitle= "Insufficient Balance";
                        strResponseText = "Error, you do not have sufficient balance in your account to complete this request";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "ACC_NOT_FOUND": {
                        strTitle= "Account Not Found";
                        strResponseText = "Error, your account could not be found, please try again";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        strTitle= "ERROR: Funds Transfer";
                        strResponseText = "An error occurred. Please try again after a few minutes.";
                    }
                }
            }
            else {
                strResponseText = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + "\n" + "A/C: " + strBusinessShortCode + "\n" + "Amount: KES " + strAmount + "\n";
                String strOriginatorID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, lnSessionID, theMAPPRequest.getSequence());

                Thread worker = new Thread(() -> {
                    PESAAPI thePESAAPI = new PESAAPI();
                    thePESAAPI.pesa_C2B_BUY_GOODS_Request(
                            strOriginatorID,
                            theMAPPRequest.getTraceID(),
                            String.valueOf(theMAPPRequest.getUsername()),
                            String.valueOf(theMAPPRequest.getUsername()),
                            strBusinessShortCode,
                            "KES",
                            dblAmountEntered,
                            "BUY_GOODS",
                            strTraceID,
                            "USSD",
                            "MBANKING_SERVER");

                });
                worker.start();
            }
*/
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, enResponseStatus, strCharge, strTitle, enDataType);

            // Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getLipaNaVikashAccounts(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

           /* boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            LinkedHashMap<String, String> accounts = null;

            switch (theAccountType.getValue()) {
                case "FOSA": {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                    break;
                }
                default: {
                    accounts = getBankAccounts(theMAPPRequest, "WITHDRAWABLE");
                }

            }*/

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);

            Element elPayOptions = doc.createElement("PAYMENT_OPTIONS");
            elData.appendChild(elPayOptions);

           /* for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", accountNumber);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }*/

            Element elOption1 = doc.createElement("OPTION");
            elOption1.setTextContent("M-Pesa");
            elPayOptions.appendChild(elOption1);

            Attr attr = doc.createAttribute("NO");
            attr.setValue("M-PESA");
            elOption1.setAttributeNode(attr);

           /* Element elOption2 = doc.createElement("OPTION");
            elOption2.setTextContent("Savings Account");
            elPayOptions.appendChild(elOption2);

            Attr attr1 = doc.createAttribute("NO");
            attr1.setValue("SAVINGS");
            elOption2.setAttributeNode(attr1);
*/

            double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
            double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(dblWithdrawalMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(dblWithdrawalMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public String getTraceID(MAPPRequest theMAPPRequest){
        //return theMAPPRequest.getTraceID(); //+APIUtils.getCurrentDate("yyyyMMddHHmmssSSS");
        return UUID.randomUUID().toString().toLowerCase();
    }

    public static void printXmlFromNode(Node node) {
        try {
            // Create a transformer
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Set transformer properties to format the output
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            // Create a StringWriter to hold the output
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            // Transform the node to the StringWriter
            transformer.transform(new DOMSource(node), result);

            // Print the formatted XML
            System.out.println(writer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
