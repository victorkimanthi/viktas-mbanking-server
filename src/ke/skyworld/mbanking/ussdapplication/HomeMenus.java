package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.*;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;

import java.util.ArrayList;
import java.util.HashMap;

public interface HomeMenus {

    default USSDResponse displayMenu_Init(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {

            String steMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strUSSDCode = String.valueOf(theUSSDRequest.getUSSDCode());
            String strUSSDSubCode = String.valueOf(theUSSDRequest.getUSSDSubCode());

            if(strUSSDCode.equals(AppConstants.strGeneralUSSDCode) && strUSSDSubCode.equals(AppConstants.strGeneralUSSDSubCode)) {

                String strHeader = "Welcome to "+AppConstants.strSACCOName;
                theUSSDResponse = displayMenu_GeneralMenus(theUSSDRequest, theParam, strHeader);

            } else {
                HashMap<String, String> checkUserReturnValues = theUSSDAPI.checkUser(theUSSDRequest);
                USSDAPIConstants.CheckUserReturnVal rVal = USSDAPIConstants.CheckUserReturnVal.valueOf(checkUserReturnValues.get("CHECK_USER_RVAL"));
                System.out.println("rVal: "+rVal);
                String strResponse = strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
                switch (rVal) {
                    case ACTIVE: {
                        strResponse = "Welcome to " + AppConstants.strMobileBankingName + "\nPlease enter your PIN to proceed:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        break;
                    }
                    case INVALID_IMSI:{
                        strResponse = "Sorry, your SIM Card is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case INVALID_IMEI:{
                        strResponse = "Sorry, your Mobile Phone is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case BLOCKED:{
                        strResponse = "Sorry, your account has been blocked from using "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case SUSPENDED:{
                        strResponse = "Sorry, your account is SUSPENDED from using "+AppConstants.strSACCOName+" mobile banking services."+AppConstants.contactUs;

                        String strLoginActionValidDate = checkUserReturnValues.get("DB_LOGIN_ACTION_VALID_DATE");
                        String actionDuration = APIUtils.getCustomDuration(strLoginActionValidDate);

                        if(!actionDuration.equals("")){
                            strResponse = "Sorry, your account is SUSPENDED from using "+AppConstants.strSACCOName+" mobile banking services. Please try again in "+actionDuration;
                        }
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case LOCKED:{
                        strResponse = "Sorry, your "+AppConstants.strSACCOName+" mobile banking account is LOCKED."+AppConstants.contactUs;
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case NOT_FOUND: {
                        strResponse = "Sorry, you are not registered to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.contactUs+".\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");


                    /* REGISTER USER
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_ACTION, "NO",theArrayListUSSDSelectOption);
                    */
                        break;
                    }
                    case NOT_IN_WHITELIST:{
                        strResponse = "Sorry, your account is not whitelisted for "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case ERROR: {
                        strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance"+AppConstants.contact+".";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }default:{
                        strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance"+AppConstants.contact+". UNKNOWN ERROR";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                }
            }

        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
            if(theUSSDResponse!=null){

            }
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Login(USSDRequest theUSSDRequest, String theParam) {
        USSDAPI theUSSDAPI = new USSDAPI();
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            HashMap<String, String> hmLoginReturnVal = theUSSDAPI.userLogin(theUSSDRequest);
            String rvalLoginReturnVal = hmLoginReturnVal.get("LOGIN_RETURN_VALUE");

            switch (rvalLoginReturnVal) {
                case "SUCCESS": {
                    theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
                    break;
                }
                case "INVALID_IMSI": {
                    String strResponse = "Sorry, your SIM Card is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services. Please visit one of our branches for assistance or contact us.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "INCORRECT_PIN": {
                    String strResponse = hmLoginReturnVal.get("LOGIN_ATTEMPT_MESSAGE");
                    String endSession = hmLoginReturnVal.get("END_SESSION");

                    if(endSession.equals("YES")){
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse,"NO");
                    } else {
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "SET_PIN": {
                    theUSSDResponse = theAppMenus.displayMenu_SetPIN(theUSSDRequest, theParam);
                    break;
                }
                case "BLOCKED": {
                    String strResponse = "Dear member, your account has been blocked from accessing mobile banking services. Please visit one of our branches for assistance or contact us.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "SUSPEND":
                case "SUSPENDED": {
                    String strResponse = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. Please contact us for assistance.";

                    String strLoginActionValidDate = hmLoginReturnVal.get("DB_LOGIN_ACTION_VALID_DATE");
                    String actionDuration = APIUtils.getCustomDuration(strLoginActionValidDate);

                    if (!actionDuration.equals("")) {
                        strResponse = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. Please try again in " + actionDuration;
                    }
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "LOCK":
                case "LOCKED": {
                    String strResponse = "Sorry, your " + AppConstants.strSACCOName + " mobile banking services account is LOCKED. Please contact us for assistance.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "ERROR": {
                    String strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                default: {
                    String strResponse = strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance. UNKNOWN ERROR";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_GeneralMenus(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {
            //SELECT
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDCharge("NO");

            theUSSDResponse.setUSSDSelectDataType(AppConstants.USSDDataType.GENERAL_MENU.getValue());
            theUSSDResponse.setUSSDSelectName(AppConstants.USSDDataType.GENERAL_MENU.name());

            //OPTIONS
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BUY_GOODS", "1: "+AppConstants.strBusinessShortCodePaymentName + " (Buy Goods & Services)");

            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());

            //SELECT OPTIONSequalsIgnoreCase
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
        }

        return theUSSDResponse;

    }

    default USSDResponse displayMenu_General(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() - 1];

            if(strLastValue.equalsIgnoreCase(AppConstants.USSDDataType.GENERAL_MENU.name())) {
                theUSSDResponse = theAppMenus.displayMenu_GeneralMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
            } else {
                String strGENERAL_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.GENERAL_MENU.name());
                System.out.println("strGENERAL_MENU: " +strGENERAL_MENU);
                switch (strGENERAL_MENU) {
                    case "BUY_GOODS":{
                        theUSSDResponse = theAppMenus.displayMenu_BuyGoodsMenus(theUSSDRequest, theParam);
                        break;
                    }
                    default: {
                        String strHeader =  AppConstants.strHomeMenuHeader + "\n{Select a valid menu}";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralMenus(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }

        }catch (Exception e){
            System.err.println(e.getMessage());
        }finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_MainInMenus(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {
            //SELECT
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDCharge("NO");

            theUSSDResponse.setUSSDSelectDataType(AppConstants.USSDDataType.MAIN_IN_MENU.getValue());
            theUSSDResponse.setUSSDSelectName(AppConstants.USSDDataType.MAIN_IN_MENU.name());

            //OPTIONS
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BALANCE_ENQUIRY", "1: Balance Enquiry");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "WITHDRAWAL", "2: Withdrawal to M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DEPOSIT", "3: Payments and Deposit");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "LOAN", "4: Loans");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "MY_ACCOUNT", "5: My Account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "FUNDS_TRANSFER", "6: Funds Transfer");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "UTILITIES", "7: Utilities");
            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());

            //SELECT OPTIONSequalsIgnoreCase
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
        }

        return theUSSDResponse;

    }

    default USSDResponse displayMenu_MainIn(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //System.out.println("MAIN IN strLastKey: " +strLastKey);
            //System.out.println("MAIN IN strLastValue: " +strLastValue);
            if (strLastValue.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name()) && (USSDConstants.arrBranchOptionNames.contains(strLastKey))) { //If the last entry is from LOGIN_PIN then display MAIN_IN_MENU
                theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
            } else {
                String strMainInMenu = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAIN_IN_MENU.name());
                switch (strMainInMenu) {
                    case "WITHDRAWAL":{
                        theUSSDResponse = theAppMenus.displayMenu_Withdrawal(theUSSDRequest, theParam);
                        break;
                    }
                    case "UTILITIES":{
                        theUSSDResponse = theAppMenus.displayMenu_Utilities(theUSSDRequest, theParam);
                        break;
                    }
                    case "DEPOSIT":{
                        theUSSDResponse = theAppMenus.displayMenu_Deposit(theUSSDRequest, theParam);
                        break;
                    }
                    case "MY_ACCOUNT": {
                        theUSSDResponse = theAppMenus.displayMenu_MyAccount(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN":{
                        theUSSDResponse = theAppMenus.displayMenu_Loan(theUSSDRequest, theParam);
                        break;
                    }
                    case "FUNDS_TRANSFER":{
                        theUSSDResponse = theAppMenus.displayMenu_FundTransfer(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHANGE_PIN":{
                        theUSSDResponse = theAppMenus.displayMenu_ChangePIN(theUSSDRequest,theParam);
                        break;
                    }
                    case "BALANCE_ENQUIRY":{
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    default: {
                        String strHeader =  AppConstants.strHomeMenuHeader + "\n{Select a valid menu}";
                        theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }

        }catch (Exception e){
            System.err.println(e.getMessage());
        }finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    //***************************************************
    //**************** OLD ****************
    //************************************************
    /*default USSDResponse displayMenu_Init(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            HashMap<String, String> checkUserReturnValues = theUSSDAPI.checkUser(theUSSDRequest);
            USSDAPIConstants.CheckUserReturnVal rVal = USSDAPIConstants.CheckUserReturnVal.valueOf(checkUserReturnValues.get("CHECK_USER_RVAL"));
            System.out.println("rVal: "+rVal);
            String strResponse = strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
            switch (rVal) {
                case ACTIVE: {
                    strResponse = "Welcome to " + AppConstants.strMobileBankingName + "\nPlease enter your PIN to proceed:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }
                case INVALID_IMSI:{
                    strResponse = "Sorry, your SIM Card is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case INVALID_IMEI:{
                    strResponse = "Sorry, your Mobile Phone is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case BLOCKED:{
                    strResponse = "Sorry, your account has been blocked from using "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case SUSPENDED:{
                    strResponse = "Sorry, your account is SUSPENDED from using "+AppConstants.strSACCOName+" mobile banking services."+AppConstants.contactUs;

                    String strLoginActionValidDate = checkUserReturnValues.get("DB_LOGIN_ACTION_VALID_DATE");
                    String actionDuration = APIUtils.getCustomDuration(strLoginActionValidDate);

                    if(!actionDuration.equals("")){
                        strResponse = "Sorry, your account is SUSPENDED from using "+AppConstants.strSACCOName+" mobile banking services. Please try again in "+actionDuration;
                    }
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case LOCKED:{
                    strResponse = "Sorry, your "+AppConstants.strSACCOName+" mobile banking account is LOCKED."+AppConstants.contactUs;
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case NOT_FOUND: {
                    strResponse = "Sorry, you are not registered to use "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.contactUs+".\n";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");


                    *//* REGISTER USER
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_ACTION, "NO",theArrayListUSSDSelectOption);
                    *//*
                    break;
                }
                case NOT_IN_WHITELIST:{
                    strResponse = "Sorry, your account is not whitelisted for "+AppConstants.strMobileBankingName+" mobile banking services."+AppConstants.visitOurBranchesAndContactUs;
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case ERROR: {
                    strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance"+AppConstants.contact+".";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }default:{
                    strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance"+AppConstants.contact+". UNKNOWN ERROR";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
            if(theUSSDResponse!=null){

            }
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Login(USSDRequest theUSSDRequest, String theParam) {
        USSDAPI theUSSDAPI = new USSDAPI();
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            HashMap<String, String> hmLoginReturnVal = theUSSDAPI.userLogin(theUSSDRequest);
            String rvalLoginReturnVal = hmLoginReturnVal.get("LOGIN_RETURN_VALUE");

            switch (rvalLoginReturnVal) {
                case "SUCCESS": {
                    theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
                    break;
                }
                case "INVALID_IMSI": {
                    String strResponse = "Sorry, your SIM Card is not allowed to use "+AppConstants.strMobileBankingName+" mobile banking services. Please visit one of our branches for assistance or contact us.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "INCORRECT_PIN": {
                    String strResponse = hmLoginReturnVal.get("LOGIN_ATTEMPT_MESSAGE");
                    String endSession = hmLoginReturnVal.get("END_SESSION");

                    if(endSession.equals("YES")){
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse,"NO");
                    } else {
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "SET_PIN": {
                    theUSSDResponse = theAppMenus.displayMenu_SetPIN(theUSSDRequest, theParam);
                    break;
                }
                case "BLOCKED": {
                    String strResponse = "Dear member, your account has been blocked from accessing mobile banking services. Please visit one of our branches for assistance or contact us.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "SUSPEND":
                case "SUSPENDED": {
                    String strResponse = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. Please contact us for assistance.";

                    String strLoginActionValidDate = hmLoginReturnVal.get("DB_LOGIN_ACTION_VALID_DATE");
                    String actionDuration = APIUtils.getCustomDuration(strLoginActionValidDate);

                    if (!actionDuration.equals("")) {
                        strResponse = "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " mobile banking services. Please try again in " + actionDuration;
                    }
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "LOCK":
                case "LOCKED": {
                    String strResponse = "Sorry, your " + AppConstants.strSACCOName + " mobile banking services account is LOCKED. Please contact us for assistance.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                case "ERROR": {
                    String strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
                default: {
                    String strResponse = strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance. UNKNOWN ERROR";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_MainInMenus(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {
            //SELECT
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDCharge("NO");

            theUSSDResponse.setUSSDSelectDataType(AppConstants.USSDDataType.MAIN_IN_MENU.getValue());
            theUSSDResponse.setUSSDSelectName(AppConstants.USSDDataType.MAIN_IN_MENU.name());

            //OPTIONS
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BALANCE_ENQUIRY", "1: Balance Enquiry");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "WITHDRAWAL", "2: Withdrawal to M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DEPOSIT", "3: Payments and Deposit");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "LOAN", "4: Loans");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "MY_ACCOUNT", "5: My Account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "FUNDS_TRANSFER", "6: Funds Transfer");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "UTILITIES", "7: Utilities");
            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());

            //SELECT OPTIONSequalsIgnoreCase
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        finally{
        }

        return theUSSDResponse;

    }

    default USSDResponse displayMenu_MainIn(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //System.out.println("MAIN IN strLastKey: " +strLastKey);
            //System.out.println("MAIN IN strLastValue: " +strLastValue);
            if (strLastValue.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name()) && (USSDConstants.arrBranchOptionNames.contains(strLastKey))) { //If the last entry is from LOGIN_PIN then display MAIN_IN_MENU
                theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
            } else {
                String strMainInMenu = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAIN_IN_MENU.name());
                switch (strMainInMenu) {
                    case "WITHDRAWAL":{
                        theUSSDResponse = theAppMenus.displayMenu_Withdrawal(theUSSDRequest, theParam);
                        break;
                    }
                    case "UTILITIES":{
                        theUSSDResponse = theAppMenus.displayMenu_Utilities(theUSSDRequest, theParam);
                        break;
                    }
                    case "DEPOSIT":{
                        theUSSDResponse = theAppMenus.displayMenu_Deposit(theUSSDRequest, theParam);
                        break;
                    }
                    case "MY_ACCOUNT": {
                        theUSSDResponse = theAppMenus.displayMenu_MyAccount(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN":{
                        theUSSDResponse = theAppMenus.displayMenu_Loan(theUSSDRequest, theParam);
                        break;
                    }
                    case "FUNDS_TRANSFER":{
                        theUSSDResponse = theAppMenus.displayMenu_FundTransfer(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHANGE_PIN":{
                        theUSSDResponse = theAppMenus.displayMenu_ChangePIN(theUSSDRequest,theParam);
                        break;
                    }
                    case "BALANCE_ENQUIRY":{
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    default: {
                        String strHeader =  AppConstants.strHomeMenuHeader + "\n{Select a valid menu}";
                        theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }

        }catch (Exception e){
            System.err.println(e.getMessage());
        }finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }*/

}
