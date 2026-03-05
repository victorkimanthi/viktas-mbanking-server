package ke.skyworld.mbanking.ussdapplication;

public class AppConstants {

    public final static String strHomeMenuHeader = "Welcome to VIKASH";
    public final static String strMobileBankingName = "VIKASH";
    public final static String strSACCOName = "VIKTAS SACCO";
    public final static String strBusinessShortCodePaymentName = "Lipa Na VIKASH";
    public final static String strContactUs = "VIKTAS SACCO";
    public final static String strAppID = "BjcPLrMschD";
    public final static String strMBankingUSSDCode = "882";
    public final static String strMBankingUSSDSubCode = "0";
    public final static String strGeneralUSSDCode = "882";
    public final static String strGeneralUSSDSubCode = "586";

    /*public final static String strHomeMenuHeader = "Welcome M-Siraji";
    public final static String strMobileBankingName = "M-Siraji";
    public final static String strSACCOName = "Siraji SACCO";
    public final static String strContactUs = "Siraji SACCO";
    public final static String strAppID = "BjcPLrMschD";*/

    /*public final static String strHomeMenuHeader = "Welcome to SULUHU SACCO Mobile";
    public final static String strMobileBankingName = "SULUHU SACCO Mobile";
    public final static String strSACCOName = "SULUHU SACCO";
    public final static String strContactUs = "SULUHU SACCO";
    public final static String strAppID = "BjcPLrMschD";*/

    /*public final static String strHomeMenuHeader = "Welcome M-Boresha";
    public final static String strMobileBankingName = "M-Boresha";
    public final static String strSACCOName = "Boresha SACCO";
    public final static String strContactUs = "Boresha SACCO";
    public final static String strAppID = "BjcPLrMschD";*/

    //DEFAULT
    public final static String contact = "";
    public final static String contactUs = " Please contact us for assistance"+contact+".";
    public final static String visitOurBranchesAndContactUs = " Please visit one of our branches for assistance or contact us"+contact+".";

    //SULUHU
    /*public final static String contact = " on 0794056489";
    public final static String contactUs = " Please contact us for assistance"+contact;
    public final static String visitOurBranchesAndContactUs = " Please visit one of our branches for assistance or contact us"+contact;*/

    public enum SPProviderCode {
        SAFARICOM(101),
        AIRTEL(102),
        Cooperative_Bank(201),
        KCB(202),
        Equity_Bank(203),
        Standard_Chartered_Bank(204),
        Barclays_Bank(205);
        //Postbank(206);
        private final int intValue;

        SPProviderCode(int theValue) {
            this.intValue = theValue;
        }

        public int getValue() {
            return intValue;
        }
    }

    public enum SPProviderAccountCode {
        //SAFARICOM
        SAFARICOM_B2C("10101"),
        SAFARICOM_C2B("10102"),
        SAFARICOM_B2B("10103"),
        KPLC_TOKEN("10104"),
        KPLC_Postpaid("10105"),
        Nairobi_Water("10106"),
        DStv("10107"),
        ZUKU("10108"),
        GOtv("10109"),
        StarTimes("10110");

        private final String strValue;

