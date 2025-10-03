package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;

public class AppActions {
	
	public AppActions(Long theUSSDMobileNo){
	}

	public static USSDResponse action_INIT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Init(theUSSDRequest, theParam);
	}

	public static USSDResponse action_GENERAL(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_General(theUSSDRequest, theParam);
	}

	public static USSDResponse action_BUY_GOODS(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_BuyGoodsMenus(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOGIN(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Login(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MAIN_IN(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_MainIn(theUSSDRequest, theParam);
	}

	public static USSDResponse action_SET_PIN(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_SetPIN(theUSSDRequest, theParam);
	}

	public static USSDResponse action_CHANGE_PIN(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_ChangePIN(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MY_ACCOUNT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_MyAccount(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MY_ACCOUNT_BALANCE(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MY_ACCOUNT_MINI_STATEMENT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_MiniStatement(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MAPP_ACTIVATION(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_MobileApp(theUSSDRequest, theParam);
	}

	public static USSDResponse action_ACCOUNT_REGISTRATION(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_AccountRegistration(theUSSDRequest, theParam);
	}

	public static USSDResponse action_SELF_REGISTRATION(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_SelfRegistration(theUSSDRequest, theParam);
	}

	public static USSDResponse action_WITHDRAWAL(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Withdrawal(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOAN(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Loan(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOAN_APPLICATION(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_LoanApplication(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOAN_REPAYMENT(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_LoanRepayment(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOAN_GUARANTORS(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_LoanGuarantors(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOANS_GUARANTEED(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_LoansGuaranteed(theUSSDRequest, theParam);
	}

	public static USSDResponse action_LOAN_QUALIFICATION(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_CheckLoanQualification(theUSSDRequest, theParam);
	}

	public static USSDResponse action_GUARANTORSHIP_ABILITY(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_CheckLoanGuarantorshipAbility(theUSSDRequest, theParam);
	}

	public static USSDResponse action_FUNDS_TRANSFER(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_FundTransfer(theUSSDRequest, theParam);
	}

	public static USSDResponse action_FUNDS_TRANSFER_INTERNAL(USSDRequest theUSSDRequest, String theParam) {
		return new AppMenus().displayMenu_FundTransferInternal(theUSSDRequest, theParam);
	}

	public static USSDResponse action_FUNDS_TRANSFER_EXTERNAL(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_FundTransferExternal(theUSSDRequest, theParam);
	}


	public static USSDResponse action_DEPOSIT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Deposit(theUSSDRequest, theParam);
	}

	public static USSDResponse action_UTILITIES(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Utilities(theUSSDRequest, theParam);
	}

	public static USSDResponse action_MPESA_FLOAT_PURCHASE(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_MPESA_Float_Purchase(theUSSDRequest, theParam);
	}

	public static USSDResponse action_ETOPUP(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Etopup(theUSSDRequest, theParam);
	}

	public static USSDResponse action_PAY_BILL(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_PayBill(theUSSDRequest, theParam);
	}

	public static USSDResponse action_PAY_BILL_MAINTENANCE_ACCOUNT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_Paybill_Maintain_Accounts(theUSSDRequest, theParam);
	}
	public static USSDResponse action_FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(USSDRequest theUSSDRequest, String theParam){
		return new AppMenus().displayMenu_FundTransferExternal_Maintain_Accounts(theUSSDRequest, theParam);
	}
} // End Class MainMenu
