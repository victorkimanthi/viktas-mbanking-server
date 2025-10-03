package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface BalanceEnquiryMenus {

    default USSDResponse displayMenu_BalanceEnquiry(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String  strHeader = "Balance Enquiry";
            USSDAPIConstants.AccountType accountType = USSDAPIConstants.AccountType.ALL;

            switch (theParam){
                case "MENU": {
                    strHeader = "Balance Enquiry";
                    theUSSDResponse = GeneralMenus.displayMenu_AccountTypesPlusALL(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    break;
                }
                default: {

                    String strAccountType = null;

                    AppConstants.USSDDataType ussdDataType = getBalanceEnquiryCallerMenu(theUSSDRequest.getUSSDData().toString());

                    switch (ussdDataType){
                        case MY_ACCOUNT_BALANCE_ACCOUNT_TYPE:{
                            strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
                            break;
                        }
                        case LOAN_MENU:{
                            strAccountType = USSDAPIConstants.AccountType.LOAN.getValue();
                            break;
                        }
                    }

                    if (strAccountType != null) {
                        switch (strAccountType){
                            case "FOSA":{
                                strHeader = "Savings Accounts Balance Enquiry";
                                accountType = USSDAPIConstants.AccountType.FOSA;
                                break;
                            }
                            case "BOSA":{
                                strHeader = "Shares, Deposits and Benevolent Balance Enquiry";
                                accountType = USSDAPIConstants.AccountType.BOSA;
                                break;
                            }
                            case "LOAN":{
                                strHeader = "Loans Balance Enquiry";
                                accountType = USSDAPIConstants.AccountType.LOAN;
                                break;
                            }
                            case "ALL":{
                                strHeader = "All Accounts Balance Enquiry";
                                accountType = USSDAPIConstants.AccountType.ALL;
                                break;
                            }
                        }
                    }
                    theUSSDResponse =  displayMenu_BalanceEnquiryMenus(theUSSDRequest, theParam , accountType, strHeader);
                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BalanceEnquiry() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_BalanceEnquiryMenus(USSDRequest theUSSDRequest, String theParam, USSDAPIConstants.AccountType theAccountType, String theHeader) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try{
            String strHeader = theHeader;

            switch (theParam) {
                case "ACCOUNT_TYPE": {
                    String strAccountType = theAccountType.getValue();
                    //String strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
                    if(!strAccountType.equals("")){
                        strHeader = strHeader+"\nSelect a category:";
                        String strResponse = theHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING,"NO");
                        break;
                    }else{
                        strHeader = strHeader+"\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_AccountTypesPlusALL(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strResponse = "Dear member, your "+strHeader+" request has been received successfully. Please wait shortly as it's being processed.\n";

                        Thread worker = new Thread(() -> {
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.accountBalanceEnquiry(theUSSDRequest, theAccountType);
                            System.out.println("accountBalanceEnquiry: "+transactionReturnVal.getValue());
                        });
                        worker.start();

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader+"\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default AppConstants.USSDDataType getBalanceEnquiryCallerMenu(String theUSSDData){

        AppConstants.USSDDataType ussdDataType = AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE;

        try{

            int intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE = theUSSDData.lastIndexOf(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
            int intLOAN_MENU = theUSSDData.lastIndexOf(AppConstants.USSDDataType.LOAN_MENU.name());

            ussdDataType = (intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE > intLOAN_MENU) ? AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE : AppConstants.USSDDataType.LOAN_MENU;
        }catch(Exception e){
            System.err.println("theAppMenus.getBalanceEnquiryCallerMenu() ERROR : " + e.getMessage());
        }
        finally{

        }
        return ussdDataType;
    }
}
