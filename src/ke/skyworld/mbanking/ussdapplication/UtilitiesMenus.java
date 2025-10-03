package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.sp.manager.SPManagerConstants;
import ke.skyworld.sp.manager.SPManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public interface UtilitiesMenus {

    default USSDResponse displayMenu_UtilitiesMenu(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.UTILITY_CODE);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            int intOptionMenu = 0;

            /*if(strMobileNumber.equals("254713000249") || strMobileNumber.equals("254712747943")) {
                intOptionMenu = intOptionMenu+1;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), "MPESA_FLOAT_PURCHASE", "1: M-PESA Float Purchase");

                intOptionMenu = intOptionMenu+1;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), "BUY_AIRTIME", "2: Buy Airtime");

            } else {
                intOptionMenu = intOptionMenu+1;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), "BUY_AIRTIME", "1: Buy Airtime");
            }*/

            intOptionMenu = intOptionMenu+1;
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), "MPESA_FLOAT_PURCHASE", "1: M-PESA Float Purchase");

            //intOptionMenu = intOptionMenu+1;
            //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), "BUY_AIRTIME", "2: Buy Airtime");


            for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                //int intOptionMenu = llSPAAccounts.indexOf(serviceProviderAccount)+1;
                intOptionMenu = intOptionMenu+1;

                String strProviderAccountIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                String strProviderCode = serviceProviderAccount.getProviderCode();
                String strProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
                String strProviderAccountName = serviceProviderAccount.getProviderAccountName();
                String strProviderAccountType = serviceProviderAccount.getProviderAccountType();
                String strProviderAccountTypeTag = serviceProviderAccount.getProviderAccountTypeTag();
                String strProviderAccountLongTag = serviceProviderAccount.getProviderAccountLongTag();
                String dblMinTransactionAmount = serviceProviderAccount.getMinTransactionAmount();
                String dblMaxTransactionAmount = serviceProviderAccount.getMaxTransactionAmount();

                HashMap<String, String> hmProviderAccount = new HashMap<>();
                hmProviderAccount.put("code",strProviderAccountCode);
                //hmProviderAccount.put("name",strProviderAccountName);
                hmProviderAccount.put("identifier",strProviderAccountIdentifier);
                //hmProviderAccount.put("provider_code",strProviderCode);
                hmProviderAccount.put("type",strProviderAccountType);
                hmProviderAccount.put("type_tag",strProviderAccountTypeTag);
                hmProviderAccount.put("long_tag",strProviderAccountLongTag);
                //hmProviderAccount.put("min_amount",dblMinTransactionAmount);
                //hmProviderAccount.put("max_amount",dblMaxTransactionAmount);

                String strOptionMenu = String.valueOf(intOptionMenu);
                String strOptionValue  = Utils.serialize(hmProviderAccount);

                String strOptionDisplayText = strOptionMenu+": "+strProviderAccountLongTag;

                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.UTILITIES_MENU, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_UtilitiesMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Utilities(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if(strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())){
                String strHeader = "Utilities";
                theUSSDResponse =  displayMenu_UtilitiesMenu(theUSSDRequest, theParam, strHeader);
            }else{

                String strUTILITIES_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

                if (strUTILITIES_MENU.equals("BUY_AIRTIME")) {
                    theUSSDResponse = theAppMenus.displayMenu_Etopup(theUSSDRequest, theParam);
                } else if (strUTILITIES_MENU.equals("MPESA_FLOAT_PURCHASE")) {
                    theUSSDResponse = theAppMenus.displayMenu_MPESA_Float_Purchase(theUSSDRequest, theParam);
                } else if (!strUTILITIES_MENU.isEmpty()) {
                    theUSSDResponse = theAppMenus.displayMenu_PayBill(theUSSDRequest, theParam);
                } else {
                    String strHeader = "Utilities\n{Select a valid menu}";
                    theUSSDResponse = displayMenu_UtilitiesMenu(theUSSDRequest, theParam, strHeader);
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Utilities() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Etopup(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "Buy Airtime";
        try{
            switch (theParam) {
                case "MENU": {
                    String strHeader2 = strHeader + " \nSelect account\n";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.ETOPUP_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());

                    if (strAccount.length() > 0){
                        String strResponse = strHeader+"\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strHeader2 = strHeader + " \n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.ETOPUP_ACCOUNT);
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());

                    String strMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum();
                    String strMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum();

                    String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                    HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                    String strSourceAccountNo = hmAccountDetails.get("number");
                    String strSourceAccountName = hmAccountDetails.get("name");
                    String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                    String strSourceAccountLabel = hmAccountDetails.get("label");
                    String strSourceAccountAvailableBalance = hmAccountDetails.get("avail_bal");

                    double dblAvailableBalance = 0;
                    try { dblAvailableBalance = Double.parseDouble(strSourceAccountAvailableBalance); }catch (Exception e){}

                    if (!strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) < Double.parseDouble(strMinimum)) {
                        String strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strMinimum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) > Double.parseDouble(strMaximum)) {
                        String strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strMaximum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }else if(Double.parseDouble(strAmount) > dblAvailableBalance){
                        String strResponse = strHeader + "\n{" +strSourceAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblAvailableBalance,"#,###.##") + " is INSUFFICIENT to Buy Airtime of KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    else {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        //String strResponse =  "Confirm "+strHeader + "\n" + "Amount: KES "+strAmount+"\n"; //Without Account No
                        String strResponse =  "Confirm "+strHeader + "\n" + "Paying Account: " + strSourceAccountLabel + "\nAmount: KES "+strAmount+"\n"; //With Account No

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ETOPUP_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_CONFIRMATION.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){
                        String  strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.";

                        /*Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.airtimePurchase(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);
                            System.out.println("withdrawal: "+transactionReturnVal.getValue());
                        });
                        worker.start();*/

                        HashMap<String, String> hmResponse = new HashMap<>();
                        USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.airtimePurchase(theUSSDRequest, PESAConstants.PESAType.PESA_OUT,hmResponse);



                        if(transactionReturnVal.equals(USSDAPIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.\n";
                        }else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                case INSUFFICIENT_BAL: {
                                    strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                    break;
                                }
                                case WITHDRAWAL_LIMIT_VIOLATION: {
                                    String strCBSResponse = hmResponse.get("WITHDRAWAL_LIMIT_VIOLATION");
                                    strResponse = "Dear member, amount requested to " +strHeader+ " violates limit RESTRICTION.\n" + strCBSResponse + "\n";

                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your " +strHeader+ " request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);

                    }else if(strConfirmation.equalsIgnoreCase("NO")){
                        String strResponse = "Dear member, your " +strHeader+ " request NOT confirmed. " +strHeader+ "  request NOT COMPLETED.\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm "+strHeader + "\n{Select a valid menu}\nPaying Account: " + strSourceAccountLabel  + "\nAmount: KES "+strAmount+"\n"; //With Account No

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_Etopup() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Etopup() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_PayBill(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strUtilityProviderAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

        HashMap<String, String> hmUtilityAccountDetails = Utils.toHashMap(strUtilityProviderAccountDetails);

        String strToSPProviderAccountCode = hmUtilityAccountDetails.get("code");
        String strToAccountIdentifier = hmUtilityAccountDetails.get("identifier");
        String strToAccountType = hmUtilityAccountDetails.get("type");
        String strToAccountNaming = hmUtilityAccountDetails.get("type_tag");
        String strToBillerName = hmUtilityAccountDetails.get("long_tag");

        String strHeader = "Pay for " + strToBillerName;
        try{
            switch (theParam) {
                case "MENU": {
                    //USE MENUs
                    theUSSDResponse = displayMenu_Paybill_Maintain_Accounts(theUSSDRequest, theParam);

                    break;
                }
                case "BILLER_ACCOUNT": {

                    String strMenuOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());


                    String strAction = "";
                    if(!strMenuOption.isEmpty()){
                        HashMap<String, String> hmMenuOption = Utils.toHashMap(strMenuOption);
                        strAction = hmMenuOption.get("ACTION");
                    }

                    switch (strAction) {
                        case "CHOICE": {
                            String strHeader2 = strHeader + " \nSelect paying account\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT);
                            break;
                        }
                        case "ADD": {
                            String strResponse = "Add " + strToBillerName +"\nEnter " + strToAccountNaming + ":";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        case "REMOVE": {
                            String strHeader2 = "Remove " + strToBillerName +"\nSelect " + strToAccountNaming + " to Remove:";

                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader2, USSDConstants.Condition.NO);
                            break;
                        }
                        default:{
                            String strHeader2 = "Pay for " + strToBillerName +"\n{Select a VALID MENU}:";
                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader2, USSDConstants.Condition.YES);
                            break;
                        }
                    }
                    break;
                }
                case "FROM_ACCOUNT": {
                    String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());

                    if( strFromAccount.length() > 0 ){
                        String strResponse = strHeader+"\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else {
                        String strHeader2 = strHeader + " \n{Select a valid paying account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT);
                    }

                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());

                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                        String strPayBillMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.PAY_BILL).getMinimum();
                        String strPayBillMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.PAY_BILL).getMaximum();

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");
                        String strSourceAccountAvailableBalance = hmAccountDetails.get("avail_bal");

                        double dblAvailableBalance = 0;
                        try { dblAvailableBalance = Double.parseDouble(strSourceAccountAvailableBalance); }catch (Exception e){}

                        double dblPayBillMinimum = Double.parseDouble(strPayBillMinimum);
                        double dblPayBillMaximum = Double.parseDouble(strPayBillMaximum);

                        double dblAmountEntered = Double.parseDouble(strAmount);

                        if (dblAmountEntered < dblPayBillMinimum) {
                            strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strPayBillMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }else if (dblAmountEntered > dblPayBillMaximum) {
                            strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strPayBillMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }else if(Double.parseDouble(strAmount) > dblAvailableBalance){
                            strResponse = strHeader + "\n{" +strSourceAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblAvailableBalance,"#,###.##") + " is INSUFFICIENT to " +strHeader+ " of KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                        }
                    } else {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strBillAccountNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());

                        HashMap<String, String> hmAccount = Utils.toHashMap(strBillAccountNumberHashMap);
                        String strAccountID = hmAccount.get("ACCOUNT_ID");
                        String strAccountName = hmAccount.get("ACCOUNT_NAME");
                        String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm "+strHeader + "\n\nBill " + strToAccountNaming + ": " + strAccountIdentifier + "\nName: " + strAccountName + "\nPaying A/C: " + strSourceAccountLabel + "\nAmount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.PAY_BILL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_CONFIRMATION.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){

                        String  strResponse;

                        /*Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.payBill(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);
                            System.out.println("withdrawal: "+transactionReturnVal.getValue());
                        });
                        worker.start();*/

                        HashMap<String, String> hmResponse = new HashMap<>();
                        USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.payBill(theUSSDRequest, PESAConstants.PESAType.PESA_OUT, hmResponse);

                        if(transactionReturnVal.equals(USSDAPIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your request to " +strHeader+ " has been received successfully. Please wait shortly as it's being processed.\n";
                        } else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your request to " +strHeader+ " CANNOT be completed.\n";
                                    break;
                                }
                                case INSUFFICIENT_BAL: {
                                    strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                    break;
                                }
                                case WITHDRAWAL_LIMIT_VIOLATION: {
                                    String strCBSResponse = hmResponse.get("WITHDRAWAL_LIMIT_VIOLATION");
                                    strResponse = "Dear member, amount requested to " +strHeader+ " violates limit RESTRICTION\n" + strCBSResponse + "\n";

                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your request to " +strHeader+ " CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your " +strHeader+ " request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_END, "NO",theArrayListUSSDSelectOption);

                    }else if(strConfirmation.equalsIgnoreCase("NO")){
                        String strResponse = "Dear member, your " +strHeader+ " request NOT confirmed. " +strHeader+ "  request NOT COMPLETED.\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strBillAccountNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());

                        HashMap<String, String> hmAccount = Utils.toHashMap(strBillAccountNumberHashMap);
                        String strAccountID = hmAccount.get("ACCOUNT_ID");
                        String strAccountName = hmAccount.get("ACCOUNT_NAME");
                        String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm "+strHeader + "\n{Select a valid menu}\n\nBill " + strToAccountNaming + ": " + strAccountIdentifier + "\nName: " + strAccountName + "\nPaying A/C: " + strSourceAccountLabel + "\nAmount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_Etopup() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Etopup() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Paybill_Maintain_Accounts(USSDRequest theUSSDRequest, String theParam){
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());
            String strUtilityProviderAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

            HashMap<String, String> hmUtilityAccountDetails = Utils.toHashMap(strUtilityProviderAccountDetails);

            String strToSPProviderAccountCode = hmUtilityAccountDetails.get("code");
            String strToAccountIdentifier = hmUtilityAccountDetails.get("identifier");
            String strToAccountType = hmUtilityAccountDetails.get("type");
            String strToAccountNaming = hmUtilityAccountDetails.get("type_tag");
            String strToBillerName = hmUtilityAccountDetails.get("long_tag");


            switch (ussdDataType) {
                case UTILITIES_MENU: {
                    String strHeader = "Pay for " + strToBillerName +"\nSelect " + strToAccountNaming + ":";

                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT: {

                    String theAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT.name());

                    if( theAccountNo.matches("^\\d{4,24}$")  ) { //4 - 24 Digits
                        String strResponse = "Add " + strToBillerName +"\nEnter " + strToAccountNaming + " NAME:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                    }else{
                        String strResponse = "Add " + strToBillerName + "\n{Enter a VALID "+strToAccountNaming+"}:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_NAME: {
                    //ADD Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT.name());
                    String strAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_NAME.name());

                    try{
                        String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                        SPManager spManager = new SPManager(strIntegritySecret);
                        spManager.createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN,strMobileNo,strToSPProviderAccountCode, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO,strAccountNo, strAccountName);

                    }catch (Exception e){
                        System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
                    }

                    String strHeader = "Pay for " + strToBillerName +"\nSelect " + strToAccountNaming + ":";
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE: {
                    //REMOVE Account

                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE.name());

                    if(!strAccountHashMap.isEmpty()){
                        try{

                            HashMap<String, String> hmAccount = Utils.toHashMap(strAccountHashMap);
                            String strAccountID = hmAccount.get("ACCOUNT_ID");
                            //String strAccountName = hmAccount.get("ACCOUNT_NAME");
                            //String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");
                            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                            SPManager spManager = new SPManager(strIntegritySecret);
                            spManager.removeUserSavedAccountsByAccountId(strAccountID);

                        }catch (Exception e){
                            System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
                        }


                        String strHeader = "Pay for " + strToBillerName +"\nSelect " + strToAccountNaming + ":";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    }else{
                        String strHeader = "Remove " + strToBillerName +"\n{Select a VALID MENU}:";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.NO);
                    }

                    break;
                }
                default:{

                    String strHeader = "Pay for " + strToBillerName +"\n{Select a VALID " + strToAccountNaming + "}:";

                    System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() UNKNOWN PARAM ERROR : strUSSDDataType = " + ussdDataType.name());
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_MPESA_Float_Purchase(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "M-PESA Float Purchase";
        try{
            switch (theParam) {
                case "MENU": {
                    String strHeader2 = strHeader + " \nSelect account\n";
                    //theUSSDResponse = GeneralMenus.displayMenu_WithdrawalBankAccounts(theUSSDRequest, theParam, strHeader2, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT);
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.name());

                    if (strAccount.length() > 0){
                        String strResponse = strHeader+"\nEnter Agent No.:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strHeader2 = strHeader + " \n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT);
                    }
                    break;
                }
                case "AGENT_NO": {
                    String strAgentNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO.name());

                    if (strAgentNo.length() > 0){
                        String strResponse = strHeader+"\nEnter Agent Name:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strHeader2 = strHeader + " \n{Select a valid Agent No.}\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader2, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "AGENT_NAME": {
                    String strAgentName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.name());

                    if (strAgentName.length() > 0){
                        String strResponse = strHeader+"\nEnter Store No.:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strHeader2 = strHeader + " \n{Select a valid Agent Name}\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader2, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "STORE_NO": {
                    String strStoreNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO.name());

                    if (strStoreNumber.length() > 0){
                        String strResponse = strHeader+"\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strHeader2 = strHeader + " \n{Select a valid Store No.}\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader2, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT.name());

                    String strMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.MPESA_FLOAT_PURCHASE).getMinimum();
                    String strMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.MPESA_FLOAT_PURCHASE).getMaximum();

                    if (!strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) < Double.parseDouble(strMinimum)) {
                        String strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strMinimum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) > Double.parseDouble(strMaximum)) {
                        String strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strMaximum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO.name());
                        String strAgentName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.name());
                        String strStoreNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        //String strResponse =  "Confirm "+strHeader + "\n" + "Amount: KES "+strAmount+"\n"; //Without Account No
                        String strResponse =  "Confirm "+strHeader + "\n"
                                + "Paying A/C: " + strSourceAccountLabel + "\n"
                                + "Agent No: " + strAgentNumber + "\n"
                                //+ "Agent Name: " + strAgentName + "\n"
                                + "Store No: " + strStoreNumber + "\n"
                                + "Amount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_CONFIRMATION.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){
                        String  strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.";

                        HashMap<String, String> hmResponse = new HashMap<>();
                        USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.MPESAFloatPurchase(theUSSDRequest, PESAConstants.PESAType.PESA_OUT, hmResponse);

                        if(transactionReturnVal.equals(USSDAPIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.";
                        }else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                case INSUFFICIENT_BAL: {
                                    strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                    break;
                                }
                                case WITHDRAWAL_LIMIT_VIOLATION: {
                                    String strCBSResponse = hmResponse.get("WITHDRAWAL_LIMIT_VIOLATION");
                                    strResponse = "Dear member, amount requested to " +strHeader+ " violates limit RESTRICTION.\n" + strCBSResponse + "\n";

                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your " +strHeader+ " request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_END, "NO",theArrayListUSSDSelectOption);

                    }else if(strConfirmation.equalsIgnoreCase("NO")){
                        String strResponse = "Dear member, your " +strHeader+ " request NOT confirmed. " +strHeader+ "  request NOT COMPLETED.\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.name());
                        String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO.name());
                        String strAgentName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.name());
                        String strStoreNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        //String strResponse =  "Confirm "+strHeader + "\n" + "Amount: KES "+strAmount+"\n"; //Without Account No
                        String strResponse =  "Confirm "+strHeader + "\n"
                                + "Paying Account No: " + strAccount + "\n"
                                + "Agent No.: " + strAgentNumber + "\n"
                                //+ "Agent Name: " + strAgentName + "\n"
                                + "Store No.: " + strStoreNumber + "\n"
                                + "Amount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_MPESA_Float_Purchase() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_MPESA_Float_Purchase() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
