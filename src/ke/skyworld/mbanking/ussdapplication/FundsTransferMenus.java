package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.*;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.sp.manager.SPManagerConstants;
import ke.skyworld.sp.manager.SPManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public interface FundsTransferMenus {
    default USSDResponse displayMenu_FundTransfer(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect Funds Transfer option\n";
                theUSSDResponse = getFundsTransferOptions(theUSSDRequest, strHeader);

            }else {
                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){
                    case FUNDS_TRANSFER_MENU:{
                        String strFundsTransferOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name());
                        if (strFundsTransferOption.equalsIgnoreCase("FUNDS_TRANSFER_INTERNAL")) {
                            theUSSDResponse = displayMenu_FundTransferInternal(theUSSDRequest, theParam);
                        }else if (strFundsTransferOption.equalsIgnoreCase("FUNDS_TRANSFER_EXTERNAL")) {
                            theUSSDResponse = displayMenu_FundTransferExternal(theUSSDRequest, theParam);
                        }else {
                            String strHeader = "Funds Transfer\n{Select a valid Funds Transfer option}\n";
                            theUSSDResponse = getFundsTransferOptions(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransfer() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransfer() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getFundsTransferOptions(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FUNDS_TRANSFER_INTERNAL", "1: " + AppConstants.strSACCOName + " account");

            boolean blCheckEmployerRestriction = theUSSDAPI.checkEmployerFunctionalityEnabled(theUSSDRequest, "Bank Transfer");

            if(!blCheckEmployerRestriction){
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "FUNDS_TRANSFER_EXTERNAL", "2: Commercial Bank Account");
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_MENU, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundsTransferOptions() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    //Internal Fund Transfer Menus
    default USSDResponse displayMenu_FundTransferInternal(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect source account\n";
                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT);
            }else {
                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){

                    case FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT:{
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                        if(strFromAccount != ""){
                            String strHeader = "Funds Transfer\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid source account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_OPTION:{

                        String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());
                        if (strOption.equalsIgnoreCase("MY_ACCOUNT")) {
                            String strHeader = "Funds Transfer\nSelect destination account";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);

                        } else if (strOption.equalsIgnoreCase("OTHER_ACCOUNT")) {
                            String strHeader = "Funds Transfer\nSelect transfer option\n";
                            theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strHeader);
                        }else {
                            String strHeader = "Funds Transfer\n{Select a valid menu}\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_TO_OPTION:{
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                        if(strToOption.equalsIgnoreCase("Mobile No")){
                            String strResponse = "Funds Transfer\nEnter Mobile No. of the destination acct.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                        }else if(strToOption.equalsIgnoreCase("ID Number")){
                            String strResponse = "Funds Transfer\nEnter ID Number of the destination acct.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid menu}\nSelect transfer option\n";
                            theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER:{
                        String strToIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.name());
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());

                        if(strToIdentifier != ""){

                            if(strToOption.equalsIgnoreCase("Mobile No")){
                                strToIdentifier = APIUtils.sanitizePhoneNumber(strToIdentifier);
                            }

                            if ( ( strToOption.equalsIgnoreCase("Mobile No") ) && ( !strToIdentifier.matches("^2547\\d{8}$") ) ) {
                                String strResponse = "Funds Transfer\n{Please enter a valid Mobile No. of the destination acct.)\n";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                            }else if ( ( strToOption.equalsIgnoreCase("ID Number") ) && ( !strToIdentifier.matches("^\\d{5,15}$") ) ) {
                                String strResponse = "Funds Transfer\n{Please enter a valid ID Number of the destination acct.\n";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                            }else{
                                String strHeader = "Funds Transfer\nSelect Funds Transfer destination acct.\n";
                                theUSSDResponse = GeneralMenus.displayMenu_IdentifierBankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);

                                int intBankAccountsCount = ( (USSDResponseSELECT)  theUSSDResponse ).getUSSDSelectOptionCustomCount();

                                if(intBankAccountsCount == 0){
                                    if ( strToOption.equalsIgnoreCase("Mobile No")  ) {
                                        String strResponse = "Funds Transfer\n{Account NOT Found. Please enter an existing Mobile No. of the destination acct.)\n";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                                    }else if (  strToOption.equalsIgnoreCase("ID Number")  ) {
                                        String strResponse = "Funds Transfer\n{Account NOT Found. Please enter an existing ID Number of the destination acct.\n";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                                    }
                                }
                            }
                        }else{
                            if(strToOption.equalsIgnoreCase("Mobile No")){
                                String strResponse = "Funds Transfer\n{Please enter a valid Mobile No. of the destination acct.)\n";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                            }else if(strToOption.equalsIgnoreCase("ID Number")){
                                String strResponse = "Funds Transfer\n{Please enter a valid ID Number of the destination acct.\n";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER, USSDConstants.USSDInputType.STRING, "NO");
                            }else{
                                String strHeader = "Funds Transfer\n{Select a valid menu}\nSelect transfer option\n";
                                theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strHeader);
                            }
                        }

                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT:{
                        String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                        String strToIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.name());

                        String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                        HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                        String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                        String strFromAccountName =  hmFromAccountNoDetails.get("name");
                        String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                        String strFromAccountLabel = hmFromAccountNoDetails.get("label");

                        String strToAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                        HashMap <String, String> hmToAccountDetails  = Utils.toHashMap(strToAccountDetails);
                        String strToAccountName =  hmToAccountDetails.get("name");
                        String strToAccountNumber = hmToAccountDetails.get("number");
                        String strToAccountTypeName = hmToAccountDetails.get("type_name");
                        String strToAccountLabel = hmToAccountDetails.get("label");
                        String strToFullName =  hmToAccountDetails.get("full_name");

                        if (strToAccountDetails.equals("")) {
                            if (strOption.equalsIgnoreCase("MY_ACCOUNT")) {
                                String strHeader = "Funds Transfer\n{Select a VALID Funds Transfer destination acct.}\n";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);
                            }else{
                                String strHeader = "Funds Transfer\n{Select a VALID Other Funds Transfer destination acct.}\n";
                                theUSSDResponse = GeneralMenus.displayMenu_IdentifierBankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);
                            }
                        }else if (strToAccountNumber.equalsIgnoreCase(strFromAccountNumber)) {
                            if (strOption.equalsIgnoreCase("MY_ACCOUNT")) {
                                String strHeader = "Funds Transfer\n{Select a different destination acct from source acct}\n";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);
                            }else {
                                String strHeader = "Funds Transfer\n{Select a different destination acct from source acct}\n";
                                theUSSDResponse = GeneralMenus.displayMenu_IdentifierBankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);
                            }
                        } else {
                            String strResponse = "Funds Transfer\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_AMOUNT:{
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strResponse = "Funds Transfer\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            String strFundsTransferMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
                            String strFundsTransferMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();

                            String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                            HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                            String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                            String strFromAccountName =  hmFromAccountNoDetails.get("name");
                            String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                            String strFromAccountLabel = hmFromAccountNoDetails.get("label");
                            String strFromAccountAvailableBalance = hmFromAccountNoDetails.get("avail_bal");

                            double dblAvailableBalance = 0;
                            try { dblAvailableBalance = Double.parseDouble(strFromAccountAvailableBalance); }catch (Exception e){}

                            double dblFundsTransferMinimum = Double.parseDouble(strFundsTransferMinimum);
                            double dblFundsTransferMaximum = Double.parseDouble(strFundsTransferMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if (dblAmountEntered < dblFundsTransferMinimum) {
                                strResponse = "Funds Transfer\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else if (dblAmountEntered > dblFundsTransferMaximum) {
                                strResponse = "Funds Transfer\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else if(Double.parseDouble(strAmount) > dblAvailableBalance){
                                strResponse = "\n{Funds Transfer " +strFromAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblAvailableBalance,"#,###.##") + " is INSUFFICIENT to withdraw KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                            }

                        } else {
                            String strResponse = "Funds Transfer\n{Please enter a valid amount}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_PIN:{
                        String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN.name());
                        if (strLoginPIN.equals(strPIN)) {

                            String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                            HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                            String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                            String strFromAccountName =  hmFromAccountNoDetails.get("name");
                            String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                            String strFromAccountLabel = hmFromAccountNoDetails.get("label");

                            String strToIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.name());

                            String strToAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                            HashMap <String, String> hmToAccountDetails  = Utils.toHashMap(strToAccountDetails);
                            String strToAccountName =  hmToAccountDetails.get("name");
                            String strToAccountNumber = hmToAccountDetails.get("number");
                            String strToAccountTypeName = hmToAccountDetails.get("type_name");
                            String strToAccountLabel = hmToAccountDetails.get("label");
                            String strToFullName =  hmToAccountDetails.get("full_name");

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());
                            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());

                            String strFormattedAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Funds Transfer\nFrom A/C: " + strFromAccountNumber + "\n" + "To A/C: " + strToAccountLabel + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                            if(strOption.equalsIgnoreCase("OTHER_ACCOUNT")){
                                String strOptionTo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());

                                strResponse = "Confirm Funds Transfer\nFrom A/C: " + strFromAccountNumber + "\nTo A/C: " + strToAccountNumber + "\nName: " + strToAccountName + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                            } else {
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            }

                        } else {
                            String strResponse = "Funds Transfer\n{Please enter correct PIN}\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_CONFIRMATION:{
                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.\n";

                            Thread worker = new Thread(() -> {
                                USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fundsTransfer(theUSSDRequest);
                                System.out.println("fundsTransfer: "+transactionReturnVal.getValue());
                            });
                            worker.start();

                            /*APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fundsTransfer(theUSSDRequest);


                            if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";
                            }else {

                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN:
                                    case INVALID_ACCOUNT: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Sorry the source acount has insufficient balance for this transaction. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    default: {
                                        strResponse = "Sorry, your Funds Transfer request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }*/

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO",theArrayListUSSDSelectOption);

                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your Funds Transfer request NOT confirmed. Funds Transfer request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        } else {

                            String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                            HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                            String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                            String strFromAccountName =  hmFromAccountNoDetails.get("name");
                            String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                            String strFromAccountLabel = hmFromAccountNoDetails.get("label");

                            String strToIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.name());

                            String strToAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                            HashMap <String, String> hmToAccountDetails  = Utils.toHashMap(strToAccountDetails);

                            String strToAccountName =  hmToAccountDetails.get("name");
                            String strToAccountNumber = hmToAccountDetails.get("number");
                            String strToAccountTypeName = hmToAccountDetails.get("type_name");
                            String strToAccountLabel = hmToAccountDetails.get("label");
                            String strToFullName =  hmToAccountDetails.get("full_name");

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());
                            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());

                            String strFormattedAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Funds Transfer\n{Select a valid menu}\nFrom A/C: " + strFromAccountNumber + "\n" + "To A/C: " + strFromAccountLabel + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                            if(strOption.equalsIgnoreCase("OTHER_ACCOUNT")){
                                String strOptionTo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                                strResponse = "Confirm Funds Transfer\n{Select a valid menu}\nFrom A/C: " + strFromAccountNumber + "\nTo A/C: " + strToAccountNumber + "\nName: " + strToAccountName + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransferInternal() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransferInternal() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getFundTransferInternalToOptionMenu(USSDRequest theUSSDRequest, String strHeader) {

        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "Mobile No", "1: Mobile No");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ID Number", "2: ID Number");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION, "NO",theArrayListUSSDSelectOption);

        }catch(Exception e){
            System.err.println("theAppMenus.getFundTransferInternalOptionMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }

        return theUSSDResponse;
    }

    default USSDResponse getFundTransferInternalOptionMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_ACCOUNT", "1: My account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_ACCOUNT", "2: Other account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundTransferInternalOptionMenu() ERROR : " + e.getMessage());
        }
            finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    //External Fund Transfer Menus
    default USSDResponse displayMenu_FundTransferExternalBanks(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                int intOptionMenu = llSPAAccounts.indexOf(serviceProviderAccount);
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
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundsTransferOptions() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_FundTransferExternal(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect Bank to transfer funds";
                theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);

            }else {

                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){
                    case FUNDS_TRANSFER_EXTERNAL_BANK:{
                        String strToBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                        if(strToBank != ""){
                            theUSSDResponse = displayMenu_FundTransferExternal_Maintain_Accounts(theUSSDRequest, theParam);
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid Bank to transfer funds}";
                            theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO:{
                        String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());

                        HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

                        String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
                        String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
                        String strToAccountType = hmBankAccountDetails.get("type");
                        String strToAccountNaming = hmBankAccountDetails.get("type_tag");
                        String strToBankName = hmBankAccountDetails.get("long_tag");

                        String strMenuOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                        String strAction = "";
                        if(!strMenuOption.isEmpty()){
                            HashMap<String, String> hmMenuOption = Utils.toHashMap(strMenuOption);
                            strAction = hmMenuOption.get("ACTION");
                        }

                        switch (strAction) {
                            case "CHOICE": {
                                String strHeader = "Funds Transfer\nSelect funds source account\n";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                                break;
                            }case "ADD": {

                                String strResponse = "Add " + strToBankName + " " + strToAccountNaming + "\nEnter " + strToAccountNaming + ":";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                break;
                            }
                            case "REMOVE": {
                                String strHeader2 = "Remove " + strToBankName + " " + strToAccountNaming +"\nSelect " + strToAccountNaming + " to Remove:";
                                theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader2, USSDConstants.Condition.NO);
                                break;
                            }
                            default:{
                                String strHeader = "Funds Transfer\n{Select a VALID MENU}:\n";
                                theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                                break;
                            }
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME:{
                        String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());

                        HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

                        String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
                        String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
                        String strToAccountType = hmBankAccountDetails.get("type");
                        String strToAccountNaming = hmBankAccountDetails.get("type_tag");
                        String strToBankName = hmBankAccountDetails.get("long_tag");

                        String strToBankAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME.name());
                        if(strToBankAccountName.length() >= 3){ //Three or more Characters
                            String strHeader = "Funds Transfer\nSelect funds source account\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                        }else{
                            String strHeader = "Funds Transfer\n{Enter a valid "+strToBankName+" account name to receive funds}:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT:{
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
                        if(strFromAccount != ""){
                            String strResponse = "Funds Transfer\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid funds source account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_AMOUNT:{
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strResponse = "Funds Transfer\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            String strFundsTransferMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum();
                            String strFundsTransferMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum();

                            String strSourceAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strSourceAccountDetails);

                            String strSourceAccountNo = hmAccountDetails.get("number");
                            String strSourceAccountName = hmAccountDetails.get("name");
                            String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                            String strSourceAccountLabel = hmAccountDetails.get("label");
                            String strSourceAccountAvailableBalance = hmAccountDetails.get("avail_bal");

                            double dblAvailableBalance = 0;
                            try { dblAvailableBalance = Double.parseDouble(strSourceAccountAvailableBalance); }catch (Exception e){}

                            double dblFundsTransferMinimum = Double.parseDouble(strFundsTransferMinimum);
                            double dblFundsTransferMaximum = Double.parseDouble(strFundsTransferMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if (dblAmountEntered < dblFundsTransferMinimum) {
                                strResponse = "Funds Transfer\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else if (dblAmountEntered > dblFundsTransferMaximum) {
                                strResponse = "Funds Transfer\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else if(Double.parseDouble(strAmount) > dblAvailableBalance){
                                strResponse = "Funds Transfer\n{" +strSourceAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblAvailableBalance,"#,###.##") + " is INSUFFICIENT to withdraw KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                            }
                        } else {
                            String strResponse = "Funds Transfer\n{Please enter a valid amount}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_PIN:{
                        String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.name());
                        if (strLoginPIN.equals(strPIN)) {

                            String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                            HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

                            String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
                            String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
                            String strToAccountType = hmBankAccountDetails.get("type");
                            String strToAccountNaming = hmBankAccountDetails.get("type_tag");
                            String strToBankName = hmBankAccountDetails.get("long_tag");

                            String strToBankAccountNoHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                            HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoHashMap);
                            String strAccountID = hmAccount.get("ACCOUNT_ID");
                            String strAccountName = hmAccount.get("ACCOUNT_NAME");
                            String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                            String strSourceAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strSourceAccountDetails);

                            String strSourceAccountNo = hmAccountDetails.get("number");
                            String strSourceAccountName = hmAccountDetails.get("name");
                            String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                            String strSourceAccountLabel = hmAccountDetails.get("label");

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Confirm Funds Transfer to "+ strToBankName +"\nA/C: " + strAccountIdentifier +
                                        " - " + strAccountName + "\nFrom A/C: " + strSourceAccountLabel + "\nAmount: KES " + strAmount + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        } else {
                            String strResponse = "Funds Transfer\n{Please enter correct PIN}\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_CONFIRMATION:{
                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";

                            HashMap<String, String> hmResponse = new HashMap<>();
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.bankTransferViaB2B(theUSSDRequest, PESAConstants.PESAType.PESA_OUT,hmResponse);

                            if(transactionReturnVal.equals(USSDAPIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.\n";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case WITHDRAWAL_LIMIT_VIOLATION: {
                                        String strCBSResponse = hmResponse.get("WITHDRAWAL_LIMIT_VIOLATION");
                                        strResponse = "Dear member, amount requested for Funds Transfer violates limit RESTRICTION\n" + strCBSResponse + "\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                        break;
                                    }

                                    default: {
                                        strResponse = "Sorry, your Funds Transfer request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO",theArrayListUSSDSelectOption);

                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your Funds Transfer request NOT confirmed. Funds Transfer request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                            HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

                            String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
                            String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
                            String strToAccountType = hmBankAccountDetails.get("type");
                            String strToAccountNaming = hmBankAccountDetails.get("type_tag");
                            String strToBankName = hmBankAccountDetails.get("long_tag");


                            String strToBankAccountNoHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                            HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoHashMap);
                            String strAccountID = hmAccount.get("ACCOUNT_ID");
                            String strAccountName = hmAccount.get("ACCOUNT_NAME");
                            String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                            String strSourceAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strSourceAccountDetails);

                            String strSourceAccountNo = hmAccountDetails.get("number");
                            String strSourceAccountName = hmAccountDetails.get("name");
                            String strSourceAccountTypeName = hmAccountDetails.get("type_name");
                            String strSourceAccountLabel = hmAccountDetails.get("label");

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Confirm Funds Transfer to "+ strToBankName +"\n{Select a valid menu}\nA/C: " + strAccountIdentifier +
                                    " - " + strAccountName + "\nFrom A/C: " + strSourceAccountLabel + "\nAmount: KES " + strAmount + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransfer() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransfer() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse displayMenu_FundTransferExternal_Maintain_Accounts(USSDRequest theUSSDRequest, String theParam){
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());
            String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());

            HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

            String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
            String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
            String strToAccountType = hmBankAccountDetails.get("type");
            String strToAccountNaming = hmBankAccountDetails.get("type_tag");
            String strToBankName = hmBankAccountDetails.get("long_tag");

            switch (ussdDataType) {
                case FUNDS_TRANSFER_EXTERNAL_BANK: {
                    if(strProviderBankAccountDetails != ""){
                        String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strToAccountNaming+" to receive funds:\n";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    }else{
                        String strHeader = "Funds Transfer\n{Select a valid Bank to transfer funds}";
                        theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);
                    }

                    break;
                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT: {
                    String theAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());

                    if( theAccountNo.matches("^\\d{6,24}$")  ) { //6 - 24 Digits

                        String strResponse = "Add " + strToBankName + " " + strToAccountNaming + "\nEnter the NAME of the account HOLDER\n(" + strToBankName + " "  +strToAccountNaming+ " " + theAccountNo + "):";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                    }else{
                        String strResponse = "Add " + strToBankName + " " + strToAccountNaming + "\n{Enter a VALID "+strToAccountNaming+"}:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;

                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME: {
                    //ADD Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());
                    String strAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME.name());

                    try{
                        String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                        SPManager spManager = new SPManager(strIntegritySecret);
                        spManager.createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN,strMobileNo,strToSPProviderAccountCode, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO,strAccountNo, strAccountName);


                    }catch (Exception e){
                        System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
                    }

                    String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strToAccountNaming+" to receive funds:\n";
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE: {
                    //REMOVE Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE.name());

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
                            System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
                        }

                        String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strToAccountNaming+" to receive funds:\n";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    }else{
                        String strHeader = "Remove " + strToBankName + " " + strToAccountNaming +"\n{Select a VALID MENU}:";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.NO);
                    }

                    break;
                }
                default:{
                    String strHeader = "Funds Transfer\n{Select a VALID "+strToBankName+" "+ strToAccountNaming+" to receive funds}:\n";
                    System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() UNKNOWN PARAM ERROR : strUSSDDataType = " + ussdDataType.name());

                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, strToAccountType, strToAccountNaming, strToSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
