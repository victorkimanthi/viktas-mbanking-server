package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.*;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.sp.manager.SPManagerConstants;
import ke.skyworld.sp.manager.SPManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public interface GeneralMenus {

    static USSDResponse displayMenu_BankAccounts(USSDRequest theUSSDRequest, String theParam, String theHeader, USSDAPIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();


            LinkedHashMap<String, LinkedHashMap <String, String>>  accounts = theUSSDAPI.getBankAccounts(theUSSDRequest, theAccountType);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            int count = 0;

            if(accounts != null) {


                for (String account_no : accounts.keySet()) {
                    count++;
                    //System.out.println("account: " + account_no + " account type: " + accounts.get(account_no));

                    LinkedHashMap <String, String> hmAccount = accounts.get(account_no);

                    String strAccountLabel = hmAccount.get("label");

                    String strOptionValue  = Utils.serialize(hmAccount);
                    String strOptionMenu = Integer.toString(count);

                    String strOptionDisplayText = strOptionMenu + ": " + strAccountLabel;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            ( (USSDResponseSELECT)  theUSSDResponse ).setUSSDSelectOptionCustomCount(count);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BankAccounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_IdentifierBankAccounts(USSDRequest theUSSDRequest, String theParam, String theHeader, USSDAPIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try{
            //
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            HashMap<Object, Object>  hmRVal = theUSSDAPI.getIdentifierBankAccounts(theUSSDRequest, theAccountType);

            HashMap<String, HashMap <String, String>>  accounts = (HashMap<String, HashMap <String, String>>) hmRVal.get("accounts");
            HashMap<String, String>  hmUserDetails = (HashMap<String, String>) hmRVal.get("user_details");


            //String strRequestStatus = (String) hmUserDetails.get("request_status");
            //String strMemberNumber = (String)  hmUserDetails.get("member_number");
            String strFullName = (String) hmUserDetails.get("full_name");
            //String strIdentifierType = (String) hmUserDetails.get("identifier_type");
            //String strIdentifier = (String) hmUserDetails.get("identifier");
            //String strIdentityType = (String) hmUserDetails.get("identity_type");
            //String strIdentity = (String) hmUserDetails.get("identity");

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            int count = 0;

            if(accounts != null) {

                for (String account_no : accounts.keySet()) {
                    count++;
                    //System.out.println("account: " + account_no + " account type: " + accounts.get(account_no));

                    HashMap <String, String> hmAccount = accounts.get(account_no);
                    hmAccount.put("full_name", strFullName);

                    String strOptionValue  = Utils.serialize(hmAccount);
                    String strOptionMenu = Integer.toString(count);

                    String strAccountLabel = hmAccount.get("label");

                    String strOptionDisplayText = strOptionMenu + ": " + strAccountLabel;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            ( (USSDResponseSELECT)  theUSSDResponse ).setUSSDSelectOptionCustomCount(count);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_IdentifierBankAccounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_Loans(USSDRequest theUSSDRequest, String theParam, String theHeader, USSDAPIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            HashMap<String, HashMap<String, String>>  loans = theUSSDAPI.getLoans(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            int count = 0;

            if(loans != null) {

                for (String loan_id : loans.keySet()) {
                    count++;
                    HashMap<String, String> hmLoan = loans.get(loan_id);

                    String strLoanAccountLabel= hmLoan.get("label");

                    String strOptionValue = Utils.serialize(hmLoan);
                    String strOptionMenu = Integer.toString(count);

                    String strOptionDisplayText = strOptionMenu + ": " + strLoanAccountLabel;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            ( (USSDResponseSELECT)  theUSSDResponse ).setUSSDSelectOptionCustomCount(count);

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanTypes(USSDRequest theUSSDRequest, String theParam, String theHeader, USSDAPIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            HashMap<String, HashMap<String, String>>  loanTypes = theUSSDAPI.getLoanTypes(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            int count = 0;

            if(loanTypes != null) {

                for (String loanType : loanTypes.keySet()) {
                    count++;
                    String strAccount = loanType;
                    HashMap<String, String> hmLoanType = loanTypes.get(loanType);

                    String strOptionValue = Utils.serialize(hmLoanType);
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");

                    String strOptionMenu = Integer.toString(count);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoanTypeName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            ( (USSDResponseSELECT)  theUSSDResponse ).setUSSDSelectOptionCustomCount(count);

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountGroups(USSDRequest theUSSDRequest, String theParam, String theHeader, USSDAPIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            HashMap<String, String> accounts = theUSSDAPI.getAccountGroups(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            int count = 0;

            if(accounts != null) {

                for (String account : accounts.keySet()) {
                    count++;
                    System.out.println("group: " + account + " group id: " + accounts.get(account));
                    String strAccount = account;
                    String strAccountType = accounts.get(account);

                    String strOptionValue = strAccount;
                    String strOptionMenu = Integer.toString(count);
                    String strOptionDisplayText = strOptionMenu + ": " + strAccountType;// + " (" + strAccount + ")"; //"1: Member Acct.(10101010101)"

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            ( (USSDResponseSELECT)  theUSSDResponse ).setUSSDSelectOptionCustomCount(count);

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BankAccounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountTypes(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FOSA", "1: Savings Accounts");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "BOSA", "2: Shares, Deposits and Benevolent");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "LOAN", "3: Loans");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountTypesPlusALL(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FOSA", "1: Savings Accounts");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "BOSA", "2: Shares, Deposits and Benevolent");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "LOAN", "3: Loans");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "ALL", "4: All Accounts");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AccountTypesPlusALL() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountCategories(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "PERSONAL", "1: Personal Account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "GROUP", "2: Group Account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanCategories(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "PERSONAL", "1: Personal Loan");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "GROUP", "2: Group Loan");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse getAccountMaintenanceMenus(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theAccountType, String theAccountNaming, String theSPProviderAccountCode, String theHeader, USSDConstants.Condition theDesplayAddRemove) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            try{

                String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                SPManager spManager = new SPManager(strIntegritySecret);
                LinkedList<LinkedHashMap<String, String>> listAccounts = spManager.getUserSavedAccountsByProvider(SPManagerConstants.UserIdentifierType.MSISDN, strMobileNo, theSPProviderAccountCode);

                //todo REMOVE CODE
                System.out.println("\n\n**** START LIST ACCOUNTS ****");
                System.out.println(listAccounts.toString());
                System.out.println("**** END LIST ACCOUNTS ****\n\n");

                int count = 0;
                for (int i = 1; i <= listAccounts.size() ; i++) {
                    LinkedHashMap<String, String> account = listAccounts.get((i-1));

                    String strUserAccountID = account.get("user_account_id");
                    String strUserAccountName = account.get("user_account_name");
                    String strUserAccountIdentifier = account.get("user_account_identifier");
                    String strIntegrityHashViolated = account.get("integrity_hash_violated");

                    HashMap<String, String> hmOptionValueAccount = new HashMap<>();
                    hmOptionValueAccount.put("ACTION","CHOICE");
                    hmOptionValueAccount.put("ACCOUNT_ID",strUserAccountID);
                    hmOptionValueAccount.put("ACCOUNT_NAME",strUserAccountName);
                    hmOptionValueAccount.put("ACCOUNT_IDENTIFIER",strUserAccountIdentifier);
                    String strOptionValueAccount= Utils.serialize(hmOptionValueAccount);

                    String strOptionDisplayText = i + ": " + strUserAccountName + " (" + strUserAccountIdentifier + ")";

                    if(strIntegrityHashViolated.equalsIgnoreCase(USSDConstants.Condition.NO.getValue())){
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(i), strOptionValueAccount, strOptionDisplayText);
                        count++;
                    }
                }
            }catch (Exception e){
                System.err.println("GeneralMenus.getAccountMaintenanceMenus() ERROR : " + e.getMessage());
            }


            if(theDesplayAddRemove.equals(USSDConstants.Condition.YES)){
                HashMap<String, String> hmOptionValueADD = new HashMap<>();
                hmOptionValueADD.put("ACTION","ADD");
                hmOptionValueADD.put("ACCOUNT_TYPE",theAccountType);
                String strOptionValueADD = Utils.serialize(hmOptionValueADD);

                HashMap<String, String> hmOptionValueREMOVE = new HashMap<>();
                hmOptionValueREMOVE.put("ACTION","REMOVE");
                hmOptionValueREMOVE.put("ACCOUNT_TYPE",theAccountType);
                String strOptionValueREMOVE = Utils.serialize(hmOptionValueREMOVE);

                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "88", strOptionValueADD, "88: Add " + theAccountNaming);
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "99", strOptionValueREMOVE, "99: Remove " + theAccountNaming);
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);


        }catch(Exception e){
            System.err.println("GeneralMenus.getAccountMaintenanceMenus() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

}

