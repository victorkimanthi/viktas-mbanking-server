package ke.skyworld.mbanking.ussdapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.msg.MSGProcessor;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.register.MemberRegisterResponse;
import ke.skyworld.lib.mbanking.register.RegisterConstants;
import ke.skyworld.lib.mbanking.register.RegisterProcessor;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.sp.manager.SPManagerConstants;
import ke.skyworld.sp.manager.SPManager;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

public class APIUtils {
    public APIUtils(){
    }

    public final static long ONE_SECOND = 1000;
    public final static long SECONDS = 60;

    public final static long ONE_MINUTE = ONE_SECOND * 60;
    public final static long MINUTES = 60;

    public final static long ONE_HOUR = ONE_MINUTE * 60;
    public final static long HOURS = 24;

    public final static long ONE_DAY = ONE_HOUR * 24;

    public static String ENCRYPTION_KEY = "6l04zjBa*iuGSv6l(2akwfqA";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    public static Object toHashMap(String objStr, TypeReference T){
        ObjectMapper objectMapper = new ObjectMapper();
        Map map = new HashMap();
        try {
            map = objectMapper.readValue(objStr, T);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String serialize(Object obj){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static HashMap<String, String[]> getXmlStringV2(String strLoansXML){

        HashMap<String, String[]> loans = new HashMap<>();

        try{
            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlLoans = ((NodeList) configXPath
                    .evaluate("/Loans", xmlDocument, XPathConstants.NODESET))
                    .item(0).getChildNodes();

            for (int i = 0; i < nlLoans.getLength(); i++) {
                NodeList nlLoan = ((NodeList) configXPath
                        .evaluate("Product", nlLoans, XPathConstants.NODESET))
                        .item(i).getChildNodes();

                loans.put(nlLoan.item(2).getTextContent(),
                        new String[]{
                                nlLoan.item(0).getTextContent(),
                                nlLoan.item(1).getTextContent(),
                                nlLoan.item(3).getTextContent()
                        });
            }
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            e.printStackTrace();
        }
        return loans;
    }

    /*public static String sanitizePhoneNumber(String thePhoneNumber){
        thePhoneNumber = thePhoneNumber.trim();
        try {
            if(thePhoneNumber.startsWith("+")){
                thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
            }

            if(thePhoneNumber.matches("^2547\\d{8}$")){
                return thePhoneNumber;
            }

            if(thePhoneNumber.matches("^07\\d{8}$")) {
                return thePhoneNumber.replaceFirst("^0", "254");
            }

            if(thePhoneNumber.matches("^7\\d{8}$")){
                return "254"+thePhoneNumber;
            }

            return "INVALID MOBILE NUMBER";
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }*/

    public static String sanitizePhoneNumber(String thePhoneNumber){
        thePhoneNumber = thePhoneNumber.replaceAll("\\s","");
        thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
        try {
            if(thePhoneNumber.startsWith("+")){
                thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
            }

            if(thePhoneNumber.matches("^2547\\d{8}$") || thePhoneNumber.matches("^2541\\d{8}$")){
                return thePhoneNumber;
            }

            if(thePhoneNumber.matches("^07\\d{8}$") || thePhoneNumber.matches("^01\\d{8}$")) {
                return thePhoneNumber.replaceFirst("^0", "254");
            }

            if(thePhoneNumber.matches("^7\\d{8}$") || thePhoneNumber.matches("^1\\d{8}$")){
                return "254"+thePhoneNumber;
            }

            if(thePhoneNumber.matches("^25407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25407", "2547");
            }

            if(thePhoneNumber.matches("^25401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25401", "2541");
            }

            if(thePhoneNumber.matches("^254\\+254\\d{9}$")){
                return thePhoneNumber.replaceFirst("^254\\+254", "254");
            }

            if(thePhoneNumber.matches("^254254\\d{9}$")){
                return thePhoneNumber.replaceFirst("^254254", "254");
            }

            if(thePhoneNumber.matches("^254\\+25401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^254\\+25401", "2541");
            }

            if(thePhoneNumber.matches("^254\\+25407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^254\\+25407", "2547");
            }

            if(thePhoneNumber.matches("^25425401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25425401", "2541");
            }

            if(thePhoneNumber.matches("^25425407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25425407", "2547");
            }
            return "INVALID_MOBILE_NUMBER";
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /*
    NAV Specific Function
    public static void hashPINsOnNAV() {
        try {
            String strClearTextPINXML = Navision.getPort().getUnhashedPINs();

            //System.out.println(strClearTextPINXML);

            if (!strClearTextPINXML.equals("ERROR")) {
                InputSource source = new InputSource(new StringReader(strClearTextPINXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList ndAccounts = ((NodeList) configXPath.evaluate("/ACCOUNTS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                double lnStartTime = (double) System.currentTimeMillis();
                for (int i = 0; i < ndAccounts.getLength(); i++) {
                    String strPhoneNumber = ndAccounts.item(i).getAttributes().getNamedItem("PHONE_NUMBER").getTextContent();
                    String strAccountNumber = ndAccounts.item(i).getAttributes().getNamedItem("ACCOUNT_NUMBER").getTextContent();
                    String strPIN = ndAccounts.item(i).getAttributes().getNamedItem("PIN").getTextContent();

                    System.out.println("Count: " + (i + 1));
                    System.out.println("Account Number: " + strAccountNumber);
                    System.out.println("Phone Number: " + strPhoneNumber);
                    System.out.println("Cleartext PIN: " + strPIN);

                    String strHashedPIN = hashPIN(strPIN);
                    //System.out.println("Hashed PIN: " + strHashedPIN);

                    String strResult = Navision.getPort().setHashedPIN(strAccountNumber, strPhoneNumber, strHashedPIN);
                    //System.out.println("RESULT: " + strResult + "\n");
                }
                double lnEndTime = (double) System.currentTimeMillis();
                double lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                //System.out.println("Finished Task In " + lnTimeTaken + " Seconds");
            }
        } catch (Exception e) {
            System.err.println("USSDAPI.hashPINsOnNAV() ERROR : " + e.getMessage());
            //hashPINsOnNAV();
        } finally {
        }
    }
    */
    public static String millisToLongDHMS(long duration) {
        StringBuffer res = new StringBuffer();
        long temp = 0;
        boolean hasDay = false;
        boolean hasHasHour = false;
        boolean hasMinute = false;
        if (duration >= ONE_SECOND) {
            temp = duration / ONE_DAY;
            if (temp > 0) {
                hasDay = true;
                duration -= temp * ONE_DAY;
                res.append(temp).append(" day").append(temp > 1 ? "s" : "")
                        .append(duration >= ONE_MINUTE ? ", " : "");
            }

            temp = duration / ONE_HOUR;
            if (temp > 0) {
                hasHasHour = true;
                duration -= temp * ONE_HOUR;
                res.append(temp).append(" hour").append(temp > 1 ? "s" : "")
                        .append(duration >= ONE_MINUTE ? ", " : "");
            }

            if(!hasDay){
                temp = duration / ONE_MINUTE;
                if (temp > 0) {
                    hasMinute = true;
                    duration -= temp * ONE_MINUTE;
                    res.append(temp).append(" minute").append(temp > 1 ? "s" : "");
                }
            }

            /*if (!res.toString().equals("") && duration >= ONE_SECOND) {
                res.append(" and ");
            }

            temp = duration / ONE_SECOND;
            if (temp > 0) {
                res.append(temp).append(" second").append(temp > 1 ? "s" : "");
            }*/
            return res.toString();
        } else {
            return "0 second";
        }
    }

    public static Date convertDateStringToDate(String date) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(SPManagerConstants.DEFAULT_DATE_TIME_FORMAT);
        try {
            return (date == null) ? null : simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getPrettyDateTimeDifference(Date startDate, Date endDate) {
        String seconds = "second";
        String minutes = "minute";
        String hours = "hour";
        String days = "day";
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
        if (elapsedSeconds > 1) seconds = seconds + "s";
        if (elapsedMinutes > 1) minutes = minutes + "s";
        if (elapsedHours > 1) hours = hours + "s";
        if (elapsedDays > 1) days = days + "s";
        if (elapsedDays <= 0) {
            if (elapsedHours <= 0) {
                if (elapsedMinutes <= 0) {
                    if (elapsedSeconds <= 0) {
                        return "3 seconds";
                    } else {
                        return String.format("%d " + seconds + "%n", elapsedSeconds);
                    }
                } else {
                    if (elapsedSeconds > 0) {
                        return String.format("%d " + minutes + ", %d " + seconds + "%n",
                                elapsedMinutes, elapsedSeconds);
                    } else {
                        return String.format("%d " + minutes + "%n", elapsedMinutes);
                    }
                }
            } else {
                if (elapsedMinutes > 0) {
                    return String.format("%d " + hours + ", %d " + minutes + "%n",
                            elapsedHours, elapsedMinutes);
                } else {
                    return String.format("%d " + hours + "%n", elapsedHours);
                }
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d " + days + ", %d " + hours + "%n",
                        elapsedDays, elapsedHours);
            } else {
                return String.format("%d " + days + "%n", elapsedDays);
            }
        }
    }


    public static String getPrettyDateTimeDifferenceRoundedUp(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
        if (elapsedDays > 0) {
            if (elapsedHours > 0) {
                elapsedDays += 1;
            }
            String days = (elapsedDays == 1) ? "DAY" : "DAYS";
            return String.format("%d " + days + "%n", elapsedDays);
        } else {
            //Days 0. Do for hours
            if (elapsedHours > 0) {
                if (elapsedMinutes > 0) {
                    elapsedHours += 1;
                }
                String hours = (elapsedHours == 1) ? "HOUR" : "HOURS";
                return String.format("%d " + hours + "%n", elapsedHours);
            } else {
                //Hours 0. Do for minutes
                if (elapsedMinutes > 0) {
                    if (elapsedSeconds > 0) {
                        elapsedMinutes += 1;
                    }
                    String minutes = (elapsedMinutes == 1) ? "MINUTE" : "MINUTES";
                    return String.format("%d " + minutes + "%n", elapsedMinutes);
                } else {
                    return "1 MINUTE";
                }
            }
        }
    }

    public static String titleCase(String inputString) {
        if (StringUtils.isBlank(inputString)) {
            return "";
        }

        if (StringUtils.length(inputString) == 1) {
            return inputString.toUpperCase();
        }

        StringBuffer resultPlaceHolder = new StringBuffer(inputString.length());

        Stream.of(inputString.split(" ")).forEach(stringPart ->
        {
            if (stringPart.length() > 1)
                resultPlaceHolder.append(stringPart.substring(0, 1)
                        .toUpperCase())
                        .append(stringPart.substring(1)
                                .toLowerCase());
            else
                resultPlaceHolder.append(stringPart.toUpperCase());

            resultPlaceHolder.append(" ");
        });
        return StringUtils.trim(resultPlaceHolder.toString());
    }

    public static String fnModifyMAPPSessionIDBkp(String theSessionID) {
        try {
            ZonedDateTime nowZoned = ZonedDateTime.now();
            Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
            Duration duration = Duration.between(midnight, Instant.now());
            long seconds = duration.getSeconds();
            return theSessionID + "_" + String.format("%05d", Integer.parseInt(String.valueOf(seconds)));
        } catch (Exception e) {
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return theSessionID;
    }

    public static boolean fnCreateFileFromBase64(String theBase64Data, String theImagePath){
        boolean rVal = false;
        try {
            byte[] data = DatatypeConverter.parseBase64Binary(theBase64Data);

            File file = new File(theImagePath);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                outputStream.write(data);
                rVal = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static int fnSendSMS(String theReceiver, String theMessage, String theCharge, MSGConstants.MSGMode theMode, int thePriority, String theCategory, String theRequestApplication, String theSourceApplication, String theSessionID, String theCorrelationID){
        try {
            String strProductID = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID");
            long lnProductID = Long.parseLong(strProductID);
            String strSender = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/SENDER");
            String strCommand = "BulkSMS";
            MSGConstants.Sensitivity theSensitivity = MSGConstants.Sensitivity.PERSONAL;


            Thread worker = new Thread(() -> {
                MSGProcessor.sendMSG(
                        lnProductID,
                        strSender,
                        theReceiver,
                        theMessage,
                        strCommand,
                        theSensitivity,
                        theCategory,
                        thePriority,
                        theCharge,
                        theMode,
                        theRequestApplication,
                        theCorrelationID,
                        theSourceApplication,
                        theSessionID
                );
            });
            worker.start();
            return 1;
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return 0;
    }

    public static class OTP {
        private int length;
        private int ttl;
        private String id;
        private String value;
        private boolean enabled;

        public OTP(int length, int ttl, String id, String value, boolean enabled) {
            this.length = length;
            this.ttl = ttl;
            this.value = value;
            this.id = id;
            this.enabled = enabled;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }


    public static class ServiceProviderAccount {
        private String strProviderCode;
        private String strProviderAccountCode;
        private String strProviderAccountName;
        private String strProviderAccountType;
        private String strProviderAccountTypeTag;
        private String strProviderAccountIdentifier;
        private String strProviderAccountLongTag;
        private String strProviderBranchCode;
        private String dblMinTransactionAmount;
        private String dblMaxTransactionAmount;

        public ServiceProviderAccount(String theProviderCode, String theProviderAccountCode, String theProviderAccountName, String theProviderAccountType, String theProviderAccountTypeTag, String theProviderAccountIdentifier, String theProviderAccountLongTag, String theProviderBranchCode, String theMinTransactionAmount, String theMaxTransactionAmount) {
            this.strProviderCode = theProviderCode;
            this.strProviderAccountCode = theProviderAccountCode;
            this.strProviderAccountName = theProviderAccountName;
            this.strProviderAccountType = theProviderAccountType;
            this.strProviderAccountTypeTag = theProviderAccountTypeTag;
            this.strProviderAccountIdentifier = theProviderAccountIdentifier;
            this.strProviderAccountLongTag = theProviderAccountLongTag;
            this.strProviderBranchCode = theProviderBranchCode;
            this.dblMinTransactionAmount = theMinTransactionAmount;
            this.dblMaxTransactionAmount = theMaxTransactionAmount;
        }

        public String getProviderCode() {
            return strProviderCode;
        }

        public void setProviderCode(String strProviderCode) {
            this.strProviderCode = strProviderCode;
        }

        public String getProviderAccountCode() {
            return strProviderAccountCode;
        }

        public void setProviderAccountCode(String strProviderAccountCode) {
            this.strProviderAccountCode = strProviderAccountCode;
        }

        public String getProviderAccountName() {
            return strProviderAccountName;
        }

        public void setProviderAccountName(String strProviderAccountName) {
            this.strProviderAccountName = strProviderAccountName;
        }

        public String getProviderAccountType() {
            return strProviderAccountType;
        }

        public String getProviderAccountTypeTag() {
            return strProviderAccountTypeTag;
        }

        public void setProviderAccountType(String strProviderAccountType) {
            this.strProviderAccountType = strProviderAccountType;
        }

        public String getProviderAccountIdentifier() {
            return strProviderAccountIdentifier;
        }

        public void setProviderAccountIdentifier(String strProviderAccountIdentifier) {
            this.strProviderAccountIdentifier = strProviderAccountIdentifier;
        }

        public String getProviderAccountLongTag() {
            return strProviderAccountLongTag;
        }

        public void setProviderAccountLongTag(String strProviderAccountLongTag) {
            this.strProviderAccountLongTag = strProviderAccountLongTag;
        }

        public String getProviderBranchCode() {
            return strProviderBranchCode;
        }

        public void setProviderBranchCode(String strProviderBranchCode) {
            this.strProviderBranchCode = strProviderBranchCode;
        }

        public String getMinTransactionAmount() {
            return dblMinTransactionAmount;
        }

        public void setMinTransactionAmount(String dblMinTransactionAmount) {
            this.dblMinTransactionAmount = dblMinTransactionAmount;
        }

        public String getMaxTransactionAmount() {
            return dblMaxTransactionAmount;
        }

        public void setMaxTransactionAmount(String dblMaxTransactionAmount) {
            this.dblMaxTransactionAmount = dblMaxTransactionAmount;
        }
    }

    public static LinkedList<ServiceProviderAccount> getSPAccounts(SPManagerConstants.ProviderAccountType theProviderAccountType){
        LinkedList<ServiceProviderAccount> rVal = new LinkedList<ServiceProviderAccount>();
        SPManager spManager;
        try {
            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            spManager = new SPManager(strIntegritySecret);
            LinkedList<LinkedHashMap<String, String>> llHsB2CAccounts = spManager.getB2BCapabilitySPAccounts(theProviderAccountType);
            for (LinkedHashMap<String, String> lhsB2CAccount : llHsB2CAccounts) {
                String strProviderCode = lhsB2CAccount.get("provider_code");
                String strProviderAccountCode = lhsB2CAccount.get("provider_account_code");
                String strProviderAccountName = lhsB2CAccount.get("provider_account_name");
                String strProviderAccountType = lhsB2CAccount.get("provider_account_type");
                String strProviderAccountTypeTag = lhsB2CAccount.get("provider_account_type_tag");
                String strProviderAccountIdentifier = lhsB2CAccount.get("provider_account_identifier");
                String strProviderAccountLongTag = lhsB2CAccount.get("provider_account_long_tag");
                String strProviderOtherDetails = lhsB2CAccount.get("provider_other_details");
                String dblMinTransactionAmount = lhsB2CAccount.get("min_transaction_amount");
                String dblMaxTransactionAmount = lhsB2CAccount.get("max_transaction_amount");

                String strProviderBranchCode = MBankingXMLFactory.getXPathValueFromXMLString("/OTHER_DETAILS/DATA/PROVIDER_ACCOUNT_DETAILS/BRANCH_CODE", strProviderOtherDetails);
                ServiceProviderAccount spaServiceProviderAccount = new ServiceProviderAccount(strProviderCode, strProviderAccountCode, strProviderAccountName, strProviderAccountType, strProviderAccountTypeTag, strProviderAccountIdentifier, strProviderAccountLongTag, strProviderBranchCode, dblMinTransactionAmount, dblMaxTransactionAmount);
                rVal.add(spaServiceProviderAccount);

            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            spManager = null;
        }

        return rVal;
    }

    public static class WithdrawalChannel {
        private String name;
        private String label;
        private String status;
        private boolean withdrawalToOtherNumber;

        public WithdrawalChannel(String name, String label, String status, boolean withdrawalToOtherNumber) {
            this.name = name;
            this.label = label;
            this.status = status;
            this.withdrawalToOtherNumber = withdrawalToOtherNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String theName) {
            this.name = theName;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String theLabel) {
            this.label = theLabel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String theStatus) {
            this.status = theStatus;
        }

        public boolean hasWithdrawalToOtherNumberEnabled() {
            return withdrawalToOtherNumber;
        }

        public void setWithdrawalToOtherNumber(boolean theWithdrawalToOtherNumber) {
            this.withdrawalToOtherNumber = theWithdrawalToOtherNumber;
        }
    }

    public static LinkedList<WithdrawalChannel> getActiveWithdrawalChannels(MBankingConstants.ApplicationType applicationType){
        LinkedList<WithdrawalChannel> rVal = new LinkedList<>();
        NodeList nlWithdrawalChannels;
        Node ndChannel;
        WithdrawalChannel withdrawalChannel;
        try {
            nlWithdrawalChannels = MBankingAPI.getNodeListFromLocalParams(applicationType, "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/CASH_WITHDRAWAL/CHANNELS/CHANNEL");
            for(int i = 0; i < nlWithdrawalChannels.getLength(); i++){
                ndChannel = nlWithdrawalChannels.item(i);

                if (ndChannel != null && ndChannel.getNodeType() == Node.ELEMENT_NODE) {
                    String strName = ndChannel.getAttributes().getNamedItem("NAME").getTextContent();
                    String strLabel = ndChannel.getAttributes().getNamedItem("LABEL").getTextContent();
                    String strStatus = ndChannel.getAttributes().getNamedItem("STATUS").getTextContent();
                    boolean blWithdrawalOtherNumberEnabled = ndChannel.getAttributes().getNamedItem("WITHDRAW_TO_OTHER_NUMBER").getTextContent().equals("ACTIVE");
                    if(strStatus.equalsIgnoreCase("ACTIVE")){
                        withdrawalChannel = new WithdrawalChannel(strName, strLabel, strStatus, blWithdrawalOtherNumberEnabled);
                        rVal.add(withdrawalChannel);
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        } finally {
            nlWithdrawalChannels = null;
            ndChannel = null;
            withdrawalChannel = null;
        }
        return rVal;
    }

    public static WithdrawalChannel getWithdrawalChannel(String theChannelName){
        WithdrawalChannel rVal = null;
        LinkedList<WithdrawalChannel> lsActiveWithdrawalChannels;
        try {
            if (theChannelName != null) {
                lsActiveWithdrawalChannels = getActiveWithdrawalChannels(MBankingConstants.ApplicationType.USSD);
                for (WithdrawalChannel lsActiveWithdrawalChannel : lsActiveWithdrawalChannels) {
                    String strName = lsActiveWithdrawalChannel.getName();
                    if (strName.equalsIgnoreCase(theChannelName)) {
                        rVal = lsActiveWithdrawalChannel;
                        break;
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            lsActiveWithdrawalChannels = null;
        }
        return rVal;
    }

    public static LinkedList<HashMap<String, String>> getStatementPeriods(MBankingConstants.ApplicationType applicationType){
        LinkedList<HashMap<String, String>> rVal = new LinkedList<>();
        NodeList nlStatementPeriods;
        Node ndChannel;
        try {
            nlStatementPeriods = MBankingAPI.getNodeListFromLocalParams(applicationType, "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/ACCOUNT_STATEMENT/STATEMENT_PERIODS/PERIOD");
            for(int i = 0; i < nlStatementPeriods.getLength(); i++){
                ndChannel = nlStatementPeriods.item(i);

                if (ndChannel != null && ndChannel.getNodeType() == Node.ELEMENT_NODE) {
                    String strStatus = ndChannel.getAttributes().getNamedItem("STATUS").getTextContent();

                    if(strStatus.equalsIgnoreCase("ACTIVE")){
                        HashMap<String, String> hmStatementPeriods = new HashMap<String, String>();
                        for(int j = 0; j < ndChannel.getAttributes().getLength(); j++){
                            String strName = ndChannel.getAttributes().item(j).getNodeName();
                            String strValue = ndChannel.getAttributes().item(j).getTextContent();
                            hmStatementPeriods.put(strName, strValue);
                        }
                        rVal.add(hmStatementPeriods);
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        } finally {
            nlStatementPeriods = null;
            ndChannel = null;
        }
        return rVal;
    }

    public static MemberRegisterResponse fnCheckMemberRegister(String theMobileNumber, RegisterConstants.MemberRegisterType theRegisterType){
        MemberRegisterResponse registerResponse = null;
        try{
            registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.MSISDN, theMobileNumber, RegisterConstants.MemberRegisterType.WHITELIST);
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return registerResponse;
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (Exception te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static Date getCurrentJavaUtilDateTime() {
        return new Date();
    }

    public static int convertToSeconds(int period, String periodUnit) {
        int converted = period;
        switch (periodUnit) {
            case "SECOND": {
                converted = period;
                break;
            }

            case "MINUTE": {
                converted = period * 60;
                break;
            }

            case "HOUR": {
                converted = period * 60 * 60;
                break;
            }

            case "DAY": {
                converted = period * 60 * 60 * 24;
                break;
            }

            case "WEEK": {
                converted = period * 60 * 60 * 24 * 7;
                break;
            }

            case "MONTH": {
                converted = period * 60 * 60 * 24 * 7 * 30;
                break;
            }

            case "YEAR": {
                converted = period * 60 * 60 * 24 * 7 * 30 * 12;
                break;
            }
        }

        return converted;
    }

    public static Date add(int period, int periodUnit) {
        Date now = getCurrentJavaUtilDateTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(periodUnit, period);
        return cal.getTime();
    }

    /**
     * @param date java.util.Date Object to convert to String
     * @return String value of Date
     * Format = yyyy-M-dd HH:mm:ss (2017-10-25 18:02:25)
     */
    public static String convertDateToDateString(Date date) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
        try {
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return current UNIX timestamp
     * of type Long
     */
    public static long getCurrentUnixTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * @return current date and time with system default format
     * Type String
     */
    public static String getCurrentDateTime() {
        return new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT)
                .format(new java.sql.Date(getCurrentUnixTimestamp()));
    }

    /**
     * @param format Desired date or date & time format
     *               Type String
     * @return current date
     * Type String
     */
    public static String getCurrentDate(String format) {
        try {
            return new SimpleDateFormat(format)
                    .format(new java.sql.Date(
                            getCurrentUnixTimestamp()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCustomDuration(String strLoginActionValidDate){
        if(strLoginActionValidDate == null || strLoginActionValidDate.isEmpty()){
            return "";
        } else {
            try {
                Date loginActionValidDate = APIUtils.convertDateStringToDate(strLoginActionValidDate);
                Date currentDate = APIUtils.getCurrentJavaUtilDateTime();
                return APIUtils.getPrettyDateTimeDifferenceRoundedUp(currentDate, Objects.requireNonNull(loginActionValidDate));
            } catch (Exception e){
                e.printStackTrace();
                return "";
            }
        }
    }

    public static String convertToBase36(String value) {
        return convertToBase(value, 36);
    }

    public static String convertToBase(String value, int base) {
        BigInteger bigInteger = new BigInteger(value);
        return bigInteger.toString(base).toUpperCase();
    }
}