        SPProviderAccountCode(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum USSDDiplayText {
        EXIT("Thank you for using our services.");

        private final String strValue;

        USSDDiplayText(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum USSDProvider {
        SAFARICOM(101),
        AIRTEL(102),
        TELKOM(103),
        YU(104),
        EQUITEL(105),
        INTERNATIONAL_GATEWAY(201);

        private final int intValue;

        USSDProvider(int theValue) {
            this.intValue = theValue;
        }

        public int getValue() {
            return intValue;
        }
    }

    public enum USSDDataType {
        INIT("INIT(INIT)"),
        // -- For Future reference if OUT_MAIN_MENU Menus is required
        //OUT_MAIN_MENU
        MAIN_OUT_MENU(USSDDataType.INIT.getValue() + "-MAIN_OUT(MENU)"),
        //LOGIN_PIN(USSDDataType.MAIN_OUT_MENU.getValue() + "-LOGIN(PIN)"), //MAIN_OUT_MENU NOT USED therefore Menu Starts at Login

        LOGIN_PIN(USSDDataType.INIT.getValue() + "-LOGIN(PIN)"),

        //MAIN_IN_MENU
        MAIN_IN_MENU(USSDDataType.LOGIN_PIN.getValue() + "-MAIN_IN(MENU)"),
					//WITHDRAWAL
					WITHDRAWAL_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-WITHDRAWAL(MENU)"),
						WITHDRAWAL_OPTION(USSDDataType.WITHDRAWAL_MENU.getValue() + "-WITHDRAWAL(OPTION)"),
							WITHDRAWAL_TO_OPTION(USSDDataType.WITHDRAWAL_MENU.getValue() + "-WITHDRAWAL(TO_OPTION)"),
								WITHDRAWAL_TO(USSDDataType.WITHDRAWAL_OPTION.getValue() + "-WITHDRAWAL(TO)"),
									WITHDRAWAL_ACCOUNT(USSDDataType.WITHDRAWAL_TO.getValue() + "-WITHDRAWAL(ACCOUNT)"),
										WITHDRAWAL_AMOUNT(USSDDataType.WITHDRAWAL_ACCOUNT.getValue() + "-WITHDRAWAL(AMOUNT)"),
											WITHDRAWAL_PIN(USSDDataType.WITHDRAWAL_AMOUNT.getValue() + "-WITHDRAWAL(PIN)"),
												WITHDRAWAL_CONFIRMATION(USSDDataType.WITHDRAWAL_PIN.getValue() + "-WITHDRAWAL(CONFIRMATION)"),
													WITHDRAWAL_END(USSDDataType.WITHDRAWAL_CONFIRMATION.getValue() + "-WITHDRAWAL(END)"),

					//UTILITIES
					UTILITIES_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-UTILITIES(MENU)"),

						//ETOPUP(Airtime etc.)
						ETOPUP_ACCOUNT(USSDDataType.UTILITIES_MENU.getValue() + "-ETOPUP(ACCOUNT)"),
							ETOPUP_AMOUNT(USSDDataType.ETOPUP_ACCOUNT.getValue() + "-ETOPUP(AMOUNT)"),
								ETOPUP_PIN(USSDDataType.ETOPUP_AMOUNT.getValue() + "-ETOPUP(PIN)"),
									ETOPUP_CONFIRMATION(USSDDataType.ETOPUP_PIN.getValue() + "-ETOPUP(CONFIRMATION)"),
										ETOPUP_END(USSDDataType.ETOPUP_CONFIRMATION.getValue() + "-ETOPUP(END)"),

						//BILL (KPLC, NAIROBI WATER, DSTV etc.)
						PAY_BILL_BILLER_ACCOUNT(USSDDataType.UTILITIES_MENU.getValue() + "-PAY_BILL(BILLER_ACCOUNT)"),
							PAY_BILL_FROM_ACCOUNT(USSDDataType.PAY_BILL_BILLER_ACCOUNT.getValue() + "-PAY_BILL(FROM_ACCOUNT)"),
								PAY_BILL_AMOUNT(USSDDataType.PAY_BILL_FROM_ACCOUNT.getValue() + "-PAY_BILL(AMOUNT)"),
									PAY_BILL_PIN(USSDDataType.PAY_BILL_AMOUNT.getValue() + "-PAY_BILL(PIN)"),
										PAY_BILL_CONFIRMATION(USSDDataType.PAY_BILL_PIN.getValue() + "-PAY_BILL(CONFIRMATION)"),
											PAY_BILL_END(USSDDataType.PAY_BILL_CONFIRMATION.getValue() + "-PAY_BILL(END)"),

						//PAY_BILL_MAINTENANCE_ACCOUNT
						PAY_BILL_MAINTENANCE_ACCOUNT_MENU(USSDDataType.UTILITIES_MENU.getValue() + "-PAY_BILL_MAINTENANCE_ACCOUNT(MENU)"),
							PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT(USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_MENU.getValue() + "-PAY_BILL_MAINTENANCE_ACCOUNT(ACCOUNT)"),
								PAY_BILL_MAINTENANCE_ACCOUNT_NAME(USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT.getValue() + "-PAY_BILL_MAINTENANCE_ACCOUNT(NAME)"),
							PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE(USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_MENU.getValue() + "-PAY_BILL_MAINTENANCE_ACCOUNT(REMOVE)"),

			        //FIXME: TEMPORARY
                    MPESA_FLOAT_PURCHASE_ACCOUNT(USSDDataType.UTILITIES_MENU.getValue() + "-MPESA_FLOAT_PURCHASE(ACCOUNT)"),
                        MPESA_FLOAT_PURCHASE_AGENT_NO(USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.getValue() + "-MPESA_FLOAT_PURCHASE(AGENT_NO)"),
                            MPESA_FLOAT_PURCHASE_AGENT_NAME(USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO.getValue() + "-MPESA_FLOAT_PURCHASE(AGENT_NAME)"),
                                MPESA_FLOAT_PURCHASE_STORE_NO(USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.getValue() + "-MPESA_FLOAT_PURCHASE(STORE_NO)"),
                                    MPESA_FLOAT_PURCHASE_AMOUNT(USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO.getValue() + "-MPESA_FLOAT_PURCHASE(AMOUNT)"),
                                        MPESA_FLOAT_PURCHASE_PIN(USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT.getValue() + "-MPESA_FLOAT_PURCHASE(PIN)"),
                                            MPESA_FLOAT_PURCHASE_CONFIRMATION(USSDDataType.MPESA_FLOAT_PURCHASE_PIN.getValue() + "-MPESA_FLOAT_PURCHASE(CONFIRMATION)"),
                                                MPESA_FLOAT_PURCHASE_END(USSDDataType.MPESA_FLOAT_PURCHASE_CONFIRMATION.getValue() + "-MPESA_FLOAT_PURCHASE(END)"),
                    //FIXME: END OF TEMPORARY

                    //DEPOSIT
					DEPOSIT_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-DEPOSIT(MENU)"),
						DEPOSIT_ACCOUNT(USSDDataType.DEPOSIT_MENU.getValue() + "-DEPOSIT(ACCOUNT)"),
							DEPOSIT_SHARE(USSDDataType.DEPOSIT_MENU.getValue() + "-DEPOSIT(SHARE)"),
								DEPOSIT_AMOUNT(USSDDataType.DEPOSIT_ACCOUNT.getValue() + "-DEPOSIT(AMOUNT)"),
									DEPOSIT_PIN(USSDDataType.DEPOSIT_AMOUNT.getValue() + "-DEPOSIT(PIN)"),
										DEPOSIT_CONFIRMATION(USSDDataType.DEPOSIT_PIN.getValue() + "-DEPOSIT(CONFIRMATION)"),
											DEPOSIT_END(USSDDataType.DEPOSIT_CONFIRMATION.getValue() + "-DEPOSIT(END)"),

					/*
					//DEPOSIT
					DEPOSIT_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-DEPOSIT(MENU)"),
						DEPOSIT_ACCOUNT(USSDDataType.DEPOSIT_MENU.getValue() + "-DEPOSIT(ACCOUNT)"),
							DEPOSIT_AMOUNT(USSDDataType.DEPOSIT_ACCOUNT.getValue() + "-DEPOSIT(AMOUNT)"),
								DEPOSIT_PIN(USSDDataType.DEPOSIT_AMOUNT.getValue() + "-DEPOSIT(PIN)"),
									DEPOSIT_CONFIRMATION(USSDDataType.DEPOSIT_PIN.getValue() + "-DEPOSIT(CONFIRMATION)"),
										DEPOSIT_END(USSDDataType.DEPOSIT_CONFIRMATION.getValue() + "-DEPOSIT(END)"),
					 */
					//MY ACCOUNT
					MY_ACCOUNT_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-MY_ACCOUNT(MENU)"),
						//GENERAL
						/*
						MY_ACCOUNT_ACCOUNT(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-MY_ACCOUNT(ACCOUNT)"),
							MY_ACCOUNT_PIN(USSDDataType.MY_ACCOUNT_ACCOUNT.getValue() + "-MY_ACCOUNT(PIN)"),
								MY_ACCOUNT_END(USSDDataType.MY_ACCOUNT_PIN.getValue() + "-MY_ACCOUNT(END)"),
						*/

						//BALANCE
                        MY_ACCOUNT_BALANCE_ACCOUNT_TYPE(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-MY_ACCOUNT_BALANCE(ACCOUNT_TYPE)"),//For type: FOSA / BOSA / ALL
                            MY_ACCOUNT_BALANCE_PIN(USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.getValue() + "-MY_ACCOUNT_BALANCE(PIN)"),
                                MY_ACCOUNT_BALANCE_END(USSDDataType.MY_ACCOUNT_BALANCE_PIN.getValue() + "-MY_ACCOUNT_BALANCE(END)"),

						//MINI STATEMENT
						MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-MY_ACCOUNT_MINI_STATEMENT(ACCOUNT_TYPE)"),
                                MY_ACCOUNT_MINI_STATEMENT_ACCOUNT(USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_TYPE.getValue() + "-MY_ACCOUNT_MINI_STATEMENT(ACCOUNT)"),
                                    MY_ACCOUNT_MINI_STATEMENT_PIN(USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT.getValue() + "-MY_ACCOUNT_MINI_STATEMENT(PIN)"),
                                        MY_ACCOUNT_MINI_STATEMENT_END(USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN.getValue() + "-MY_ACCOUNT_MINI_STATEMENT(END)"),

						//MAPP_ACTIVATION
						MAPP_ACTIVATION_ACTION(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-MAPP_ACTIVATION(ACTION)"),
							MAPP_ACTIVATION_CONFIRMATION(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-MAPP_ACTIVATION(CONFIRMATION)"),
								MAPP_ACTIVATION_END(USSDDataType.MAPP_ACTIVATION_CONFIRMATION.getValue() + "-MAPP_ACTIVATION(END)"),

                        //MEMBER_REGISTRATION
                        ACCOUNT_REGISTRATION_ACTION(USSDDataType.MY_ACCOUNT_MENU.getValue() + "-ACCOUNT_REGISTRATION(ACTION)"),
                            ACCOUNT_REGISTRATION_NAME(USSDDataType.ACCOUNT_REGISTRATION_ACTION.getValue() + "-ACCOUNT_REGISTRATION(NAME)"),
                                ACCOUNT_REGISTRATION_MOBILE_NUMBER(USSDDataType.ACCOUNT_REGISTRATION_NAME.getValue() + "-ACCOUNT_REGISTRATION(MOBILE_NUMBER)"),
                                    ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER(USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.getValue() + "-ACCOUNT_REGISTRATION(NATIONAL_ID_NUMBER)"),
                                        ACCOUNT_REGISTRATION_DATE_OF_BIRTH(USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.getValue() + "-ACCOUNT_REGISTRATION(DATE_OF_BIRTH)"),
                                            ACCOUNT_REGISTRATION_PIN(USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.getValue() + "-ACCOUNT_REGISTRATION(PIN)"),
                                                ACCOUNT_REGISTRATION_CONFIRMATION(USSDDataType.ACCOUNT_REGISTRATION_PIN.getValue() + "-ACCOUNT_REGISTRATION(CONFIRMATION)"),
                                                    ACCOUNT_REGISTRATION_END(USSDDataType.ACCOUNT_REGISTRATION_CONFIRMATION.getValue() + "-ACCOUNT_REGISTRATION(END)"),

                        //SELF_REGISTRATION
                        SELF_REGISTRATION_ACTION(USSDDataType.INIT.getValue() + "-SELF_REGISTRATION(ACTION)"),
                            SELF_REGISTRATION_NAME(USSDDataType.SELF_REGISTRATION_ACTION.getValue() + "-SELF_REGISTRATION(NAME)"),
                                SELF_REGISTRATION_NATIONAL_ID_NUMBER(USSDDataType.SELF_REGISTRATION_NAME.getValue() + "-SELF_REGISTRATION(NATIONAL_ID_NUMBER)"),
                                    SELF_REGISTRATION_DATE_OF_BIRTH(USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.getValue() + "-SELF_REGISTRATION(DATE_OF_BIRTH)"),
                                        SELF_REGISTRATION_CONFIRMATION(USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.getValue() + "-SELF_REGISTRATION(CONFIRMATION)"),
                                            SELF_REGISTRATION_END(USSDDataType.SELF_REGISTRATION_CONFIRMATION.getValue() + "-SELF_REGISTRATION(END)"),

						//LOAN
						LOAN_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-LOAN(MENU)"),
							LOAN_QUALIFICATION_TYPE(USSDDataType.LOAN_MENU.getValue() + "-LOAN_QUALIFICATION(TYPE)"),
								LOAN_QUALIFICATION_END(USSDDataType.LOAN_QUALIFICATION_TYPE.getValue() + "-LOAN_QUALIFICATION(END)"),

                            LOAN_GUARANTORSHIP_ABILITY_END(USSDDataType.LOAN_MENU.getValue() + "-LOAN_GUARANTORSHIP_ABILITY(END)"),

							LOAN_APPLICATION_TYPE(USSDDataType.LOAN_MENU.getValue() + "-LOAN_APPLICATION(TYPE)"),
								LOAN_APPLICATION_AMOUNT(USSDDataType.LOAN_APPLICATION_TYPE.getValue() + "-LOAN_APPLICATION(AMOUNT)"),
									LOAN_APPLICATION_PIN(USSDDataType.LOAN_APPLICATION_AMOUNT.getValue() + "-LOAN_APPLICATION(PIN)"),
										LOAN_APPLICATION_CONFIRMATION(USSDDataType.LOAN_APPLICATION_PIN.getValue() + "-LOAN_APPLICATION(CONFIRMATION)"),
                                            LOAN_APPLICATION_END(USSDDataType.LOAN_APPLICATION_CONFIRMATION.getValue() + "-LOAN_APPLICATION(END)"),


                            LOAN_REPAYMENT_OPTION(USSDDataType.LOAN_MENU.getValue() + "-LOAN_REPAYMENT(OPTION)"),
                                LOAN_REPAYMENT_FUNDS_ACCOUNT(USSDDataType.LOAN_REPAYMENT_OPTION.getValue() + "-LOAN_REPAYMENT(FUNDS_ACCOUNT)"),
                                    LOAN_REPAYMENT_LOAN(USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.getValue() + "-LOAN_REPAYMENT(LOAN)"),
                                        LOAN_REPAYMENT_AMOUNT(USSDDataType.LOAN_REPAYMENT_LOAN.getValue() + "-LOAN_REPAYMENT(AMOUNT)"),
                                            LOAN_REPAYMENT_PIN(USSDDataType.LOAN_REPAYMENT_AMOUNT.getValue() + "-LOAN_REPAYMENT(PIN)"),
                                                LOAN_REPAYMENT_CONFIRMATION(USSDDataType.LOAN_REPAYMENT_PIN.getValue() + "-LOAN_REPAYMENT(CONFIRMATION)"),
                                                    LOAN_REPAYMENT_END(USSDDataType.LOAN_REPAYMENT_CONFIRMATION.getValue() + "-LOAN_REPAYMENT(END)"),

                        LOAN_GUARANTORS_TYPE(USSDDataType.LOAN_MENU.getValue() + "-LOAN_GUARANTORS(TYPE)"),
                            LOAN_GUARANTORS_OPTION(USSDDataType.LOAN_GUARANTORS_TYPE.getValue() + "-LOAN_GUARANTORS(OPTION)"),
                                LOAN_GUARANTORS_GUARANTORS(USSDDataType.LOAN_GUARANTORS_OPTION.getValue() + "-LOAN_GUARANTORS(GUARANTORS)"),
                                    LOAN_GUARANTORS_MOBILE_NUMBER(USSDDataType.LOAN_GUARANTORS_OPTION.getValue() + "-LOAN_GUARANTORS(MOBILE_NUMBER)"),
                                        LOAN_GUARANTORS_PIN(USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.getValue() + "-LOAN_GUARANTORS(PIN)"),
                                            LOAN_GUARANTORS_CONFIRMATION(USSDDataType.LOAN_GUARANTORS_PIN.getValue() + "-LOAN_GUARANTORS(CONFIRMATION)"),
                                                LOAN_GUARANTORS_END(USSDDataType.LOAN_GUARANTORS_CONFIRMATION.getValue() + "-LOAN_GUARANTORS(END)"),


                        LOANS_GUARANTEED_OPTION(USSDDataType.LOAN_MENU.getValue() + "-LOANS_GUARANTEED(OPTION)"),
                            LOANS_GUARANTEED_TYPE(USSDDataType.LOANS_GUARANTEED_OPTION.getValue() + "-LOANS_GUARANTEED(TYPE)"),
                                LOANS_GUARANTEED_LOAN_DETAILS(USSDDataType.LOANS_GUARANTEED_TYPE.getValue() + "-LOANS_GUARANTEED(LOAN_DETAILS)"),
                                    LOANS_GUARANTEED_PIN(USSDDataType.LOANS_GUARANTEED_LOAN_DETAILS.getValue() + "-LOANS_GUARANTEED(PIN)"),
                                        LOANS_GUARANTEED_CONFIRMATION(USSDDataType.LOANS_GUARANTEED_PIN.getValue() + "-LOANS_GUARANTEED(CONFIRMATION)"),
                                            LOANS_GUARANTEED_END(USSDDataType.LOANS_GUARANTEED_CONFIRMATION.getValue() + "-LOANS_GUARANTEED(END)"),

						/*LOAN_QUALIFICATION_TYPE(USSDDataType.LOAN_MENU.getValue() + "-LOAN_QUALIFICATION(TYPE)"),
							LOAN_QUALIFICATION_END(USSDDataType.LOAN_QUALIFICATION_TYPE.getValue() + "-LOAN_QUALIFICATION(END)"),

						LOAN_APPLICATION_TYPE(USSDDataType.LOAN_MENU.getValue() + "-LOAN_APPLICATION(TYPE)"),
							LOAN_APPLICATION_AMOUNT(USSDDataType.LOAN_APPLICATION_TYPE.getValue() + "-LOAN_APPLICATION(AMOUNT)"),
								LOAN_APPLICATION_PIN(USSDDataType.LOAN_APPLICATION_AMOUNT.getValue() + "-LOAN_APPLICATION(PIN)"),
									LOAN_APPLICATION_CONFIRMATION(USSDDataType.LOAN_APPLICATION_PIN.getValue() + "-LOAN_APPLICATION(CONFIRMATION)"),
										LOAN_APPLICATION_END(USSDDataType.LOAN_APPLICATION_CONFIRMATION.getValue() + "-LOAN_APPLICATION(END)"),


						LOAN_REPAYMENT_MENU(USSDDataType.LOAN_MENU.getValue() + "-LOAN_REPAYMENT(MENU)"),
							LOAN_REPAYMENT_ACCOUNT(USSDDataType.LOAN_REPAYMENT_MENU.getValue() + "-LOAN_REPAYMENT(ACCOUNT)"),
								LOAN_REPAYMENT_AMOUNT(USSDDataType.LOAN_REPAYMENT_ACCOUNT.getValue() + "-LOAN_REPAYMENT(AMOUNT)"),
									LOAN_REPAYMENT_PIN(USSDDataType.LOAN_REPAYMENT_AMOUNT.getValue() + "-LOAN_REPAYMENT(PIN)"),
											LOAN_REPAYMENT_CONFIRMATION(USSDDataType.LOAN_REPAYMENT_PIN.getValue() + "-LOAN_REPAYMENT(CONFIRMATION)"),
												LOAN_REPAYMENT_END(USSDDataType.LOAN_REPAYMENT_CONFIRMATION.getValue() + "-LOAN_REPAYMENT(END)"),
							*/

					//FUNDS TRANSFER
					FUNDS_TRANSFER_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-FUNDS_TRANSFER(MENU)"),

						//INTERNAL TRANSFER
						FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT(USSDDataType.FUNDS_TRANSFER_MENU.getValue() + "-FUNDS_TRANSFER_INTERNAL(FROM_ACCOUNT)"),
							FUNDS_TRANSFER_INTERNAL_OPTION(USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.getValue() + "-FUNDS_TRANSFER_INTERNAL(OPTION)"),
								FUNDS_TRANSFER_INTERNAL_TO_OPTION(USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.getValue() + "-FUNDS_TRANSFER_INTERNAL(TO_OPTION)"),
                                    FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER(USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.getValue() + "-FUNDS_TRANSFER_INTERNAL(TO_IDENTIFIER)"),
                                        FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT(USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.getValue() + "-FUNDS_TRANSFER_INTERNAL(TO_ACCOUNT)"),
                                            FUNDS_TRANSFER_INTERNAL_AMOUNT(USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.getValue() + "-FUNDS_TRANSFER_INTERNAL(AMOUNT)"),
                                                FUNDS_TRANSFER_INTERNAL_PIN(USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.getValue() + "-FUNDS_TRANSFER_INTERNAL(PIN)"),
                                                    FUNDS_TRANSFER_INTERNAL_CONFIRMATION(USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN.getValue() + "-FUNDS_TRANSFER_INTERNAL(CONFIRMATION)"),
                                                        FUNDS_TRANSFER_INTERNAL_END(USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION.getValue() + "-FUNDS_TRANSFER_INTERNAL(END)"),

						//EXTERNAL TRANSFER
						FUNDS_TRANSFER_EXTERNAL_BANK(USSDDataType.FUNDS_TRANSFER_MENU.getValue() + "-FUNDS_TRANSFER_EXTERNAL(BANK)"),
							FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO(USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.getValue() + "-FUNDS_TRANSFER_EXTERNAL(TO_BANK_ACCOUNT_NO)"),
								FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME(USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.getValue() + "-FUNDS_TRANSFER_EXTERNAL(TO_BANK_ACCOUNT_NO)"),
									FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT(USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME.getValue() + "-FUNDS_TRANSFER_EXTERNAL(FROM_ACCOUNT)"),
										FUNDS_TRANSFER_EXTERNAL_AMOUNT(USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.getValue() + "-FUNDS_TRANSFER_EXTERNAL(AMOUNT)"),
											FUNDS_TRANSFER_EXTERNAL_PIN(USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.getValue() + "-FUNDS_TRANSFER_EXTERNAL(PIN)"),
												FUNDS_TRANSFER_EXTERNAL_CONFIRMATION(USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.getValue() + "-FUNDS_TRANSFER_EXTERNAL(CONFIRMATION)"),
													FUNDS_TRANSFER_EXTERNAL_END(USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION.getValue() + "-FUNDS_TRANSFER_EXTERNAL(END)"),

						//FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT
						FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_MENU(USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.getValue() + "-FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(MENU)"),
							FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT(USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_MENU.getValue() + "-FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(ACCOUNT)"),
								FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME(USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT.getValue() + "-FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(NAME)"),
							FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE(USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_MENU.getValue() + "-FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(REMOVE)"),

					//ACCOUNT_REGISTRATION_MENU
					//ACCOUNT_REGISTRATION_MENU(USSDDataType.MAIN_IN_MENU.getValue() + "-ACCOUNT_REGISTRATION(MENU)"),
					//BINGWA SACCO TO PROVIDE CLARITY ON SUB MENUS

					//CHANGE_PIN
					CHANGE_PIN_CURRENT_PIN(USSDDataType.MAIN_IN_MENU.getValue() + "-CHANGE_PIN(CURRENT_PIN)"),
						CHANGE_PIN_NEW_PIN(USSDDataType.CHANGE_PIN_CURRENT_PIN.getValue() + "-CHANGE_PIN(NEW_PIN)"),
							CHANGE_PIN_CONFIRM_PIN(USSDDataType.CHANGE_PIN_NEW_PIN.getValue() + "-CHANGE_PIN(CONFIRM_PIN)"),
								CHANGE_PIN_END(USSDDataType.CHANGE_PIN_CONFIRM_PIN.getValue() + "-CHANGE_PIN(END)"),


			//FORGOT PIN
			FORGOT_PIN_ID_NO(USSDDataType.INIT.getValue() + "-FORGOT_PIN(ID_NO)"),
				FORGOT_PIN_DOB(USSDDataType.FORGOT_PIN_ID_NO.getValue() + "-FORGOT_PIN(DOB)"),
					FORGOT_PIN_END(USSDDataType.FORGOT_PIN_DOB.getValue() + "-FORGOT_PIN(END)"),


			//SET_PIN
			SET_PIN_ID_NO(USSDDataType.LOGIN_PIN.getValue() + "-SET_PIN(ID_NO)"),
        SET_PIN_NEW_PIN(USSDDataType.SET_PIN_ID_NO.getValue() + "-SET_PIN(NEW_PIN)"),
        SET_PIN_CONFIRM_PIN(USSDDataType.SET_PIN_NEW_PIN.getValue() + "-SET_PIN(CONFIRM_PIN)"),
        SET_PIN_TC(USSDDataType.SET_PIN_CONFIRM_PIN.getValue() + "-SET_PIN(TC)"),
        SET_PIN_END(USSDDataType.SET_PIN_TC.getValue() + "-SET_PIN(END)"),

        //GENERAL_MENU
        GENERAL_MENU(USSDDataType.INIT.getValue() + "-GENERAL(MENU)"),

        //BUSINESS_SHORT_CODE
        BUY_GOODS_MENU(USSDDataType.GENERAL_MENU.getValue() + "-BUY_GOODS(BUY_GOODS)"),
            BUY_GOODS_BUSINESS_SHORT_CODE(USSDDataType.BUY_GOODS_MENU.getValue() + "-BUY_GOODS(BUSINESS_SHORT_CODE)"),
                BUY_GOODS_AMOUNT(USSDDataType.BUY_GOODS_BUSINESS_SHORT_CODE.getValue() + "-BUY_GOODS(AMOUNT)"),
                    BUY_GOODS_CONFIRMATION(USSDDataType.BUY_GOODS_AMOUNT.getValue() + "-BUY_GOODS(CONFIRMATION)"),
                        BUY_GOODS_END(USSDDataType.BUY_GOODS_CONFIRMATION.getValue() + "-BUY_GOODS(END)"),

        USSD_PROCESS_OVERIDE_BACK("USSD_PROCESS_OVERIDE(BACK)"),
        USSD_PROCESS_OVERIDE_HOME("USSD_PROCESS_OVERIDE(HOME)"),
        USSD_PROCESS_OVERIDE_ERROR("USSD_PROCESS_OVERIDE(ERROR)"),
        USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND("USSD_PROCESS_OVERIDE(DATA_TYPE_NOT_FOUND)");

        private final String strValue;

        USSDDataType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }
}
