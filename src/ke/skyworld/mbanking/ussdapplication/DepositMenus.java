package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;

public interface DepositMenus {

    default USSDResponse displayMenu_DepositMenu(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SAVINGS_ACCOUNT", "1: Savings Deposit via M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "SHARES_AND_DEPOSITS", "2: Shares, Deposits and Benevolent via M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "PAY_LOAN", "3: Pay Loan");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_MENU, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_DepositMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Deposit(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if(strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())){
                String strHeader = "Payments and Deposit";
                theUSSDResponse =  displayMenu_DepositMenu(theUSSDRequest, theParam, strHeader);
            }else{ //LOAN_MENU

                String strDEPOSIT_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_MENU.name());

                switch (strDEPOSIT_MENU) {
                    case "SAVINGS_ACCOUNT": {
                        String strHeader = "Savings Deposit via M-PESA";
                        theUSSDResponse = theAppMenus.displayMenu_AccountsDeposit(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                    case "SHARES_AND_DEPOSITS": {
                        String strHeader = "Shares, Deposits and Benevolent via M-PESA";
                        theUSSDResponse = theAppMenus.displayMenu_AccountsDeposit(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                    case "PAY_LOAN": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanRepayment(theUSSDRequest, theParam);
                        break;
                    }
                    default:{
                        String strHeader = "Payments and Deposit\n{Select a valid menu}";
                        theUSSDResponse =  displayMenu_DepositMenu(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Deposit() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_AccountsDeposit(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);
        String strSender = pesaParam.getSenderIdentifier();

        try {

            switch (theParam) {
                case "MENU": {
                    String strHeader = theHeader + "\nSelect account";
                    theUSSDResponse = getBankAccounts(theUSSDRequest, theParam, strHeader);
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());

                    if(strAccount != ""){
                        String strResponse = theHeader +"\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strHeader = theHeader+"\n{Select a valid menu}";
                        theUSSDResponse = getBankAccounts(theUSSDRequest, theParam, strHeader);
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                    if(strAmount.matches("^[1-9][0-9]*$")) {
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                        HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                        String strAccountNo = hmAccountDetails.get("number");
                        String strAccountName = hmAccountDetails.get("name");
                        String strAccountTypeName = hmAccountDetails.get("type_name");
                        String strAccountLabel = hmAccountDetails.get("label");

                        String strResponse = "Confirm " + theHeader + "\nPaybill no.: " + strSender + "\nAccount: " + strAccountLabel + "\n" + "Amount: KES " + strAmount + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        String strDepositMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.DEPOSIT).getMinimum();
                        String strDepositMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.DEPOSIT).getMaximum();

                        double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
                        double dblDepositMaximum = Double.parseDouble(strDepositMaximum);

                        double dblAmountEntered = Double.parseDouble(strAmount);

                        if (dblAmountEntered < dblDepositMinimum) {
                            strResponse = theHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strDepositMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        if (dblAmountEntered > dblDepositMaximum) {
                            strResponse = theHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strDepositMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    }else{
                        String strResponse = theHeader+"\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                /* SKIP PIN REQUEST
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                    if(strAmount.matches("^[1-9][0-9]*$")){
                        String strResponse = theHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.DEPOSIT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = theHeader+"\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_PIN.name());
                    if(strLoginPIN.equals(strPIN)){
                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm "+theHeader+"\nAccount: "+strAccount+"\n" + "Amount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = theHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.DEPOSIT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                 */
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{
                            String strResponse ="";

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                            String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                            String strAccountNo = hmAccountDetails.get("number");
                            String strAccountName = hmAccountDetails.get("name");
                            String strAccountTypeName = hmAccountDetails.get("type_name");
                            String strAccountLabel = hmAccountDetails.get("label");

                            if(theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()){

                                strResponse = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + "\n" + "A/C: " + strAccountNo + "\n" + "Amount: KES " + strAmount + "\n";

                                String strOriginatorID = theUSSDRequest.getUSSDTraceID();
                                String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                String strReceiverDetails = strReceiver;
                                Double lnAmount = Utils.stringToDouble(strAmount);
                                String strReference = strOriginatorID;

                                Thread worker = new Thread(() -> {
                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(strOriginatorID, theUSSDRequest.getUSSDTraceID(), strReceiver, strReceiverDetails, strAccountNo, "KES", lnAmount, "ACCOUNT_DEPOSIT", strReference, "USSD", "MBANKING");
                                });
                                worker.start();
                            }else{
                                strResponse = "Use the details below to pay via M-PESA\nPaybill no: " + strSender + "\n" + "A/C: " + strAccountNo + "\n" + "Amount: KES " + strAmount + "\n";
                            }

                            //End USSD.
                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                            /*Cont USSD
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO",theArrayListUSSDSelectOption);
                              */
                            break;
                        }case "NO":{
                            String strResponse = "Dear member, your "+theHeader+" request NOT confirmed. "+theHeader+" request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);

                            String strAccountNo = hmAccountDetails.get("number");
                            String strAccountName = hmAccountDetails.get("name");
                            String strAccountTypeName = hmAccountDetails.get("type_name");
                            String strAccountLabel = hmAccountDetails.get("label");

                            String strResponse = "Confirm " + theHeader + "\n{Select a valid menu}\nPaybill no.: " + strSender + "\nAccount: " + strAccountLabel + "\n" + "Amount: KES " + strAmount + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_AccountsDeposit() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = theHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AccountsDeposit() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getBankAccounts(USSDRequest theUSSDRequest, String theParam, String strHeader) {
        USSDResponse theUSSDResponse = null;

        try{
            String strAccountTypes = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_MENU.name());
            switch (strAccountTypes){
                case "SAVINGS_ACCOUNT":{
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.DEPOSIT_ACCOUNT);
                    break;
                }
                case "SHARES_AND_DEPOSITS":{
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.BOSA, AppConstants.USSDDataType.DEPOSIT_ACCOUNT);
                    break;
                }
                case "PAY_LOAN":{
                    theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.LOAN, AppConstants.USSDDataType.DEPOSIT_ACCOUNT);
                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.getBankAccounts() ERROR : " + e.getMessage());
        }
        finally{

        }
        return theUSSDResponse;
    }

}
