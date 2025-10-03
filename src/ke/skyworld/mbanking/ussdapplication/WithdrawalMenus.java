package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public interface WithdrawalMenus {

    default USSDResponse displayMenu_Withdrawal(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader =  "Withdrawal to M-PESA";

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            //The Flow:
            //Withdraw via M-Pesa -> Cash Withdrawal Select M-Pesa -> Select Source Account -> Select My Number -> Input Amount -> Input PIN -> Confirm Transactions
            switch (theParam) {
                case "MENU": {
                    String strFullHeader = strHeader + "\nSelect account\n";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strFullHeader, USSDAPIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                    if (strAccount.length() > 0){
                        //USE ONLY WHEN WITHDRAWAL TO OTHER NUMBER IS ENABLED
                        /*
                        APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel("M-PESA");
                        if(withdrawalChannel != null){
                            if(withdrawalChannel.hasWithdrawalToOtherNumberEnabled()){
                                String strFullHeader = strHeader + "\nSelect withdrawal option\n";
                                theUSSDResponse = getWithdrawalToOptionMenu(theUSSDRequest, strFullHeader);
                            } else {
                                String strResponse = strHeader + "\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                            }
                        } else {
                            String strFullHeader = strHeader + "\n{Select a valid menu}\n";
                            theUSSDResponse = getWithdrawalOptionMenu(theUSSDRequest, strFullHeader);
                        }
                        */

                        //USE TO SKIP WHEN WITHDRAWAL TO OTHER NUMBER IS DISABLED
                        String strResponse = strHeader + "\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");

                    }else{
                        String strFullHeader = strHeader + "\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strFullHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT);
                    }
                    break;
                }
                case "TO_OPTION": {
                    String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_OPTION.name());
                    String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
                    if (strToOption.equalsIgnoreCase("MY_NUMBER")) {
                        String strResponse = strHeader + "\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                        String strFullHeader = strHeader + "\nEnter Other Mobile No.\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strFullHeader, AppConstants.USSDDataType.WITHDRAWAL_TO, USSDConstants.USSDInputType.STRING,"NO");
                    }else {
                        String strFullHeader = strHeader + "\n{Select a valid menu}\nSelect withdrawal option?\n";
                        theUSSDResponse = getWithdrawalOptionMenu(theUSSDRequest, strFullHeader);
                    }
                    break;
                }
                case "TO":{
                    String strOtherMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                    strOtherMobileNo = APIUtils.sanitizePhoneNumber(strOtherMobileNo);
                    if(!strOtherMobileNo.equalsIgnoreCase("INVALID MOBILE NUMBER") || !strOtherMobileNo.matches("^254((7[0-2][0-9])|(74[0-3])|(74[5-6])|(748)|(75[7-9])|(76[8-9])|(79[0-9]))[0-9]{6}$")){
                        String strResponse = strHeader + "\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strFullHeader = strHeader + "\n{Enter a valid mobile number}\nEnter Other Mobile No.\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strFullHeader, AppConstants.USSDDataType.WITHDRAWAL_TO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                    String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                    HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                    String strSourceAccountNo = hmAccountDetails.get("number");
                    String strSourceAccountName = hmAccountDetails.get("name");
                    String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                    String strSourceAccountLabel = hmAccountDetails.get("label");
                    String strSourceAccountAvailableBalance = hmAccountDetails.get("avail_bal");

                    double dblAvailableBalance = 0;
                    try { dblAvailableBalance = Double.parseDouble(strSourceAccountAvailableBalance); }catch (Exception e){}

                    double dblMinimumAmount = Double.parseDouble(theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                    double dblMaximumAmount = Double.parseDouble(theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                    String strResponse = strHeader + "\nEnter your PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");

                    if(!strAmount.matches("^[1-9][0-9]*$")){
                        strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else if(Double.parseDouble(strAmount) < dblMinimumAmount){
                        strResponse = strHeader + "\n{MINIMUM amount allowed is KES "+Utils.formatDouble(dblMinimumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if(Double.parseDouble(strAmount) > dblMaximumAmount){
                        strResponse = strHeader + "\n{MAXIMUM amount allowed is KES "+Utils.formatDouble(dblMaximumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else if(Double.parseDouble(strAmount) > dblAvailableBalance){
                        strResponse = strHeader + "\n{" +strSourceAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblAvailableBalance,"#,###.##") + " is INSUFFICIENT to withdraw KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        //String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                        //String strResponse =  "Confirm Cash Withdrawal\nAmount: KES "+strAmount+"\nAccount: " + strAccount + "\n";

                        String strMobileNo = Long.toString(theUSSDRequest.getUSSDMobileNo());
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

                        /*if(strToOption != null) {
                            if ( strOption.equalsIgnoreCase("M-PESA")  ){
                                if(strToOption.equalsIgnoreCase("OTHER_NUMBER")){
                                    strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                                }
                            }
                        }*/

                        APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel("M-PESA");
                        if(withdrawalChannel != null) {
                            if (withdrawalChannel.hasWithdrawalToOtherNumberEnabled()) {
                                if(strToOption != null) {
                                    if(strToOption.equalsIgnoreCase("OTHER_NUMBER")){
                                        strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                                    }
                                }
                            }
                        }

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                        String strSourceAccountNo = hmAccountDetails.get("number");
                        String strSourceAccountName = hmAccountDetails.get("name");
                        String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                        String strSourceAccountLabel = hmAccountDetails.get("label");

                        strMobileNo = APIUtils.sanitizePhoneNumber(strMobileNo);

                        String strResponse =  "Confirm " + strHeader + "\nFrom: "+ strSourceAccountLabel + "\nTo: "+ strMobileNo + "\nAmount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{

                            String strResponse = "Dear member, your " + strHeader + " request has been received successfully. Please wait shortly as it's being processed.";

                            HashMap<String, String> hmResponse = new HashMap<>();
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal  = theUSSDAPI.mobileMoneyWithdrawal(theUSSDRequest, PESAConstants.PESAType.PESA_OUT, hmResponse);

                            if(transactionReturnVal.equals(USSDAPIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your " + strHeader + " request has been received successfully. Please wait shortly as it's being processed.\n";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your " + strHeader + " request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your " + strHeader + " request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                        break;
                                    }case WITHDRAWAL_LIMIT_VIOLATION: {
                                        String strCBSResponse = hmResponse.get("WITHDRAWAL_LIMIT_VIOLATION");
                                        strResponse = "Dear member, amount requested to Withdraw violates limit RESTRICTION\n" + strCBSResponse + "\n";
                                        break;
                                    }
                                    case INVALID_MOBILE_NUMBER: {
                                        strResponse = "Dear member, you have entered an invalid phone number. Please check the phone number and try again.\n";
                                        break;
                                    }
                                    default: {
                                        strResponse = "Sorry, your " + strHeader + " request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear member, your " + strHeader + " request NOT confirmed. Cash Withdrawal request NOT COMPLETED.\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            //String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                            //String strResponse =  "Confirm Cash Withdrawal\nAmount: KES "+strAmount+"\nAccount: " + strAccount + "\n";

                            String strMobileNo = Long.toString(theUSSDRequest.getUSSDMobileNo());
                            String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());


                            if(strToOption != null) {
                                if(strToOption.equalsIgnoreCase("OTHER_NUMBER")){
                                    strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                                }
                            }

                            String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                            String strSourceAccountNo = hmAccountDetails.get("number");
                            String strSourceAccountName = hmAccountDetails.get("name");
                            String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                            String strSourceAccountLabel = hmAccountDetails.get("label");

                            String strResponse =  "Confirm " + strHeader + "\n{Select a valid menu}" + "\nFrom: "+ strSourceAccountLabel + "\nTo:"+strMobileNo+"\nAmount: KES "+strAmount+"\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                default:{
                    System.err.println("theAppMenus.displayMenu_Withdrawal() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader + "\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Withdrawal() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getWithdrawalOptionMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            LinkedList<APIUtils.WithdrawalChannel> lsWithdrawalChannels = APIUtils.getActiveWithdrawalChannels(MBankingConstants.ApplicationType.USSD);
            for(int i = 0; i < lsWithdrawalChannels.size(); i++){
                String strOptionMenu = String.valueOf(i+1);
                String strName = lsWithdrawalChannels.get(i).getName();
                String strLabel = lsWithdrawalChannels.get(i).getLabel();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strName, strOptionMenu+": "+strLabel);
            }
            /*USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "M-PESA", "1: M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ATM", "2: ATM");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "AGENT", "3: Agent");*/
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_OPTION, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getWithdrawalToOptionMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getWithdrawalToOptionMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_NUMBER", "1: MY phone number");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_NUMBER", "2: OTHER phone number");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getWithdrawalOptionMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
