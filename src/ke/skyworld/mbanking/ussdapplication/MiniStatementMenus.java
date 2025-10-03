package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface MiniStatementMenus {

    default USSDResponse displayMenu_MiniStatement(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String strHeader = "Mini Statement";
            USSDAPIConstants.AccountType accountType = USSDAPIConstants.AccountType.ALL;

            switch (theParam){
                case "MENU": {
                    strHeader = "Mini Statement";
                    theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE);
                    break;
                }
                default: {

                    String strAccountType = null;

                    AppConstants.USSDDataType ussdDataType = getMiniStatementEnquiryCallerMenu(theUSSDRequest.getUSSDData().toString());

                    switch (ussdDataType){
                        case MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE:{
                            strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE.name());
                            break;
                        }
                        case LOAN_MENU:{
                            strAccountType = USSDAPIConstants.AccountType.LOAN.getValue();
                            break;
                        }
                    }

                    switch (strAccountType){
                        case "FOSA":{
                            strHeader = "Savings Account Mini Statement";
                            accountType = USSDAPIConstants.AccountType.FOSA;
                            break;
                        }
                        case "BOSA":{
                            strHeader = "Shares, Deposits and Benevolent Mini Statement";
                            accountType = USSDAPIConstants.AccountType.BOSA;
                            break;
                        }
                        case "LOAN":{
                            strHeader = "Loan Mini Statement";
                            accountType = USSDAPIConstants.AccountType.LOAN;
                            break;
                        }
                    }

                    theUSSDResponse =  displayMenu_MiniStatementMenus(theUSSDRequest, theParam , accountType, strHeader);
                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_MiniStatement() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_MiniStatementMenus(USSDRequest theUSSDRequest, String theParam, USSDAPIConstants.AccountType theAccountType, String theHeader) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String strHeader = theHeader;

            switch (theParam) {
                case "ACCOUNT_TYPE": {
                    String strAccountType = theAccountType.getValue();
                    //String strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE.name());
                    if(!strAccountType.equals("")){
                        switch (theAccountType){
                            case FOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                            case  BOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                            case LOAN:{
                                String strResponse  = strHeader + "\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                        }
                        break;
                    }else{
                        strHeader = strHeader+"\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE);
                    }
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT.name());

                    if(!strAccount.equals("")){
                        String strResponse = strHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse  = strHeader + "\n{Select a valid menu}";
                        switch (theAccountType){
                            case FOSA:{
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                            case  BOSA:{
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                            case LOAN:{
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, USSDAPIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT);
                                break;
                            }
                        }
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN.name());
                    if(strLoginPIN.equals(strPIN)){
                        String strResponse = "Dear member, your "+strHeader+" request has been received successfully. Please wait shortly as it's being processed.\n";

                        Thread worker = new Thread(() -> {
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.accountMiniStatement(theUSSDRequest, theAccountType);
                            System.out.println("accountMiniStatement: "+transactionReturnVal.getValue());
                        });
                        worker.start();

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_END, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader+"\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_MiniStatementMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "My Account\n{Sorry, an error has occurred while processing your request}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_MiniStatementMenus() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default AppConstants.USSDDataType getMiniStatementEnquiryCallerMenu(String theUSSDData){

        AppConstants.USSDDataType ussdDataType = AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE;

        try{

            int intMY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE = theUSSDData.lastIndexOf(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE.name());
            int intLOAN_MENU = theUSSDData.lastIndexOf(AppConstants.USSDDataType.LOAN_MENU.name());

            ussdDataType = (intMY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE > intLOAN_MENU) ? AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE : AppConstants.USSDDataType.LOAN_MENU;
        }catch(Exception e){
            System.err.println("theAppMenus.getMiniStatementEnquiryCallerMenu() ERROR : " + e.getMessage());
        }
        finally{

        }
        return ussdDataType;
    }

}
