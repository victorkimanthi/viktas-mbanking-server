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
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.xtreme.XTremeDBCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface LoansMenus {
    default USSDResponse displayMenu_Loan(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if (strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())) {
                String strHeader = "Loans";
                theUSSDResponse = getLoansMenus(theUSSDRequest, strHeader);
            } else { //LOAN_MENU

                String strLOAN_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_MENU.name());

                switch (strLOAN_MENU) {
                    case "CHECK_QUALIFICATION": {
                        theUSSDResponse = theAppMenus.displayMenu_CheckLoanQualification(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHECK_GUARANTORSHIP_ABILITY": {
                        theUSSDResponse = theAppMenus.displayMenu_CheckLoanGuarantorshipAbility(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_APPLICATION": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanApplication(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_REPAYMENT": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanRepayment(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_BALANCE": {
                        theParam = "ACCOUNT_TYPE"; //OVERRIDE and start at ACCOUNT_TYPE
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_MINI_STATEMENT": {
                        theParam = "ACCOUNT_TYPE"; //OVERRIDE and start at ACCOUNT_TYPE
                        theUSSDResponse = theAppMenus.displayMenu_MiniStatement(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_GUARANTORS": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanGuarantors(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOANS_GUARANTEED": {
                        theUSSDResponse = theAppMenus.displayMenu_LoansGuaranteed(theUSSDRequest, theParam);
                        break;
                    }

                    default: {
                        String strHeader = "Loans\n{Select a valid menu}";
                        theUSSDResponse = getLoansMenus(theUSSDRequest, strHeader);

                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getLoansMenus(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "CHECK_QUALIFICATION", "1: Check Loan Limit");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "CHECK_GUARANTORSHIP_ABILITY", "2: Check Guarantorship Ability");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "LOAN_APPLICATION", "3: Apply Loan");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "LOAN_REPAYMENT", "4: Pay Loan");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "LOAN_BALANCE", "5: Loan Balance");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "LOAN_MINI_STATEMENT", "6: Loan Mini-Statement");
            /*USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "LOAN_GUARANTORS", "7: Loan Guarantors");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "8", "LOANS_GUARANTEED", "8: Loans Guaranteed");*/
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_MENU, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.getLoansMenus() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_CheckLoanQualification(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            switch (theParam) {
                case "MENU": {
                    String strHeader = "Check Loan Limit";
                    theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE);
                    break;
                }
                case "TYPE": {

                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE.name());

                    if (strLoanType != "") {

                        HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                        String strLoanTypeID = hmLoanType.get("id");
                        String strLoanTypeCode = hmLoanType.get("code");
                        String strLoanTypeName = hmLoanType.get("name");
                        String strLoanTypeMaxAmount = hmLoanType.get("max");
                        String strLoanTypeMinAmount = hmLoanType.get("min");
                        String strLoanTypeMaxDuration = hmLoanType.get("duration");
                        String strLoanTypeInterest = hmLoanType.get("interest");

                        String strResponse = "Dear member, your " + strLoanTypeName + " qualification request has been received successfully. Please wait shortly as it's being processed.\n";

                        Thread worker = new Thread(() -> {
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkLoanQualification(theUSSDRequest);
                            System.out.println("checkLoanQualification: " + transactionReturnVal.getValue());
                        });
                        worker.start();

                        /*APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkLoanQualification(theUSSDRequest);

                        String strResponse ="";

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your Loan Qualification request has been received successfully. Please wait shortly as it's being processed.";
                        }else {


                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your Loan Qualification request CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your Loan Qualification request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your Loan Qualification request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }
                        */
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_QUALIFICATION_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strHeader = "Check Loan Limit\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE);
                    }

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_CheckLoanQualification() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Check Loan Limit\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_QUALIFICATION_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_CheckLoanQualification() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_CheckLoanGuarantorshipAbility(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            switch (theParam) {
                case "MENU": {
                    String strResponse = "Dear member, your loan guarantorship ability request has been received successfully. Please wait shortly as it's being processed.\n";

                    Thread worker = new Thread(() -> {
                        USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkLoanGuarantorshipAbility(theUSSDRequest);
                        System.out.println("checkLoanGuarantorshipAbility: " + transactionReturnVal.getValue());
                    });
                    worker.start();

                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORSHIP_ABILITY_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_CheckLoanGuarantorshipAbility() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Check Guarantorship Ability\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORSHIP_ABILITY_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_CheckLoanQualification() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoanApplication(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLoan = "";
            switch (theParam) {
                case "MENU": {
                    String strHeader = "Loan Application";
                    theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE);
                    break;
                }
                case "TYPE": {

                    String strLoanApplicationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());

                    if (strLoanApplicationType != "") {
                        String strLoanTypeDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                        HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanTypeDetails);
                        String strLoanTypeID = hmLoanType.get("id");
                        String strLoanTypeCode = hmLoanType.get("code");
                        String strLoanTypeName = hmLoanType.get("name");
                        String strLoanTypeMaxAmount = hmLoanType.get("max");
                        String strLoanTypeMinAmount = hmLoanType.get("min");
                        String strLoanTypeMaxDuration = hmLoanType.get("duration");
                        String strLoanTypeInterest = hmLoanType.get("interest");

                        double dblLoanTypeMinAmount = 0;
                        double dblLoanTypeMaxAmount = 0;
                        double dblLoanTypeMaxDuration = 0;
                        double dblLoanTypeInterest = 0;

                        try { dblLoanTypeMinAmount = Double.parseDouble(strLoanTypeMinAmount); }catch (Exception e){}
                        try { dblLoanTypeMaxAmount = Double.parseDouble(strLoanTypeMaxAmount); }catch (Exception e){}
                        try { dblLoanTypeMaxDuration = Double.parseDouble(strLoanTypeMaxDuration); }catch (Exception e){}
                        try { dblLoanTypeInterest = Double.parseDouble(strLoanTypeInterest); }catch (Exception e){}

                        String strLoanApplicationMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.APPLY_LOAN).getMinimum();
                        String strLoanApplicationMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.APPLY_LOAN).getMaximum();
                        double dblLoanApplicationMinimum = Double.parseDouble(strLoanApplicationMinimum);
                        double dblLoanApplicationMaximum = Double.parseDouble(strLoanApplicationMaximum);

                        dblLoanApplicationMinimum = Math.max(dblLoanApplicationMinimum, dblLoanTypeMinAmount);
                        dblLoanApplicationMaximum = Math.min(dblLoanApplicationMaximum, dblLoanTypeMaxAmount);

                        String strMenuInfo = "";
                        if(dblLoanApplicationMinimum > 0){ strMenuInfo = strMenuInfo + "Min: KES " + Utils.formatDouble(dblLoanApplicationMinimum, "#,###.##") + "\n";}
                        if(dblLoanApplicationMaximum > 0){ strMenuInfo = strMenuInfo + "Max: KES " + Utils.formatDouble(dblLoanApplicationMaximum, "#,###.##") + "\n";}
                        if(dblLoanTypeMaxDuration > 0){ strMenuInfo = strMenuInfo + "Duration: " + Utils.formatDouble(dblLoanTypeMaxDuration, "#,###.##") + " month(s)\n";}
                        if(dblLoanTypeInterest > 0){ strMenuInfo = strMenuInfo + "Interest : " + Utils.formatDouble(dblLoanTypeInterest, "#,###.##") + "%\n";}

                        String strResponse = strLoanTypeName + " Application\n" + strMenuInfo + "Enter amount:";

                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strHeader = "Loan Application\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE);
                    }

                    break;
                }
                case "AMOUNT": {
                    String strLoanTypeDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanTypeDetails);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    double dblLoanTypeMinAmount = 0;
                    double dblLoanTypeMaxAmount = 0;
                    double dblLoanTypeMaxDuration = 0;
                    double dblLoanTypeInterest = 0;

                    try { dblLoanTypeMinAmount = Double.parseDouble(strLoanTypeMinAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxAmount = Double.parseDouble(strLoanTypeMaxAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxDuration = Double.parseDouble(strLoanTypeMaxDuration); }catch (Exception e){}
                    try { dblLoanTypeInterest = Double.parseDouble(strLoanTypeInterest); }catch (Exception e){}

                    String strLoanApplicationMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.APPLY_LOAN).getMinimum();
                    String strLoanApplicationMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD, USSDAPIConstants.USSD_PARAM_TYPE.APPLY_LOAN).getMaximum();
                    double dblLoanApplicationMinimum = Double.parseDouble(strLoanApplicationMinimum);
                    double dblLoanApplicationMaximum = Double.parseDouble(strLoanApplicationMaximum);

                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());

                    dblLoanApplicationMinimum = Math.max(dblLoanApplicationMinimum, dblLoanTypeMinAmount);
                    dblLoanApplicationMaximum = Math.min(dblLoanApplicationMaximum, dblLoanTypeMaxAmount);

                    String strMenuInfo = "";
                    if(dblLoanApplicationMinimum > 0){ strMenuInfo = strMenuInfo + "Min: KES " + Utils.formatDouble(dblLoanApplicationMinimum, "#,###.##") + "\n";}
                    if(dblLoanApplicationMaximum > 0){ strMenuInfo = strMenuInfo + "Max: KES " + Utils.formatDouble(dblLoanApplicationMaximum, "#,###.##") + "\n";}
                    if(dblLoanTypeMaxDuration > 0){ strMenuInfo = strMenuInfo + "Duration: " + Utils.formatDouble(dblLoanTypeMaxDuration, "#,###.##") + " month(s)\n";}
                    if(dblLoanTypeInterest > 0){ strMenuInfo = strMenuInfo + "Interest : " + Utils.formatDouble(dblLoanTypeInterest, "#,###.##") + "%\n";}

                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strLoanTypeName + " Application\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        double dblAmountEntered = Double.parseDouble(strAmount);
                        if ( dblAmountEntered < dblLoanApplicationMinimum ) {
                            strResponse = strLoanTypeName + " Application\n" + strMenuInfo + "{MINIMUM amount is KES " + Utils.formatDouble(dblLoanApplicationMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }else if (dblAmountEntered > dblLoanApplicationMaximum) {
                            strResponse = strLoanTypeName + " Application\n" + strMenuInfo + "{MAXIMUM amount is KES " + Utils.formatDouble(dblLoanApplicationMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = strLoanTypeName + " Application\n" + strMenuInfo + "{Please enter a valid amount}:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoanTypeDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanTypeDetails);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    double dblLoanTypeMinAmount = 0;
                    double dblLoanTypeMaxAmount = 0;
                    double dblLoanTypeMaxDuration = 0;
                    double dblLoanTypeInterest = 0;

                    try { dblLoanTypeMinAmount = Double.parseDouble(strLoanTypeMinAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxAmount = Double.parseDouble(strLoanTypeMaxAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxDuration = Double.parseDouble(strLoanTypeMaxDuration); }catch (Exception e){}
                    try { dblLoanTypeInterest = Double.parseDouble(strLoanTypeInterest); }catch (Exception e){}

                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());

                        String strMenuInfo = "";
                        if(dblLoanTypeMaxDuration > 0){ strMenuInfo = strMenuInfo + "Duration: " + Utils.formatDouble(dblLoanTypeMaxDuration, "#,###.##") + " month(s)\n";}
                        if(dblLoanTypeInterest > 0){ strMenuInfo = strMenuInfo + "Interest : " + Utils.formatDouble(dblLoanTypeInterest, "#,###.##") + "%\n";}

                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strResponse = "Confirm " + strLoanTypeName + " Application\n" + strMenuInfo + "Amount Applied: KES " + strAmount + "\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                    } else {
                        String strResponse = strLoanTypeName + " Application\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "CONFIRMATION": {

                    String strLoanTypeDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanTypeDetails);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    double dblLoanTypeMinAmount = 0;
                    double dblLoanTypeMaxAmount = 0;
                    double dblLoanTypeMaxDuration = 0;
                    double dblLoanTypeInterest = 0;

                    try { dblLoanTypeMinAmount = Double.parseDouble(strLoanTypeMinAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxAmount = Double.parseDouble(strLoanTypeMaxAmount); }catch (Exception e){}
                    try { dblLoanTypeMaxDuration = Double.parseDouble(strLoanTypeMaxDuration); }catch (Exception e){}
                    try { dblLoanTypeInterest = Double.parseDouble(strLoanTypeInterest); }catch (Exception e){}

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION.name());
                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse = "Dear member, your " + strLoanTypeName + " Application request has been received successfully. Please wait shortly as it's being processed.\n";

                        Thread worker = new Thread(() -> {
                            USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);
                            System.out.println("loanApplication: " + transactionReturnVal.getValue());
                        });
                        worker.start();
                        /*
                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);

                        String strResponse ="";

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your "+strLoanName+" Application request has been received successfully. Please wait shortly as it's being processed.";
                        }else {


                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your "+strLoanName+" Application request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }
                        */
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);

                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your " + strLoanTypeName + " Application request NOT confirmed. Loan Application request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());

                        String strMenuInfo = "";
                        if(dblLoanTypeMaxDuration > 0){ strMenuInfo = strMenuInfo + "Duration: " + Utils.formatDouble(dblLoanTypeMaxDuration, "#,###.##") + " month(s)\n";}
                        if(dblLoanTypeInterest > 0){ strMenuInfo = strMenuInfo + "Interest : " + Utils.formatDouble(dblLoanTypeInterest, "#,###.##") + "%\n";}

                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strResponse = "Confirm " + strLoanTypeName + " Application\n{Select a valid menu}\n" + strMenuInfo + "Amount Applied: KES " + strAmount + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_LoanApplication() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loan Application\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanApplication() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoanRepayment(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);
        String strSender = pesaParam.getSenderIdentifier();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();
            if (theParam.equalsIgnoreCase("MENU")) {
                String strHeader = "Pay Loan";
                theUSSDResponse = getLoanRepaymentOption(theUSSDRequest, strHeader);

            } else { //LOAN_REPAYMENT_MENU
                String strLOAN_REPAYMENT_OPTION = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION.name());

                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType) {
                    case LOAN_REPAYMENT_OPTION: {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION.name());
                        if (!strLoanType.equals("")) {
                            String strHeader = "Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nSelect Loan";
                            theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN);

                        } else {
                            String strHeader = "Pay Loan\n{Select a valid menu}";
                            theUSSDResponse = getLoanRepaymentOption(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_LOAN: {
                        if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                            String strLoansInService = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                            if (!strLoansInService.equals("")) {
                                String strHeader = "Pay Loan\nSelect source of funds account\n";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT);
                            } else {
                                String strHeader = "Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\n{Select a VALID Loan}";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN);
                            }
                        } else {
                            String strLoansInService = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());

                            HashMap<String, String> hmLoan = Utils.toHashMap(strLoansInService);
                            String strLoanID = hmLoan.get("id");
                            String strLoanTypeName = hmLoan.get("type");
                            String strLoanAmount= hmLoan.get("amount");
                            String strLoanBalance = hmLoan.get("balance");
                            String strLoanAccountLabel= hmLoan.get("label");
                            String strLoanInstallmentAmount= hmLoan.get("installment");
                            String strLoanInterestAmount= hmLoan.get("interest");

                            String strFormattedLoanBalance = Utils.formatDouble(strLoanBalance,"#,###.##");
                            String strFormattedLoanInstallmentAmount= Utils.formatDouble(strLoanInstallmentAmount,"#,###.##");
                            String strFormattedLoanInterestAmount= Utils.formatDouble(strLoanInterestAmount,"#,###.##");

                            double totalPayable = Double.parseDouble(strLoanBalance) + Double.parseDouble(strLoanInterestAmount);
                            String strFormattedLoanTotalPayableAmount= Utils.formatDouble(String.valueOf(totalPayable),"#,###.##");

                            if (!strLoansInService.equals("")) {
                                String strResponse = "Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION;
                                strResponse = strResponse + "\nBalance KES "+strFormattedLoanBalance;
                                strResponse = strResponse + "\nInstalment KES "+strFormattedLoanInstallmentAmount;
                                strResponse = strResponse + "\nInterest KES "+strFormattedLoanInterestAmount;
                                strResponse = strResponse + "\nTotal KES "+strFormattedLoanTotalPayableAmount;
                                strResponse = strResponse + "\n\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            } else {
                                String strHeader = "Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\n{Select a valid option}";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN);
                            }
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_FUNDS_ACCOUNT: {
                        String strLoansInService = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        String strFromAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.name());

                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoansInService);
                        String strLoanID = hmLoan.get("id");
                        String strLoanTypeName = hmLoan.get("type");
                        String strLoanAmount= hmLoan.get("amount");
                        String strLoanBalance = hmLoan.get("balance");
                        String strLoanAccountLabel= hmLoan.get("label");
                        String strLoanInstallmentAmount= hmLoan.get("installment");

                        String strFormattedLoanBalance = Utils.formatDouble(strLoanBalance,"#,###.##");
                        String strFormattedLoanInstallmentAmount= Utils.formatDouble(strLoanInstallmentAmount,"#,###.##");

                        String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        if (!strFromAccountDetails.equals("")) {
                            String strResponse = "Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION;
                            strResponse = strResponse + "\nBalance KES "+strFormattedLoanBalance;
                            strResponse = strResponse + "\nInstalment KES "+strFormattedLoanInstallmentAmount;
                            strResponse = strResponse + "\n\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            String strHeader = "Pay Loan\n{Select a VALID source of funds account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_AMOUNT: {

                        String strLoanDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoanDetails);
                        String strLoanID = hmLoan.get("id");
                        String strLoanTypeName = hmLoan.get("type");
                        String strLoanAmount= hmLoan.get("amount");
                        String strLoanBalance = hmLoan.get("balance");
                        String strLoanAccountLabel= hmLoan.get("label");
                        String strLoanInstallmentAmount= hmLoan.get("installment");

                        String strFormattedLoanBalance = Utils.formatDouble(strLoanBalance,"#,###.##");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());

                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strFormattedAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.name());
                                HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                                String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                                String strFromAccountName =  hmFromAccountNoDetails.get("name");
                                String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                                String strFromAccountLabel = hmFromAccountNoDetails.get("label");
                                String strFromAccountAvailableBalance = hmFromAccountNoDetails.get("avail_bal");

                                strResponse = "Confirm Pay Loan via "+  strLOAN_REPAYMENT_OPTION + "\nFunds Account: " + strFromAccountLabel + "\n" + "Loan: " + strLoanAccountLabel + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                            } else {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no.: " + strSender + "\nLoan: " + strLoanAccountLabel + "\nAmount: KES " + strFormattedAmount + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                            double dblLoanBalance = 0;

                            try { dblLoanBalance = Double.parseDouble(strLoanBalance); }catch (Exception e){}

                            String strPayLoanMinimum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD,  USSDAPIConstants.USSD_PARAM_TYPE.PAY_LOAN).getMinimum();
                            String strPayLoanMaximum = theUSSDAPI.getAmountLimitCustomParameters(MBankingConstants.ApplicationType.USSD,  USSDAPIConstants.USSD_PARAM_TYPE.PAY_LOAN).getMaximum();

                            double dblPayLoanMinimum = Double.parseDouble(strPayLoanMinimum);
                            double dblPayLoanMaximum = Double.parseDouble(strPayLoanMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);


                            if (dblAmountEntered < dblPayLoanMinimum) {
                                strResponse = "Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strPayLoanMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else if (dblAmountEntered > dblPayLoanMaximum) {
                                strResponse = "Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strPayLoanMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            } else if (dblAmountEntered > dblLoanBalance) {
                                strResponse = "Pay " + strLoanAccountLabel + "\n{Amount KES " + Utils.formatDouble(strAmount, "#,###.##") + " EXCEEDS  loan balance KES " + Utils.formatDouble(strLoanBalance, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }else{
                                if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                    String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.name());
                                    HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                                    String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                                    String strFromAccountName =  hmFromAccountNoDetails.get("name");
                                    String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                                    String strFromAccountLabel = hmFromAccountNoDetails.get("label");
                                    String strFromAccountAvailableBalance = hmFromAccountNoDetails.get("avail_bal");

                                    double dblFromAccountAvailableBalance = 0;
                                    try { dblFromAccountAvailableBalance = Double.parseDouble(strFromAccountAvailableBalance); }catch (Exception e){}
                                    if(dblAmountEntered > dblFromAccountAvailableBalance){
                                        strResponse = "Pay " + strLoanAccountLabel  +  "\n{" +strFromAccountLabel+ " avail bal KES "  +Utils.formatDouble(dblFromAccountAvailableBalance,"#,###.##") + " is INSUFFICIENT to pay KES " + Utils.formatDouble(strAmount,"#,###.##")+"}\nEnter amount:";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                                    }
                                }
                            }

                        } else {
                            String strResponse = "Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION;
                            strResponse = strResponse + "\nBalance KES "+strFormattedLoanBalance;
                            strResponse = strResponse + "\nInstalment KES "+strFormattedLoanBalance;
                            strResponse = strResponse + "\n\n{Please enter a valid amount}:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_CONFIRMATION: {
                        String strLoanDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoanDetails);
                        String strLoanID = hmLoan.get("id");
                        String strLoanTypeName = hmLoan.get("type");
                        String strLoanAmount= hmLoan.get("amount");
                        String strLoanBalance = hmLoan.get("balance");
                        String strLoanAccountLabel= hmLoan.get("label");
                        String strLoanInstallmentAmount= hmLoan.get("installment");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());
                        String strFormattedAmount = Utils.formatDouble(strAmount, "#,###");

                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                strResponse = "Dear member, your request to Pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION + " has been received successfully.\n";

                                Thread worker = new Thread(() -> {
                                    USSDAPIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanRepayment(theUSSDRequest);
                                    System.out.println("loanRepayment: " + transactionReturnVal.getValue());
                                });
                                worker.start();

                            } else {
                                /*if (theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {
                                    strResponse = "You will be prompted by " + strLOAN_REPAYMENT_OPTION + " for payment\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                                    //String strOriginatorID = Long.toString(theUSSDRequest.getUSSDSessionID());
                                    String strOriginatorID = theUSSDRequest.getUSSDTraceID();
                                    String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                    String strReceiverDetails = strReceiver; //todo -> Get Receiver Name
                                    String strAccount = strLoanID;
                                    Double lnAmount = Utils.stringToDouble(strAmount);
                                    //String strReference = strReceiver;
                                    String strReference = strOriginatorID;

                                    //Generate temp account to send to M-PESA
                                    String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmssSSS"); //0001 //
                                    strTempAccount = APIUtils.convertToBase36(strTempAccount);
                                    XTremeDBCache.store(strTempAccount, strAccount);

                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(strOriginatorID, strReceiver, strReceiverDetails, strTempAccount, "KES", lnAmount, "LOAN_REPAYMENT", strReference, "USSD", "MBANKING");
                                } else {
                                    strResponse = "Use the details below to pay via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                                }*/


                                //Generate temp account to send to M-PESA
                                //String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmssSSS"); //0001 //
                                //strTempAccount = APIUtils.convertToBase36(strTempAccount);
                                //String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmss");
                                //XTremeDBCache.store(strTempAccount, strAccountNo);

                                /*if(theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()){

                                    strResponse = "You will be prompted by " + strLOAN_REPAYMENT_OPTION + " for payment\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                                    String strOriginatorID = theUSSDRequest.getUSSDTraceID();
                                    String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                    String strReceiverDetails = strReceiver;
                                    Double lnAmount = Utils.stringToDouble(strAmount);
                                    String strReference = strOriginatorID;

                                    String finalStrTempAccount = strTempAccount;
                                    Thread worker = new Thread(() -> {
                                        PESAAPI thePESAAPI = new PESAAPI();
                                        thePESAAPI.pesa_C2B_Request(strOriginatorID, strReceiver, strReceiverDetails, finalStrTempAccount, "KES", lnAmount, "LOAN_REPAYMENT", strReference, "USSD", "MBANKING");
                                    });
                                    worker.start();
                                }else{
                                    strResponse = "Use the details below to pay via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                                }*/

                                /*String strAccountNo = strLoanID;
                                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
                                String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                String strReceiverDetails = strReceiver;
                                double lnAmount = Utils.stringToDouble(strAmount);
                                //String strReference = strReceiver;

                                if(theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {

                                    strResponse = "You will be prompted by " + strLOAN_REPAYMENT_OPTION + " for payment\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";

                                    //Generate temp account to send to M-PESA
                                    String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmssSSS");
                                    strTempAccount = APIUtils.convertToBase36(strTempAccount);
                                    XTremeDBCache.store(strTempAccount, strAccountNo);

                                    String finalStrTempAccount = strTempAccount;
                                    Thread worker = new Thread(() -> {
                                        PESAAPI thePESAAPI = new PESAAPI();
                                        thePESAAPI.pesa_C2B_Request(
                                                strTransactionID,
                                                strReceiver,
                                                strReceiverDetails,
                                                finalStrTempAccount,
                                                "KES",
                                                lnAmount,
                                                "LOAN_REPAYMENT",
                                                strTransactionID,
                                                "USSD",
                                                "MBANKING"
                                        );
                                    });
                                    worker.start();
                                } else {
                                    strResponse = "Use the details below to pay via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanID + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                                }*/

                                //Generate temp account to send to M-PESA
                                String strAccountNo = strLoanID;
                                String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmssSSS");
                                strTempAccount = APIUtils.convertToBase36(strTempAccount);
                                XTremeDBCache.store(strTempAccount, strAccountNo);

                                if(theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()){

                                    strResponse = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + "\n" + "A/C: " + strAccountNo + "\n" + "Amount: KES " + strAmount + "\n";

                                    String strOriginatorID = theUSSDRequest.getUSSDTraceID();
                                    String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                    String strReceiverDetails = strReceiver;
                                    Double lnAmount = Utils.stringToDouble(strAmount);
                                    String strReference = strOriginatorID;

                                    String finalStrTempAccount = strTempAccount;
                                    Thread worker = new Thread(() -> {
                                        PESAAPI thePESAAPI = new PESAAPI();
                                        thePESAAPI.pesa_C2B_Request(strOriginatorID, theUSSDRequest.getUSSDTraceID(), strReceiver, strReceiverDetails, finalStrTempAccount, "KES", lnAmount, "LOAN_REPAYMENT", strReference, "USSD", "MBANKING");
                                    });
                                    worker.start();
                                }else{
                                    strResponse = "Use the details below to pay via M-PESA\nPaybill no: " + strSender + "\n" + "A/C: " + strAccountNo + "\n" + "Amount: KES " + strAmount + "\n";
                                }

                            }

                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                            /*ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);*/
                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your request to pay " + strLoanAccountLabel + " via " + strLOAN_REPAYMENT_OPTION + " was NOT confirmed. Pay Loan request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                String strFromAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.name());
                                HashMap <String, String> hmFromAccountNoDetails  = Utils.toHashMap(strFromAccountNoDetails);
                                String strFromAccountNumber = hmFromAccountNoDetails.get("number");
                                String strFromAccountName =  hmFromAccountNoDetails.get("name");
                                String strFromAccountTypeName = hmFromAccountNoDetails.get("type_name");
                                String strFromAccountLabel = hmFromAccountNoDetails.get("label");

                                strResponse = "Confirm Pay Loan via "+  strLOAN_REPAYMENT_OPTION + "\n{Select a valid menu}\nFunds Account: " + strFromAccountLabel + "\n" + "Loan: " + strLoanAccountLabel + "\n" + "Amount: KES " + strFormattedAmount + "\n";
                            } else {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\n{Select a valid menu}\nPaybill no.: " + strSender + "\nLoan: " + strLoanAccountLabel + "\nAmount: KES " + strFormattedAmount + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        }

                        break;
                    }
                    default: {
                        System.err.println("theAppMenus.displayMenu_LoanRepayment() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Pay Loan\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanRepayment() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getLoanRepaymentOption(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "M-PESA", "1: M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "Savings Account", "2: Savings Account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION, "NO", theArrayListUSSDSelectOption);
            return theUSSDResponse;
        } catch (Exception e) {
            System.err.println("theAppMenus.getLoanRepaymentOption() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse displayMenu_LoanGuarantors(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLoan = "";
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            switch (theParam) {
                case "MENU": {
                    String strHeader = "Loan Guarantors";
                    theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE);
                    break;
                }
                case "TYPE": {

                    String strGuarantorsType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());

                    if (!strGuarantorsType.equals("")) {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                        HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                        String strLoanTypeID = hmLoanType.get("id");
                        String strLoanTypeCode = hmLoanType.get("code");
                        String strLoanTypeName = hmLoanType.get("name");
                        String strLoanTypeMaxAmount = hmLoanType.get("max");
                        String strLoanTypeMinAmount = hmLoanType.get("min");
                        String strLoanTypeMaxDuration = hmLoanType.get("duration");
                        String strLoanTypeInterest = hmLoanType.get("interest");

                        //TODO: check if Loan requires guarantors
                        //if yes
                        String strResponse = strLoanTypeName + " Guarantors\n";
                        theUSSDResponse = getGuarantorsMenus(theUSSDRequest, strResponse);
                        //if no

                    } else {
                        String strHeader = "Loan Guarantors\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE);
                    }
                    break;
                }
                case "OPTION": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strResponse = strLoanTypeName + " Guarantors\n";
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());

                    switch (strAction) {
                        case "VIEW_GUARANTORS": {
                            //TODO: fetch all guarantors of the loan
                            theUSSDResponse = getAllLoanGuarantors(theUSSDRequest, strResponse);
                            break;
                        }
                        case "ADD_GUARANTOR": {
                            //TODO: check if all required number of guarantors have been added
                            strResponse = strLoanTypeName + " Add Guarantor\n" + "Enter Guarantor Mobile No.";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        default: {
                            strResponse = strLoanTypeName + " Guarantors\n{Select a valid menu}";
                            theUSSDResponse = getGuarantorsMenus(theUSSDRequest, strResponse);
                            break;
                        }
                    }
                    break;
                }
                case "GUARANTORS": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strGuarantor = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTORS.name());
                    if (strGuarantor != null && !strGuarantor.isEmpty()) {
                        HashMap<String, String> hmGuarantor = Utils.toHashMap(strGuarantor);
                        String strName = hmGuarantor.get("NAME");
                        String strID = hmGuarantor.get("ID");
                        String strMobileNo = hmGuarantor.get("MOBILE_NUMBER");

                        String strResponse = strLoanTypeName + " Guarantors\n";

                        strResponse = strResponse + "Guarantor Details:\n";
                        strResponse = strResponse + "\nName: " + strName;
                        strResponse = strResponse + "\nID: " + strID;
                        strResponse = strResponse + "\nMobile: " + strMobileNo;

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);

                    } else {
                        String strResponse = strLoanTypeName + " Guarantors\n{Select a valid menu}";
                        theUSSDResponse = getAllLoanGuarantors(theUSSDRequest, strResponse);
                    }
                    break;
                }
                case "MOBILE_NUMBER": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strResponse = strLoanTypeName + " Add Guarantor\n";

                    String strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.name());
                    strMobileNo = APIUtils.sanitizePhoneNumber(strMobileNo);

                    if (!strMobileNo.equalsIgnoreCase("INVALID MOBILE NUMBER")/* || !strOtherMobileNo.matches("^254((7[0-2][0-9])|(74[0-3])|(74[5-6])|(748)|(75[7-9])|(76[8-9])|(79[0-9]))[0-9]{6}$")*/) {
                        //TODO: check if user with specified mobile number exists. and check if user is already a guarantor for the loan
                        //if okay
                        strResponse = strResponse + "\nEnter PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        strResponse = strResponse + "{Enter a valid mobile number}\nEnter Guarantor Mobile No.\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        //TODO: fetch guarantor details with mobile number
                        String strResponse = "Confirm Guarantor Details for " + strLoanTypeName + ":\n";
                        theUSSDResponse = addGuarantorConfirmation(theUSSDRequest, strResponse);
                    } else {
                        String strResponse = strLoanTypeName + " Add Guarantor\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_CONFIRMATION.name());
                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse = "Dear member, your " + strLoanTypeName + " Add Guarantor request has been received successfully. Please wait shortly as it's being processed.";

                        Thread worker = new Thread(() -> {
                            //TODO: call function for adding guarantor
                            /*APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);
                            System.out.println("loanApplication: " + transactionReturnVal.getValue());*/
                        });
                        worker.start();
                        /*
                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);

                        String strResponse ="";

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your "+strLoanName+" Application request has been received successfully. Please wait shortly as it's being processed.";
                        }else {


                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your "+strLoanName+" Application request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }
                        */
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);

                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your " + strLoanTypeName + " Add Guarantor request NOT confirmed. Add Guarantor request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = "Confirm Guarantor Details for " + strLoanTypeName + ":\n{Select a valid menu}\n";
                        theUSSDResponse = addGuarantorConfirmation(theUSSDRequest, strResponse);
                    }
                    break;
                }
                case "END": {
                    String strResponse = "Loan Guarantors\n{Select a valid menu}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_LoanGuarantors() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loan Guarantors\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanGuarantors() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getGuarantorsMenus(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "VIEW_GUARANTORS", "1: View Guarantors");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ADD_GUARANTOR", "2: Add Guarantor");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.getGuarantorsMenus() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getAllLoanGuarantors(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            //TODO: fetch all guarantors of the loan
            //SAMPLE TEST
            List<HashMap<String, String>> lstGuarantors = new ArrayList<HashMap<String, String>>() {{
                add(new HashMap<String, String>() {{
                    put("NAME", "James Munene");
                    put("ID", "123456");
                    put("MOBILE_NUMBER", "254712082273");
                }});

                add(new HashMap<String, String>() {{
                    put("NAME", "Mary Ann");
                    put("ID", "48189294");
                    put("MOBILE_NUMBER", "2547009988556");
                }});

                add(new HashMap<String, String>() {{
                    put("NAME", "Alex Kibet");
                    put("ID", "983626633");
                    put("MOBILE_NUMBER", "2547018873476");
                }});

            }};
            int i = 0;
            for (HashMap<String, String> hmGuarantor : lstGuarantors) {
                i++;
                String strOptionValue = Utils.serialize(hmGuarantor);
                String strName = hmGuarantor.get("NAME");
                String strOptionMenu = Integer.toString(i);
                String strOptionDisplayText = strOptionMenu + ": " + strName;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTORS, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("LoansMenus.getAllLoanGuarantors() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse addGuarantorConfirmation(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            //TODO: fetch guarantor details with mobile number
            //if found
            //SAMPLE TEST
            HashMap<String, String> hmGuarantor = new HashMap<String, String>() {{
                put("NAME", "James Munene");
                put("ID", "123456");
                put("MOBILE_NUMBER", "254712082273");
            }};

            String strName = hmGuarantor.get("NAME");
            String strID = hmGuarantor.get("ID");
            String stMobileNumber = hmGuarantor.get("MOBILE_NUMBER");

            theHeader = theHeader + "\nName: " + strName;
            theHeader = theHeader + "\nID: " + strID;
            theHeader = theHeader + "\nMobile: " + stMobileNumber;

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("LoansMenus.addGuarantorConfirmation() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoansGuaranteed(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLoan = "";
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            switch (theParam) {
                case "MENU": {
                    String strHeader = "Loans Guaranteed\n";
                    theUSSDResponse = getLoansGuaranteedOptions(theUSSDRequest, strHeader);
                    break;
                }
                case "OPTION": {
                    String strOptions = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_OPTION.name());
                    if (!strOptions.equals("")) {
                        String strHeader = "Loans Guaranteed - " + strOptions;
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE);
                    } else {
                        String strHeader = "Loans Guaranteed\n{Select a valid menu}";
                        theUSSDResponse = getLoansGuaranteedOptions(theUSSDRequest, strHeader);
                    }
                    break;
                }
                case "TYPE": {
                    String strOptions = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_OPTION.name());
                    String strGuarantorsType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE.name());

                    if (!strGuarantorsType.equals("")) {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE.name());
                        HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                        String strLoanTypeID = hmLoanType.get("id");
                        String strLoanTypeCode = hmLoanType.get("code");
                        String strLoanTypeName = hmLoanType.get("name");
                        String strLoanTypeMaxAmount = hmLoanType.get("max");
                        String strLoanTypeMinAmount = hmLoanType.get("min");
                        String strLoanTypeMaxDuration = hmLoanType.get("duration");
                        String strLoanTypeInterest = hmLoanType.get("interest");

                        if (strOptions.equals("PENDING")) {
                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n";
                            theUSSDResponse = getLoansGuaranteed_LoanDetails(theUSSDRequest, strResponse, "PENDING");
                        } else {
                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n";
                            theUSSDResponse = getLoansGuaranteed_LoanDetails(theUSSDRequest, strResponse, "APPROVED");
                        }
                    } else {
                        String strHeader = "Loans Guaranteed - " + strOptions + "\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, theParam, strHeader, USSDAPIConstants.AccountType.ALL, AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE);
                    }
                    break;
                }
                case "LOAN_DETAILS": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strOptions = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_OPTION.name());
                    String strLoaner = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_LOAN_DETAILS.name());

                    if (strLoaner != null && !strLoaner.isEmpty()) {
                        if (strOptions.equals("PENDING")) {
                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n";
                            strResponse = strResponse + "Enter PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOANS_GUARANTEED_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            HashMap<String, String> hmGuarantor = Utils.toHashMap(strLoaner);
                            String strLoanSerial = hmGuarantor.get("LOAN_SERIAL");
                            String strName = hmGuarantor.get("NAME");
                            String stPhoneNumber = hmGuarantor.get("MOBILE_NUMBER");
                            String strAmount = hmGuarantor.get("AMOUNT");
                            String strDate = hmGuarantor.get("DATE");

                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n";

                            strResponse = strResponse + "\nSerial: " + strLoanSerial;
                            strResponse = strResponse + "\nName: " + strName;
                            strResponse = strResponse + "\nMobile: " + stPhoneNumber;
                            strResponse = strResponse + "\nAmount: " + strAmount;
                            strResponse = strResponse + "\nDate: " + strDate;


                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);
                        }
                    } else {
                        if (strOptions.equals("PENDING")) {
                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n{Select a valid menu}";
                            theUSSDResponse = getLoansGuaranteed_LoanDetails(theUSSDRequest, strResponse, "PENDING");
                        } else {
                            String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n{Select a valid menu}";
                            theUSSDResponse = getLoansGuaranteed_LoanDetails(theUSSDRequest, strResponse, "APPROVED");
                        }
                    }
                    break;
                }
                case "PIN": {
                    String strOptions = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_OPTION.name());

                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        String strResponse = "Confirm Details for " + strLoanTypeName + ":\n";
                        theUSSDResponse = confirmLoanGuarantee(theUSSDRequest, strResponse);
                    } else {
                        String strResponse = strLoanTypeName + " Guaranteed - " + strOptions + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOANS_GUARANTEED_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanTypeID = hmLoanType.get("id");
                    String strLoanTypeCode = hmLoanType.get("code");
                    String strLoanTypeName = hmLoanType.get("name");
                    String strLoanTypeMaxAmount = hmLoanType.get("max");
                    String strLoanTypeMinAmount = hmLoanType.get("min");
                    String strLoanTypeMaxDuration = hmLoanType.get("duration");
                    String strLoanTypeInterest = hmLoanType.get("interest");

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_CONFIRMATION.name());
                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse = "Dear member, your Loan Guarantee request has been received successfully. Please wait shortly as it's being processed.";

                        Thread worker = new Thread(() -> {
                            //TODO: call function for adding guarantor
                            /*APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);
                            System.out.println("loanApplication: " + transactionReturnVal.getValue());*/
                        });
                        worker.start();
                        /*
                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest);

                        String strResponse ="";

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your "+strLoanName+" Application request has been received successfully. Please wait shortly as it's being processed.";
                        }else {


                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your "+strLoanName+" Application request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your "+strLoanName+" Application request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }
                        */
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);

                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your Loan Guarantee request NOT confirmed. Loan Guarantee request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = "Confirm Details for " + strLoanTypeName + ":\n{Select a valid menu}\n";
                        theUSSDResponse = confirmLoanGuarantee(theUSSDRequest, strResponse);
                    }
                    break;
                }
                case "END": {
                    String strResponse = "Loans Guaranteed\n{Select a valid menu}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_LoansGuaranteed() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loans Guaranteed\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanGuarantors() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getLoansGuaranteedOptions(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "PENDING", "1: Pending");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "APPROVED", "2: Approved");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_OPTION, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.getLoansGuaaranteedOptions() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getLoansGuaranteed_LoanDetails(USSDRequest theUSSDRequest, String theHeader, String strLoanStatus) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            //TODO: fetch all loans pending approval with strStatus
            //SAMPLE TEST
            List<HashMap<String, String>> lstLoaners = new ArrayList<HashMap<String, String>>() {{
                add(new HashMap<String, String>() {{
                    put("LOAN_SERIAL", "LN9283838");
                    put("NAME", "James Munene");
                    put("ID", "12345688");
                    put("MOBILE_NUMBER", "254712082273");
                    put("AMOUNT", "23456");
                    put("DATE", "2020-06-25 12:00:00");
                }});

                add(new HashMap<String, String>() {{
                    put("LOAN_SERIAL", "LN0002883");
                    put("NAME", "Mary Ann");
                    put("ID", "48189294");
                    put("MOBILE_NUMBER", "2547009988556");
                    put("AMOUNT", "876455");
                    put("DATE", "2020-06-25 12:00:00");
                }});

                add(new HashMap<String, String>() {{
                    put("LOAN_SERIAL", "LN1255454");
                    put("NAME", "Alex Kibet");
                    put("ID", "983626633");
                    put("MOBILE_NUMBER", "2547018873476");
                    put("AMOUNT", "1299480");
                    put("DATE", "2020-06-25 12:00:00");
                }});
            }};
            int i = 0;
            for (HashMap<String, String> hmGuarantor : lstLoaners) {
                i++;
                String strOptionValue = Utils.serialize(hmGuarantor);
                String strLoanSerial = hmGuarantor.get("LOAN_SERIAL");
                String strName = hmGuarantor.get("NAME");
                String strOptionMenu = Integer.toString(i);
                String strOptionDisplayText = strOptionMenu + ": " + strLoanSerial + " - " + strName;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_LOAN_DETAILS, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("LoansMenus.getAllLoaners_PENDING() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse confirmLoanGuarantee(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            //SAMPLE TEST
            String strLoaner = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_LOAN_DETAILS.name());

            HashMap<String, String> hmGuarantor = Utils.toHashMap(strLoaner);

            String strLoanSerial = hmGuarantor.get("LOAN_SERIAL");
            String strName = hmGuarantor.get("NAME");
            String strMobileNumber = hmGuarantor.get("MOBILE_NUMBER");
            String strAmount = hmGuarantor.get("AMOUNT");
            String strDate = hmGuarantor.get("DATE");

            theHeader = theHeader + "\nSerial: " + strLoanSerial;
            theHeader = theHeader + "\nName: " + strName;
            theHeader = theHeader + "\nMobile: " + strMobileNumber;
            theHeader = theHeader + "\nAmount: " + strAmount;
            theHeader = theHeader + "\nDate: " + strDate;

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("LoansMenus.confirmLoanGuarantee() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}

