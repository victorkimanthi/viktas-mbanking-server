package ke.skyworld.mbanking.ussdapi;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.PESA;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESAProcessor;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.cbs.CBSAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPIUtils;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.sp.manager.SPManagerConstants;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static ke.skyworld.mbanking.ussdapi.APIUtils.fnSendSMS;
import static ke.skyworld.mbanking.ussdapplication.AppConstants.strAppID;

public class USSDAPI {
	public USSDAPI() {
	}

	private final HashMap<String, String> payBillCodesHashMap = new HashMap<>();

	public HashMap<String, String> checkUser(USSDRequest theUSSDRequest) {

		USSDAPIConstants.CheckUserReturnVal rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
		String strCheckStatus = USSDAPIConstants.CheckUserReturnVal.ERROR.getValue();

		HashMap<String, String> transactionReturnVal = new HashMap<>();
		transactionReturnVal.put("CHECK_USER_RVAL", rVal.getValue());

		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			HashMap<String,String> hmRVal = CBSAPI.checkUser(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, "IMSI", strSIMID);
			strCheckStatus = hmRVal.get("user_status");

			switch (strCheckStatus) {
				case "ACTIVE": {
					rVal = USSDAPIConstants.CheckUserReturnVal.ACTIVE;
					break;
				}
				case "INVALID_DEVICE_IDENTIFIER": {
					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMSI;
					break;
				}
				case "INVALID_IMSI": {
					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMSI;
					break;
				}
				case "INVALID_IMEI":{
					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMEI;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.CheckUserReturnVal.BLOCKED;
					break;
				}
				case "SUSPENDED": {
					rVal = USSDAPIConstants.CheckUserReturnVal.SUSPENDED;
					transactionReturnVal.put("DB_LOGIN_ACTION_VALID_DATE", hmRVal.get("auth_action_valid_date"));
					break;
				}
				case "LOCKED": {
					transactionReturnVal.put("rVal", USSDAPIConstants.CheckUserReturnVal.LOCKED.getValue());
					break;
				}
				case "NOT_FOUND":{
					rVal = USSDAPIConstants.CheckUserReturnVal.NOT_FOUND;
					break;
				}
				case "ERROR":{
					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
					break;
				} default: {
					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
				}
			}

			transactionReturnVal.put("CHECK_USER_RVAL", rVal.getValue());

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return transactionReturnVal;
	}

//	public USSDAPIConstants.CheckUserReturnVal checkUser(String theUserMSISDN) {
//
//		USSDAPIConstants.CheckUserReturnVal rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
//		try {
//			String strSIMID = "";
//
//			//todo - Implement Integration to CBS
//			//String strCheckStatus = Navision.getPort().userCheck(theUserMSISDN, strSIMID, true, "");
//
//
//			/*
//				REQUEST:
//				{
//				"action": "CHECK_USER",
//					"payload": {
//					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
//					"identifier_type": "MSISDN",
//					"identifier": "254712345678",
//					"device_identifier_type": "IMSI",
//					"device_identifier": "1099200912931023"
//					}
//				}
//
//				RESPONSE:
//				{
//					"transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
//					"transaction_status_date_time": "2021-04-07 19:34:07",
//					"user_status": "ACTIVE",
//					"auth_action": "NONE",
//					"auth_action_valid_date": "",
//					"login_flag": "",
//					"login_attempts": 0,
//					"otp_flag": "",
//					"otp_attempts": "0"
//				}
//				 */
//
//			JSONObject jsonRequest = new JSONObject();
//			jsonRequest.put("action", "CHECK_USER");
//
//
//			JSONObject jsonRequestPayload = new JSONObject();
//			jsonRequestPayload.put("api_request_id",  UUID.randomUUID().toString());
//			jsonRequestPayload.put("identifier_type", "MSISDN");
//			jsonRequestPayload.put("identifier", theUserMSISDN);
//			jsonRequestPayload.put("device_identifier_type", "IMSI");
//			jsonRequestPayload.put("device_identifier", strSIMID);
//			jsonRequest.put("payload", jsonRequestPayload);
//
//			String strJSONRequest = jsonRequest.toString();
//			String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);
//
//			JSONObject jsonResponse = null;
//
//			String strCheckStatus = USSDAPIConstants.CheckUserReturnVal.ERROR.getValue();
//			if(strJSONResponse!=null){
//				try {
//					jsonResponse = new JSONObject(strJSONResponse);
//					strCheckStatus = jsonResponse.get("user_status").toString();
//				}catch (Exception e){
//					System.out.println("Error converting String to JSON");
//				}
//			}else {
//				System.out.println("Received NULL Response");
//			}
//
//			switch (strCheckStatus) {
//				case "ACTIVE": {
//					rVal = USSDAPIConstants.CheckUserReturnVal.ACTIVE;
//					break;
//				}
//				case "INVALID_IMSI": {
//					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMSI;
//					break;
//				}
//				case "INVALID_IMEI":{
//					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMEI;
//					break;
//				}
//				case "BLOCKED":{
//					rVal = USSDAPIConstants.CheckUserReturnVal.BLOCKED;
//					break;
//				}
//				case "SUSPENDED": {
//					rVal = USSDAPIConstants.CheckUserReturnVal.SUSPENDED;
//					break;
//				}
//				case "NOT_FOUND":{
//					rVal = USSDAPIConstants.CheckUserReturnVal.NOT_FOUND;
//					break;
//				}
//				case "ERROR":{
//					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
//					break;
//				} default: {
//					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
//				}
//			}
//		} catch (Exception e) {
//			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
//		}
//		return rVal;
//	}

	public USSDAPIConstants.CheckUserReturnVal MOCheckUser(String theRequestCorrelationID, String theUserMSISDN) {

		USSDAPIConstants.CheckUserReturnVal rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
		String strCheckStatus = USSDAPIConstants.CheckUserReturnVal.ERROR.getValue();
		try {
			String strSIMID = "";
			HashMap<String,String> hmRVal = CBSAPI.MOCheckUser(theRequestCorrelationID, "MSISDN", theUserMSISDN);
			strCheckStatus = hmRVal.get("user_status");

			switch (strCheckStatus) {
				case "FOUND":
				case "ACTIVE": {
					rVal = USSDAPIConstants.CheckUserReturnVal.ACTIVE;
					break;
				}
				case "INVALID_IMSI": {
					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMSI;
					break;
				}
				case "INVALID_IMEI":{
					rVal = USSDAPIConstants.CheckUserReturnVal.INVALID_IMEI;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.CheckUserReturnVal.BLOCKED;
					break;
				}
				case "SUSPENDED": {
					rVal = USSDAPIConstants.CheckUserReturnVal.SUSPENDED;
					break;
				}
				case "NOT_FOUND":{
					rVal = USSDAPIConstants.CheckUserReturnVal.NOT_FOUND;
					break;
				}
				case "ERROR":{
					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
					break;
				} default: {
					rVal = USSDAPIConstants.CheckUserReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	//todo PENDING
	public String getUserAuthActionExpiryTime(USSDRequest theUSSDRequest){
		String rVal = "";
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			/*
			{
				"action": "GET_AUTH_SECURITY_PARAMETERS",
				"payload": {
					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"identifier_type": "MSISDN",
					"identifier": "254712345678",
					 "pin": "{{pin}}",
					"device_identifier_type": "IMSI",
					"device_identifier": "1099200912931023"
					"auth_security_type": "LOGIN"
				}
			 }
			 */

			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("action", "GET_AUTH_SECURITY_PARAMETERS");

			JSONObject jsonRequestPayload = new JSONObject();
			jsonRequestPayload.put("api_request_id",  theUSSDRequest.getUSSDTraceID());
			jsonRequestPayload.put("identifier_type", "MSISDN");
			jsonRequestPayload.put("identifier", strMobileNumber);
			jsonRequestPayload.put("pin", strPIN);
			jsonRequestPayload.put("device_identifier_type", "IMSI");
			jsonRequestPayload.put("device_identifier", strSIMID);
			jsonRequest.put("payload", jsonRequestPayload);

			String strJSONRequest = jsonRequest.toString();
			String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

			String strLoginAttempts = "";
			String strAuthActionValidDate = "";

			JSONObject jsonResponse = null;
			if(strJSONResponse!=null){
				try {
					jsonResponse = new JSONObject(strJSONResponse);
					strLoginAttempts = jsonResponse.get("login_attempts").toString();
					strAuthActionValidDate = jsonResponse.get("auth_action_valid_date").toString();

					Date dtAuthActionValidDate =new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").parse(strAuthActionValidDate);
					Date dtNow =new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").parse(MBankingDB.getDBDateTime());

					rVal = "Please try again in " + APIUtils.getPrettyDateTimeDifferenceRoundedUp(dtNow, dtAuthActionValidDate);

				}catch (Exception e){
					System.out.println("Error converting String to JSON");
				}
			}else {
				System.out.println("Received NULL Response");
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	public HashMap<String, String> userLogin(USSDRequest theUSSDRequest) {
		HashMap<String, String> loginReturnVal = new HashMap<>();
		String rVal;
		String strLoginAttemptMessage = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
		loginReturnVal.put("LOGIN_RETURN_VALUE", "ERROR");
		loginReturnVal.put("LOGIN_ATTEMPT_MESSAGE", strLoginAttemptMessage);

		try {
			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			HashMap<String,String> hmRVal = CBSAPI.userLogin(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID);
			String strLoginStatus = hmRVal.get("login_status");
			String strLoginAttempts = hmRVal.get("login_attempts");
			String strDbLoginActionValidDate = hmRVal.get("auth_action_valid_date");
			loginReturnVal.put("DB_LOGIN_ACTION_VALID_DATE", strDbLoginActionValidDate);

			int intUserLoginAttemptsCount = 0;

			try {  intUserLoginAttemptsCount =  Integer.parseInt(strLoginAttempts); }catch (Exception e){}

			if(strLoginStatus.equals("INCORRECT_PIN")) {

				//Increase login attempts
				int intCurrentLoginAttempts = intUserLoginAttemptsCount+1;

				HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
				hmMSGPlaceholders.put("[MOBILE_NUMBER]", strMobileNumber);
				hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(intUserLoginAttemptsCount));
				hmMSGPlaceholders.put("[FIRST_NAME]", "Member" /*getMemberAccountDetails(theUSSDRequest, "full_name")*/);
				//hmMSGPlaceholders.put("[SUSPEND_DURATION]", APIUtils.getCustomDuration(strDbLoginActionValidDate));

				String xml = USSDLocalParameters.getClientXMLParameters();
				HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(intUserLoginAttemptsCount,
						hmMSGPlaceholders, xml, MBankingConstants.AuthType.PASSWORD);

				HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
				HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

				//Default Incorrect PIN message
				strLoginAttemptMessage = "{Sorry the PIN provided is NOT correct}\nPlease enter your PIN to proceed:";
				loginReturnVal.put("END_SESSION", "NO");

				//Check if action is needed
				if(!currentAuthenticationAttemptsAction.isEmpty()){
					String strLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
					String strLoginActionTag = currentAuthenticationAttemptsAction.get("NAME");

					//Check action
					switch (strLoginAction) {
						case "SUSPEND": {
							int loginActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
							String loginActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
							loginActionDuration = APIUtils.convertToSeconds(loginActionDuration, loginActionDurationUnit);
							Date loginActionValidDate = APIUtils.add(loginActionDuration, Calendar.SECOND);
							String strLoginActionValidDate = APIUtils.convertDateToDateString(loginActionValidDate);

							//Persist Action to DB
							HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
									"PASSWORD",  strLoginAction, strLoginActionValidDate, strLoginActionTag, APIUtils.getCurrentDateTime());
							String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " +loginActionDurationUnit+"(S)";

							if(!hmRValAuth.isEmpty()){
								String setAuthStatus = hmRValAuth.get("set_auth_security_parameters_status");
								if(setAuthStatus.equals("SUCCESS")){
									//Override Incorrect PIN message
									strLoginAttemptMessage = "Your mobile banking account has been SUSPENDED for "+friendlyActionDuration;
									loginReturnVal.put("END_SESSION", "YES");
								}
							}
							break;
						}

						case "LOCK": {
							//Persist Action to DB
							HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
									"PASSWORD",  strLoginAction, null, strLoginActionTag, APIUtils.getCurrentDateTime());

							if(!hmRValAuth.isEmpty()){
								String setAuthStatus = hmRValAuth.get("set_auth_security_parameters_status");
								if(setAuthStatus.equals("SUCCESS")){
									//Override Incorrect PIN message
									strLoginAttemptMessage = "Your mobile banking account has been LOCKED. Please visit one of our branches for assistance or contact us. ";
									loginReturnVal.put("END_SESSION", "YES");
								}
							}
							break;
						}

						default: {
							//Persist Action to DB
							HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
									"PASSWORD",  strLoginAction, null, strLoginActionTag, APIUtils.getCurrentDateTime());
						}
					}
				}

				//Check future action
				if(!futureAuthenticationAttemptsAction.isEmpty()){
					String futureLoginAction = futureAuthenticationAttemptsAction.get("ACTION");
					String futureLoginActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
					String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " +futureLoginActionDurationUnit+"(S)";
					String attemptsRemainingToFutureLoginAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

					String currentLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
					if(currentLoginAction == null) currentLoginAction = "NONE";

					//Override Incorrect PIN message
					if(futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")){
						if(loginReturnVal.get("END_SESSION").equals("NO")){
							strLoginAttemptMessage = "{Sorry the PIN provided is NOT correct}\nYou have "+attemptsRemainingToFutureLoginAction+" attempt(s) before your mobile banking account is SUSPENDED for "+friendlyFutureActionDuration+".\nPlease enter your PIN:";
						}
					} else if(futureLoginAction.equals("LOCK") && !currentLoginAction.equals("LOCK")){
						if(loginReturnVal.get("END_SESSION").equals("NO")){
							strLoginAttemptMessage = "{Sorry the PIN provided is NOT correct}\nYou have "+attemptsRemainingToFutureLoginAction+" attempt(s) before your mobile banking account is LOCKED. Please enter your PIN:";
						}
					}
				}
			} else if (strLoginStatus.equals("SUCCESS")) {
				//Reset Login Auth Parameters
				HashMap<String,String> hmRValAuth = CBSAPI.setAuthSecurityParameters(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
						"PASSWORD",  "NONE", null, null, APIUtils.getCurrentDateTime());
			}

			loginReturnVal.put("LOGIN_RETURN_VALUE", strLoginStatus);
			loginReturnVal.put("LOGIN_ATTEMPT_MESSAGE", strLoginAttemptMessage);

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return loginReturnVal;
	}

	//todo DONE
	public USSDAPIConstants.SetPINReturnVal setUserPIN(USSDRequest theUSSDRequest) {

		USSDAPIConstants.SetPINReturnVal rVal = USSDAPIConstants.SetPINReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
			String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name());
			String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());

			HashMap<String,String > hmRVal = CBSAPI.setUserPIN(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
					strNewPIN, "NATIONAL_ID", strIDNo);

			String strLoginStatus = hmRVal.get("login_status");
			String strLoginAttempts = hmRVal.get("login_attempts");
			String strSetPinStatus = hmRVal.get("set_pin_status");
			String strSetPinStatusDescription = hmRVal.get("set_pin_status_description");

			switch (strSetPinStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.SetPINReturnVal.SUCCESS;
					break;
				}
				case "INVALID_ACCOUNT": {
					rVal = USSDAPIConstants.SetPINReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.SetPINReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_NEW_PIN":{
					rVal = USSDAPIConstants.SetPINReturnVal.INVALID_NEW_PIN;
					break;
				}
				case "ERROR":{
					rVal = USSDAPIConstants.SetPINReturnVal.ERROR;
					break;
				}
				default:{
					rVal = USSDAPIConstants.SetPINReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING confirm return val if change_pin_status or set_pin_status ???
	public USSDAPIConstants.ChangePINReturnVal changeUserPIN(USSDRequest theUSSDRequest) {
		USSDAPIConstants.ChangePINReturnVal rVal = USSDAPIConstants.ChangePINReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN.name());
			String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name());

			HashMap<String,String> hmRVal = CBSAPI.changeUserPIN(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strNewPIN);
			String strChangePinStatus = hmRVal.get("change_pin_status");
			String strChangePinStatusDescription = hmRVal.get("change_pin_status_description");

				switch (strChangePinStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.ChangePINReturnVal.SUCCESS;
					break;
				}
				case "INCORRECT_PIN": {
					rVal = USSDAPIConstants.ChangePINReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_NEW_PIN":{
					rVal = USSDAPIConstants.ChangePINReturnVal.INVALID_NEW_PIN;
					break;
				}
				case "ERROR":{
					rVal = USSDAPIConstants.ChangePINReturnVal.ERROR;
					break;
				}
				default:{
					rVal = USSDAPIConstants.ChangePINReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING
	public USSDAPIConstants.AccountRegistrationReturnVal accountRegistration(USSDRequest theUSSDRequest) {
		USSDAPIConstants.AccountRegistrationReturnVal rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMemberName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME.name());
			String strMemberMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.name());
			String strMemberNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.name());
			String strMemberDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.name());

			DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

			strMemberDateOfBirth = strMemberDateOfBirth.replaceAll("\\D", "/");

			Date dtMemberDateOfBirth = format.parse(strMemberDateOfBirth);

			GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
			calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
			XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

			strMemberMobileNumber = APIUtils.sanitizePhoneNumber(strMemberMobileNumber);

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			//todo - Implement Integration to CBS
			//String strMemberVirtualRegistrationStatus =  Navision.getPort().registerVirtualMember(strMemberName, strMemberNationalIDNumber, strMemberMobileNumber, xmlGregCalMemberDateOfBirth, strMobileNumber, strEntryNumber);
			String strMemberVirtualRegistrationStatus =  USSDAPIConstants.AccountRegistrationReturnVal.SUCCESS.getValue();

			switch (strMemberVirtualRegistrationStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.SUCCESS;
					break;
				}
				case "MEMBER_EXISTS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.MEMBER_EXISTS;
					break;
				}
				case "ENTRY_EXISTS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ENTRY_EXISTS;
					break;
				}
				case "PIN_MISMATCH": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.PIN_MISMATCH;
					break;
				}
				case "INVALID_PIN": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_PIN;
					break;
				}
				case "INVALID_FIRSTNAME": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_FIRSTNAME;
					break;
				}
				case "INVALID_LASTNAME": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_LASTNAME;
					break;
				}
				case "INVALID_IDNO": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_IDNO;
					break;
				}
				case "INVALID_DOB": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_DOB;
					break;
				}
				case "ERROR": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
					break;
				}
				default:{
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING
	public USSDAPIConstants.AccountRegistrationReturnVal selfRegistration(USSDRequest theUSSDRequest) {
		USSDAPIConstants.AccountRegistrationReturnVal rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMemberName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NAME.name());
			String strMemberNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.name());
			String strMemberDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.name());

			DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

			strMemberDateOfBirth = strMemberDateOfBirth.replaceAll("\\D", "/");

			Date dtMemberDateOfBirth = format.parse(strMemberDateOfBirth);

			GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
			calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
			XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			//todo - Implement Integration to CBS
			//String strMemberVirtualRegistrationStatus = Navision.getPort().registerVirtualMember(strMemberName, strMemberNationalIDNumber, strMobileNumber, xmlGregCalMemberDateOfBirth, "", strEntryNumber);
			String strMemberVirtualRegistrationStatus = USSDAPIConstants.AccountRegistrationReturnVal.SUCCESS.getValue();

			switch (strMemberVirtualRegistrationStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.SUCCESS;
					break;
				}
				case "MEMBER_EXISTS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.MEMBER_EXISTS;
					break;
				}
				case "ENTRY_EXISTS": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ENTRY_EXISTS;
					break;
				}
				case "PIN_MISMATCH": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.PIN_MISMATCH;
					break;
				}
				case "INVALID_PIN": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_PIN;
					break;
				}
				case "INVALID_FIRSTNAME": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_FIRSTNAME;
					break;
				}
				case "INVALID_LASTNAME": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_LASTNAME;
					break;
				}
				case "INVALID_IDNO": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_IDNO;
					break;
				}
				case "INVALID_DOB": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.INVALID_DOB;
					break;
				}
				case "ERROR": {
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
					break;
				}
				default:{
					rVal = USSDAPIConstants.AccountRegistrationReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo DONE
	public LinkedHashMap<String, LinkedHashMap <String, String>> getBankAccounts(USSDRequest theUSSDRequest, USSDAPIConstants.AccountType theAccountType) {
		LinkedHashMap<String, LinkedHashMap <String, String>> accounts = null;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strAccountType = theAccountType.getValue();
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			accounts =  CBSAPI.getBankAccounts(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strAccountType);

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return accounts;
	}

	public HashMap<Object, Object> getIdentifierBankAccounts(USSDRequest theUSSDRequest, USSDAPIConstants.AccountType theAccountType) {
		HashMap<Object, Object> hmRVal = null;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strAccountType = theAccountType.getValue();
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			String strFromIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_IDENTIFIER.name());
			String strFromIdentifierType = "MSISDN";
			String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());

			if(strToOption.equalsIgnoreCase("Mobile No")){
				strFromIdentifierType = "MSISDN";
			}else if(strToOption.equalsIgnoreCase("ID Number")){
				strFromIdentifierType = "NATIONAL_ID";
			}else{
				strFromIdentifierType = "MSISDN";
			}

			hmRVal = CBSAPI.getUserDetails(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strFromIdentifierType, strFromIdentifier);

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return hmRVal;
	}

	public HashMap<String, String> verifyBusinessShortCode(USSDRequest theUSSDRequest) {
		HashMap<Object, Object> hmAPIRVal = null;
		HashMap<String, String> hmRVal = new HashMap<>();
		hmRVal.put("STATUS", "ERROR");
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strBusinessShortCode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_BUSINESS_SHORT_CODE.name());
			strBusinessShortCode = (strBusinessShortCode == null) ? "" : strBusinessShortCode;
			strBusinessShortCode = strBusinessShortCode.trim();

			/**
			 * {
			 *   "request_status": "SUCCESS/SHORT_CODE_NOT_FOUND/ERROR",
			 *   "business_short_code": "500100",
			 *   "business_name": "John's Hardware",
			 *   "associated_account": {
			 *     "account_name": "Salary Account",
			 *     "account_label": "Salary Account (41-02392-0093-01)",
			 *     "account_number": "41-02392-0093-01",
			 *     "account_balance": 1600.00
			 *   }
			 * }
			 */
			hmAPIRVal = CBSAPI.verifyBusinessShortCode(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber,"IMSI", strSIMID, "MSISDN", strMobileNumber, strBusinessShortCode);

			String requestStatus = String.valueOf(hmAPIRVal.get("request_status"));
			hmRVal.put("STATUS", requestStatus);

			if(requestStatus.equals("SUCCESS")){

				HashMap<String, String> associatedAccount = (HashMap<String, String>) hmAPIRVal.get("associated_account");

				hmRVal.put("BUSINESS_SHORT_CODE", String.valueOf(hmAPIRVal.get("business_short_code")));
				hmRVal.put("BUSINESS_NAME", String.valueOf(hmAPIRVal.get("business_name")));
				hmRVal.put("ASSOCIATED_ACCOUNT_NAME", String.valueOf(associatedAccount.get("account_name")));
				hmRVal.put("ASSOCIATED_ACCOUNT_LABEL", String.valueOf(associatedAccount.get("account_label")));
				hmRVal.put("ASSOCIATED_ACCOUNT_NUMBER", String.valueOf(associatedAccount.get("account_number")));
				hmRVal.put("ASSOCIATED_ACCOUNT_BALANCE", String.valueOf(associatedAccount.get("account_balance")));
			}

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return hmRVal;
	}

	//todo PENDING
	public HashMap<String, String> getAccountGroups(USSDRequest theUSSDRequest) {
		HashMap<String, String> groups = null;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			groups = new HashMap<>();

			/*
			<Groups>
				<Group>
					<Number>GRP001</Number>
					<Name>Bidii Youth Group</Name>
				</Group>
				<Group>
					<Number>GRP001</Number>
					<Name>Umoja Youth Group</Name>
				</Group>
				<Group>
					<Number>GRP001</Number>
					<Name>Westlands Empowerment Group</Name>
				</Group>
			</Groups>
			 */
			//todo - Implement Integration to CBS
			//String strGroupsXML = Navision.getPort().memberGroups(strMobileNumber);
			String strGroupsXML = "<Groups><Group><Number>GRP001</Number><Name>Bidii Youth Group</Name></Group><Group><Number>GRP001</Number><Name>Umoja Youth Group</Name></Group><Group><Number>GRP001</Number><Name>Westlands Empowerment Group</Name></Group></Groups>";


			InputSource source = new InputSource(new StringReader(strGroupsXML));
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document xmlDocument = builder.parse(source);
			XPath configXPath = XPathFactory.newInstance().newXPath();

			NodeList nlGroups = ((NodeList) configXPath.evaluate("/Groups", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
			groups = new HashMap<>();

			for (int i = 0; i < nlGroups.getLength(); i++) {
				NodeList nlGroup = ((NodeList) configXPath.evaluate("Group", nlGroups, XPathConstants.NODESET)).item(i).getChildNodes();
				groups.put(nlGroup.item(0).getTextContent(), nlGroup.item(1).getTextContent());
			}

			groups = (groups.size() > 0) ? groups : null;

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return groups;
	}

	//todo DONE
	public USSDAPIConstants.TransactionReturnVal accountBalanceEnquiry(USSDRequest theUSSDRequest, USSDAPIConstants.AccountType theAccountType) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		String strMSG  = "Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strAccountType = theAccountType.getValue();
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN.name());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			HashMap<String, HashMap <String, String>>  accounts = null;
			String strAccountBalanceEnquiryStatus = "ERROR";
			HashMap<String, Object> hmRVal =  CBSAPI.accountBalanceEnquiry(theUSSDRequest.getUSSDTraceID(), strTransactionID,"MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strAccountType);

			try{
				strAccountBalanceEnquiryStatus = (String) hmRVal.get("request_status");
				accounts = (HashMap<String, HashMap<String, String>>) hmRVal.get("accounts");
			}catch (Exception e){}

			//HashMap<String, HashMap <String, String>>
			switch (strAccountBalanceEnquiryStatus) {
				case "SUCCESS": {

					String strMSGDisplayAccountType = "";

					switch (theAccountType){
						case FOSA:  {
							strMSGDisplayAccountType = strMSGDisplayAccountType + "Savings";
							rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
							break;
						}
						case BOSA:  {
							strMSGDisplayAccountType = strMSGDisplayAccountType + "Shares, Deposits and Benevolent";

							rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
							break;
						}
						case LOAN:  {
							strMSGDisplayAccountType = strMSGDisplayAccountType + "Loans";
							rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
							break;
						}
						case ALL:  {
							strMSGDisplayAccountType = strMSGDisplayAccountType + "All";
							rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
							break;
						}
						default:  {
							rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
							break;
						}
					}

					try {

						if(accounts.size() > 0){
							String strAvailableBalances  = "";
							for (String account_no : accounts.keySet()) {
								HashMap<String, String> hmAccount = accounts.get(account_no);

								//String strMSGAccountTypeName = hmAccount.get("account_type_name");
								String strMSGAccountType = hmAccount.get("account_type");
								String strMSGAccountName = hmAccount.get("account_name");
								String strMSGAccountNumber =  hmAccount.get("account_number");
								String strMSGAccountBalance =  hmAccount.get("account_balance");
								String strMSGAccountTypeClass =  hmAccount.get("account_type_class");
								String strMSGAccountTypeName =  hmAccount.get("account_type_name");
								String strMSGAccountLabel =  hmAccount.get("account_label");

								String strMSGFormattedAvailableBalance = Utils.formatDouble(strMSGAccountBalance, "#,###.##");

								if(theAccountType.equals(USSDAPIConstants.AccountType.ALL)){ strAvailableBalances =  strAvailableBalances + strMSGAccountType + ":\n"; }
								strAvailableBalances  =  strAvailableBalances + strMSGAccountLabel + "\n";
								strAvailableBalances = strAvailableBalances + "Avail Bal: KES " + strMSGFormattedAvailableBalance + "\n\n" ;
							}
							strAvailableBalances = strAvailableBalances.trim();
							strMSG  = "Dear member, your " + strMSGDisplayAccountType + " account balance:\n" + strAvailableBalances;
						}else{
							strMSG = "Dear member, your balance enquiry request has FAILED. No account found. Please contact the SACCO if you have any active from " + strMSGDisplayAccountType + " accounts";
						}

					}catch (Exception e){
						strMSG  = "Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					}

					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "INCORRECT_PIN": {
					strMSG  = "Dear member, your balance enquiry request has FAILED due to INCORRECT PIN. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT": {
					strMSG  = "Dear member, your balance enquiry request has FAILED due to INVALID ACCOUNT. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					strMSG  = "Dear member, your balance enquiry request has FAILED due to INSUFFICIENT BALANCE. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "BLOCKED":{
					strMSG  = "Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					strMSG  = "Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			strMSG  = "Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}finally {
			try {
				/*
				String strOriginatorID = UUID.randomUUID().toString().toLowerCase();
				String strSourceReference = strOriginatorID;
				String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
				MSGAPI.sendMSG(strOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
						"MBANKING_SERVER", 210, "BALANCE_ENQUIRY", MSGConstants.Sensitivity.NORMAL, theUSSDRequest.getUSSDTraceID(), strSourceReference);
				*/
			}catch (Exception e){

			}
		}
		return rVal;
	}

	public String MOAccountBalanceEnquiry(String theRequestCorrelationID, String theSourceReference, String theUserMSISDN) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		StringBuilder strBalanceEnquiryMsg = new StringBuilder("Account Balances:\n");
		try {
			HashMap<String, HashMap <String, String>>  accounts = null;
			String strAccountBalanceEnquiryStatus = "ERROR";
			HashMap<String, Object> hmRVal =  CBSAPI.MOAccountBalanceEnquiry(theRequestCorrelationID, theSourceReference, "MSISDN", theUserMSISDN);

			try{
				strAccountBalanceEnquiryStatus = (String) hmRVal.get("request_status");
				accounts = (HashMap<String, HashMap<String, String>>) hmRVal.get("accounts");
			}catch (Exception e){}

			//HashMap<String, HashMap <String, String>>
			switch (strAccountBalanceEnquiryStatus) {
				case "SUCCESS": {
					try {
						if(accounts != null && !accounts.isEmpty()){
							HashMap<String, String> FOSABalances = new HashMap<>();
							HashMap<String, String> BOSABalances = new HashMap<>();
							HashMap<String, String> loanBalances = new HashMap<>();

							for(String accountNumber : accounts.keySet()) {
								HashMap<String, String> account = accounts.get(accountNumber);
								String accountType = account.get("account_type");
								String accountLabel = account.get("account_label");
								String accountBalance = account.get("account_balance");
								accountBalance = "KES "+Utils.formatDouble(accountBalance, "#,##0.00");

								switch (accountType) {
									case "FOSA": {
										FOSABalances.put(accountLabel, accountBalance);
										break;
									}
									case "BOSA": {
										BOSABalances.put(accountLabel, accountBalance);
										break;
									}
									case "LOAN": {
										loanBalances.put(accountLabel, accountBalance);
										break;
									}
								}
							}

							if(!FOSABalances.isEmpty()){
								strBalanceEnquiryMsg.append("Savings:\n");
								for(String account : FOSABalances.keySet()){
									strBalanceEnquiryMsg.append(account).append(": ").append(FOSABalances.get(account)).append("\n");
								}
								strBalanceEnquiryMsg.append("\n");
							}

							if(!BOSABalances.isEmpty()){
								strBalanceEnquiryMsg = new StringBuilder("\n" + strBalanceEnquiryMsg + "Schemes:\n");
								for(String account : BOSABalances.keySet()){
									strBalanceEnquiryMsg.append(account).append(": ").append(BOSABalances.get(account)).append("\n");
								}
								strBalanceEnquiryMsg.append("\n");
							}

							if(!loanBalances.isEmpty()){
								strBalanceEnquiryMsg = new StringBuilder("\n" + strBalanceEnquiryMsg + "Loans:\n");
								for(String account : loanBalances.keySet()){
									strBalanceEnquiryMsg.append(account).append(": ").append(loanBalances.get(account)).append("\n");
								}
								strBalanceEnquiryMsg.append("\n");
							}

							//Remove trailing new lines
							strBalanceEnquiryMsg = new StringBuilder(strBalanceEnquiryMsg.toString().trim());

							if(strBalanceEnquiryMsg.toString().equals("Account Balances:")) {
								strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.");
							}
						} else {
							strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. No account found. Please contact the SACCO if you have any active accounts.");
						}
					}catch (Exception e){
						strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.");
					}

					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "INCORRECT_PIN": {
					strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED due to INCORRECT PIN. Please try again later. Kindly contact the SACCO if the problem persists.");
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT": {
					strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED due to INVALID ACCOUNT. Please try again later. Kindly contact the SACCO if the problem persists.");
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED due to INSUFFICIENT BALANCE. Please try again later. Kindly contact the SACCO if the problem persists.");
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "BLOCKED":{
					strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.");
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.");
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			strBalanceEnquiryMsg = new StringBuilder("Dear member, your balance enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.");
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}finally {
			try {
				/*
				String strOriginatorID = UUID.randomUUID().toString().toLowerCase();
				String strSourceReference = strOriginatorID;
				String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
				MSGAPI.sendMSG(strOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
						"MBANKING_SERVER", 210, "BALANCE_ENQUIRY", MSGConstants.Sensitivity.NORMAL, theUSSDRequest.getUSSDTraceID(), strSourceReference);
				*/
			}catch (Exception e){

			}
		}
		return strBalanceEnquiryMsg.toString();
	}

	//todo PENDING - MOVE THIS TO MSG API
	public String mOAccountBalanceEnquiry(String theUserMSISDN) {
		String rVal = "Error";
		try {
			//todo - Implement Integration to CBS
			//rVal = Navision.getPort().accountBalanceEnquiry(UUID.randomUUID().toString().toUpperCase(), "", theUserMSISDN, "", "ALL");
			rVal = "Dear Member, your account balance is ...";
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}
		return rVal;
	}

	//todo DONE TO VERIFY
	public USSDAPIConstants.TransactionReturnVal accountMiniStatement(USSDRequest theUSSDRequest, USSDAPIConstants.AccountType theAccountType) {
		HashMap<Object, Object> hmRVal = null;
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		String strMSG  = "Dear member, your mini-statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
		try {

			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strStatementType = "MINI_STATEMENT";
			int intMaxNumberOfTransactions = 5;			  //Maximum Transactions on Statement
			String strEndDate = MBankingDB.getDBDateTime();
			String strStartDate = Utils.dateAdd(strEndDate, "yyyy-MM-dd HH:mm:ss", Calendar.MONTH, -3);  //Three Months from Now.
			String strAccountType = theAccountType.getValue();

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN.name());

			if(theAccountType.equals(USSDAPIConstants.AccountType.LOAN)){
				strMSG  = "Dear member, your loan mini-statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";

				String strAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT.name());
				HashMap <String, String> hmAccountNoDetails  = Utils.toHashMap(strAccountNoDetails);
				String strLoanID =  hmAccountNoDetails.get("id");
				String strLoanTypeName = hmAccountNoDetails.get("type");
				String strLoan_Amount = hmAccountNoDetails.get("amount");

				hmRVal = CBSAPI.loanMiniStatement(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
						strStatementType, intMaxNumberOfTransactions, strStartDate, strEndDate, strLoanID);
			}else{
				strMSG  = "Dear member, your account mini-statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";

				String strAccountNoDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT.name());
				HashMap <String, String> hmAccountNoDetails  = Utils.toHashMap(strAccountNoDetails);
				String strAccountName =  hmAccountNoDetails.get("name");
				String strAccountNumber = hmAccountNoDetails.get("number");
				String strAccountTypeName = hmAccountNoDetails.get("type_name");

				String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

				hmRVal = CBSAPI.accountMiniStatement(theUSSDRequest.getUSSDTraceID(), strTransactionID,"MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
						strStatementType, intMaxNumberOfTransactions, strStartDate, strEndDate, strAccountType, strAccountNumber);
			}

			HashMap<String, HashMap <String, String>>  transactions = (HashMap<String, HashMap <String, String>>) hmRVal.get("transactions");
			HashMap<String, String>  hmRequestDetails = (HashMap<String, String>) hmRVal.get("request_details");

			int intMaxNumberRows = 5;

			String strAccountMiniStatementStatus = hmRequestDetails.get("request_status");

			switch (strAccountMiniStatementStatus){
				case "SUCCESS":{
					try {
						String strMSGRequestStatus = hmRequestDetails.get("request_status");
						String strMSGAccountType = hmRequestDetails.get("account_type");
						String strMSGAccountName = hmRequestDetails.get("account_name");
						String strMSGAccountNumber = hmRequestDetails.get("account_number");
						String strMSGAvailableBalance = hmRequestDetails.get("available_balance");
						String strMSGAccountTypeName = hmRequestDetails.get("account_type_name");
						String strAccountTypeClass = hmRequestDetails.get("account_type_class");

						if(transactions.size() > 0){

							String strMSGMiniStatement  = "";
							for (String index : transactions.keySet()) {
								HashMap<String, String> hmAccount = transactions.get(index);

								String strMSGTransactionDateTime = hmAccount.get("transaction_date_time");
								String strMSGTransactionReference = hmAccount.get("transaction_reference");
								String strMSGTransactionDate = hmAccount.get("transaction_date");
								String strMSGTransactionTime = hmAccount.get("transaction_time");
								String strMSGTransactionAmount =  hmAccount.get("transaction_amount");
								String strMSGRunningBalance =  hmAccount.get("running_balance");
								String strMSGTransactionDescription =  hmAccount.get("transaction_description");

								String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,###.##");
								String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,###.##");

								String strMSGFormattedTransactionDateTime  = Utils.formatDate(strMSGTransactionDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

								strMSGMiniStatement  =  strMSGMiniStatement + "Ref:" + strMSGTransactionReference+ "\n";
								strMSGMiniStatement  =  strMSGMiniStatement + "Date:" + strMSGFormattedTransactionDateTime+ "\n";
								strMSGMiniStatement  =  strMSGMiniStatement + "Amnt:" + strMSGFormattedTransactionAmount+ "\n";
								strMSGMiniStatement  =  strMSGMiniStatement + "Run Bal:" + strMSGFormattedRunningBalance+ "\n\n";
								//strMSGMiniStatement = strMSGMiniStatement + "---";
							}
							strMSG  = "Dear member, your " + strMSGAccountTypeName + " Mini-Statement:\n" + strMSGMiniStatement;
						}else{
							strMSG = "Dear member, your Mini-Statement request has FAILED. No transaction(s) found. Please contact the SACCO if you have any active from " + strMSGAccountTypeName + " accounts";
						}

					}catch (Exception e){
						strMSG  = "Dear member, your Mini-Statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					}

					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "INCORRECT_PIN":{
					strMSG  = "Dear member, your Mini-Statement request has FAILED due to INCORRECT PIN. Please try again later. Kindly contact the SACCO if the problem persists.";

					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					strMSG  = "Dear member, your Mini-Statement request has FAILED due to INVALID ACCOUNT. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					strMSG  = "Dear member, your Mini-Statement request has FAILED due to INSUFFICIENT BALANCE. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "BLOCKED":{
					strMSG  = "Dear member, your Mini-Statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					strMSG  = "Dear member, your Mini-Statement request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}

		} catch (Exception e) {
			strMSG  = "Dear member, your Mini-Statement enquiry request has FAILED. Please try again later. Kindly contact the SACCO if the problem persists.";
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}finally {
			try {
				/*
				String strOriginatorID = UUID.randomUUID().toString().toLowerCase();
				String strSourceReference = strOriginatorID;
				String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
				MSGAPI.sendMSG(strOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
						"MBANKING_SERVER", 210, "MINI_STATEMENT", MSGConstants.Sensitivity.NORMAL, theUSSDRequest.getUSSDTraceID(), strSourceReference);
				*/
			}catch (Exception e){

			}
		}

		return rVal;
	}

	//todo DONE

	public USSDAPIConstants.TransactionReturnVal mobileMoneyWithdrawal(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType, HashMap<String, String> theResponse) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMobileNumberTo = strMobileNumberFrom;

            String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPriority = 200;

			String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_OPTION.name());
			String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

			System.out.println("strOption:"+strOption);
			System.out.println("strToOption:"+strToOption);

//			APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel(strOption);
			APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel("M-PESA");
			if(withdrawalChannel != null) {
				System.out.println("strToOption 1");

				if (withdrawalChannel.hasWithdrawalToOtherNumberEnabled()) {
					System.out.println("strToOption 2:");

					if(strToOption != null) {
						System.out.println("strToOption 3:");

						if(strToOption.equalsIgnoreCase("OTHER_NUMBER")){
							System.out.println("strToOption 4:");
							strMobileNumberTo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
							System.out.println("OTHER_NUMBER 1");
						}
					}
				}
			}

			strMobileNumberTo = APIUtils.sanitizePhoneNumber(strMobileNumberTo);

			String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
			HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
			String strSourceAccountNo = hmAccountDetails.get("number");
			String strSourceAccountName = hmAccountDetails.get("name");
			String strSourceAccountTypeName = hmAccountDetails.get("type_name");
			String strSourceAccountLabel = hmAccountDetails.get("label");

			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "MOBILE_MONEY_WITHDRAWAL";
			String strAPICategory = "MPESA_WITHDRAWAL";

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			String strSourceName = strSourceAccountName;
			String strReceiverName = strSourceAccountName;
			String strBeneficiaryName = strSourceAccountName;

			if (strToOption != null) {
				System.out.println("here 1");
				if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
					System.out.println("here 2");
					strSourceName = strMobileNumberTo;
					strReceiverName = strMobileNumberTo;
					strBeneficiaryName = strMobileNumberTo;
				}
			}

			System.out.println("mobile no to:"+ strMobileNumberTo);

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAType(thePESAType);
			pesa.setCategory(strCategory);
			pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
            pesa.setPESAStatusDescription("New PESA");
			pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumberFrom);
            pesa.setInitiatorAccount(strMobileNumberFrom);
            pesa.setInitiatorName(strSourceName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("ACCOUNT_NO");
			pesa.setSourceIdentifier(strSourceAccountNo);
			pesa.setSourceAccount(strSourceAccountNo);
			pesa.setSourceName(strSourceName);
            pesa.setSourceReference(strTransactionID);
            pesa.setSourceApplication("CBS");
            pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("SHORT_CODE");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			pesa.setSenderOtherDetails("<DATA/>");

			pesa.setReceiverType("MSISDN");
			pesa.setReceiverIdentifier(strMobileNumberTo);
			pesa.setReceiverAccount(strMobileNumberTo);
			pesa.setReceiverName(strReceiverName);
			pesa.setReceiverOtherDetails("<DATA/>");

			pesa.setBeneficiaryType("MSISDN");
			pesa.setBeneficiaryIdentifier(strMobileNumberTo);
			pesa.setBeneficiaryAccount(strMobileNumberTo);
			pesa.setBeneficiaryName(strBeneficiaryName);
			pesa.setBeneficiaryOtherDetails("<DATA/>");

			//If Withdrawal name can be found
			String  strTransactionDescription = "Cash Withdrawal by "+strMobileNumberFrom+" - "+strSourceAccountName+ " to "+strMobileNumberTo;
			pesa.setTransactionRemark(strTransactionDescription);
			pesa.setTransactionCurrency("KES");
			pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2C);
			pesa.setCommand("BusinessPayment");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("MBANKING_SERVER");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
			pesa.setPesaDateScheduled(strDateTime);
			pesa.setPesaDateCreated(strDateTime);
			pesa.setLocalDateCreated(strDateTime);

			HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID,
					pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
					pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
					pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
					strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, theUSSDRequest.getUSSDTraceID(), "MBANKING_SERVER", "USSD", strDateTime);

			String strTransactionStatus = hmRVal.get("transaction_status");
			String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
			String strTransactionDateTime = hmRVal.get("transaction_date_time");

//			System.out.println();

			switch (strTransactionStatus) {
				case "SUCCESS": {
					String strMSG = "";

					String strFormattedAmount = Utils.formatDouble(strAmount, "#,###.##");
					String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

					if(PESAProcessor.sendPESA(pesa) > 0){
						strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
						//send the receiving party an sms
						/*if (strWithdrawalToOption.equalsIgnoreCase("OTHER_NUMBER")) {
							Navision.getPort().sendSms(23, strMemberName+" has sent you "+strAmount+" ");
						}*/
					} else {
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;

						HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
								pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
								"", strDateTime);

						String strResultTransactionStatus = hmRValResult.get("transaction_status");
						String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
						String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

						if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
							strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
						}else{
							strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
						}
					}

					/*
					String strMSGOriginatorID = UUID.randomUUID().toString().toLowerCase();
					String strMSGRequestCorrelationID =  strOriginatorID;
					String strSourceReference = strTransactionID;
					String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
					MSGAPI.sendMSG(strMSGOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
							"MBANKING_SERVER", 210, strCategory, MSGConstants.Sensitivity.NORMAL, strMSGRequestCorrelationID, strSourceReference);
					*/
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "WITHDRAWAL_LIMIT_VIOLATION":{
					rVal = USSDAPIConstants.TransactionReturnVal.WITHDRAWAL_LIMIT_VIOLATION;
					theResponse.put("WITHDRAWAL_LIMIT_VIOLATION",strTransactionStatusDescription);
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo NOT DONE
	public USSDAPIConstants.TransactionReturnVal atmCashWithdrawal(USSDRequest theUSSDRequest) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMobileNumberToReceiveSMS = strMobileNumber;
			long lnMobileNumber = Long.parseLong(strMobileNumber);

			String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_OPTION.name());
			String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

			APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel(strOption);
			if(withdrawalChannel != null) {
				if (withdrawalChannel.hasWithdrawalToOtherNumberEnabled()) {
					if(strToOption != null) {
						if(strToOption.equalsIgnoreCase("OTHER_NUMBER")){
							strMobileNumberToReceiveSMS = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
						}
					}
				}
			}

			strMobileNumberToReceiveSMS = APIUtils.sanitizePhoneNumber(strMobileNumberToReceiveSMS);
			long lnMobileNumberToReceiveSMS = Long.parseLong(strMobileNumberToReceiveSMS);

			long lnSessionID = theUSSDRequest.getUSSDSessionID();

			String strGUID = MBankingDB.getDB_GUID().toUpperCase().trim();

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
			long lnAmount = Long.parseLong(strAmount);
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());
			Crypto crypto = new Crypto();
			strPIN = crypto.hash("MD5", strPIN);
			
			String strTransaction = "Withdrawal Request";

			//String strMemberNames = Navision.getPort().getMemberName(strMobileNumber);
			//String strTransactionDescription = "ATM Cash Withdrawal by " + strMobileNumber + " - " + strMemberNames + " to " + strMobileNumber;
			//String strWithdrawalStatus = Navision.getPort().insertMpesaTransaction(strGUID, strUSSDSessionID, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumber, strPIN, "USSD", strUSSDSessionID, "MBANKING");

			//System.out.println("Withdrawal Status: " + strWithdrawalStatus);
			//String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");
			//String strMemberName = arrWithdrawalStatus[1].split(" ")[0];

			String strWithdrawalStatus = "SUCCESS";
			String strMemberName = "PETER JOHN";
			String strTransactionDescription = "ATM Cash Withdrawal by " + strMobileNumber + " - " + strMemberName + " to " + strMobileNumber;

			switch (strWithdrawalStatus) {
				case "SUCCESS": {

					String strSMS = "SUCCESS";
					//todo: implement ATM gateway call here
					if (strSMS.equalsIgnoreCase("ERROR")) {
						//todo: Implement ATM reversal here
						//Navision.getPort().reverseWithdrawalRequest(strGUID);
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
					} else {
						sendSMS(strMobileNumberToReceiveSMS, strSMS, MSGConstants.MSGMode.EXPRESS, 200, "ATM_WITHDRAWAL_CODE", theUSSDRequest);
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					}
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo DONE
	public USSDAPIConstants.TransactionReturnVal airtimePurchase(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType, HashMap<String, String> theResponse) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {

			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMobileNumberTo = strMobileNumberFrom;

			String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPriority = 200;

			String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
			HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
			String strSourceAccountNo = hmAccountDetails.get("number");
			String strSourceAccountName = hmAccountDetails.get("name");
			String strSourceAccountTypeName = hmAccountDetails.get("type_name");
			String strSourceAccountLabel = hmAccountDetails.get("label");

			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_PIN.name());

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.AIRTIME);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "AIRTIME_PURCHASE";
			String strAPICategory = "AIRTIME_PURCHASE";

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			String strSourceName = strSourceAccountName;
			String strReceiverName = strSourceAccountName;
			String strBeneficiaryName = strSourceAccountName;

			String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());


			if (strToOption != null) {
				if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
					strSourceName = strMobileNumberTo;
					strReceiverName = strMobileNumberTo;
					strBeneficiaryName = strMobileNumberTo;
				}
			}

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAType(thePESAType);
			pesa.setCategory(strCategory);
			pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
			pesa.setPESAStatusDescription("New PESA");
			pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumberFrom);
            pesa.setInitiatorAccount(strMobileNumberFrom);
            pesa.setInitiatorName(strSourceName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("ACCOUNT_NO");
			pesa.setSourceIdentifier(strSourceAccountNo);
			pesa.setSourceAccount(strSourceAccountNo);
			pesa.setSourceName(strSourceName);
            pesa.setSourceReference(strTransactionID);
            pesa.setSourceApplication("CBS");
			pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("SKY_CODE");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			pesa.setSenderOtherDetails("<DATA/>");

			pesa.setReceiverType("MSISDN");
			pesa.setReceiverIdentifier(strMobileNumberFrom);
			pesa.setReceiverAccount(strMobileNumberFrom);
			pesa.setReceiverName(strReceiverName);
			pesa.setReceiverOtherDetails("<DATA/>");

			pesa.setBeneficiaryType("MSISDN");
			pesa.setBeneficiaryIdentifier(strMobileNumberTo);
			pesa.setBeneficiaryAccount(strMobileNumberTo);
			pesa.setBeneficiaryName(strBeneficiaryName);
			pesa.setBeneficiaryOtherDetails("<DATA/>");

			String  strTransactionDescription = "Airtime Purchase by "+strMobileNumberFrom+" - "+strSourceAccountName+ " to "+strMobileNumberTo;

            pesa.setTransactionRemark(strTransactionDescription);
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2C);
			pesa.setCommand("E-TOPUP");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("MBANKING_SERVER");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDateTime);
            pesa.setPesaDateCreated(strDateTime);
            pesa.setLocalDateCreated(strDateTime);

			HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID,
					pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
					pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
					pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
					strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, theUSSDRequest.getUSSDTraceID(), "MBANKING_SERVER", "USSD", strDateTime);

			String strTransactionStatus = hmRVal.get("transaction_status");
			String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
			String strTransactionDateTime = hmRVal.get("transaction_date_time");

			switch (strTransactionStatus) {
				case "SUCCESS": {
					String strMSG = "";

					String strFormattedAmount = Utils.formatDouble(strAmount, "#,###.##");
					String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

					if(PESAProcessor.sendPESA(pesa) > 0){
						strMSG = "Dear member, your Airtime Purchase request of KES " + strFormattedAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
						//send the receiving party an sms
						/*if (strWithdrawalToOption.equalsIgnoreCase("OTHER_NUMBER")) {
							Navision.getPort().sendSms(23, strMemberName+" has sent you "+strAmount+" ");
						}*/
					} else {
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;

						HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
								pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
								"", strDateTime);

						String strResultTransactionStatus = hmRValResult.get("transaction_status");
						String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
						String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

						if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
							strMSG = "Dear member, your Airtime Purchase request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
						}else{
							strMSG = "Dear member, your Airtime Purchase request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
						}
					}
					/*
					String strMSGOriginatorID = UUID.randomUUID().toString().toLowerCase();
					String strMSGRequestCorrelationID =  strOriginatorID;
					String strSourceReference = strTransactionID;
					String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
					MSGAPI.sendMSG(strMSGOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
							"MBANKING_SERVER", 210, strCategory, MSGConstants.Sensitivity.NORMAL, strMSGRequestCorrelationID, strSourceReference);
					*/
					theResponse.put("SUCCESS","SUCCESS");
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					theResponse.put("INCORRECT_PIN","INCORRECT_PIN");
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					theResponse.put("INVALID_ACCOUNT","INVALID_ACCOUNT");
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					theResponse.put("INSUFFICIENT_BAL","INSUFFICIENT_BAL");
					break;
				}
				case "WITHDRAWAL_LIMIT_VIOLATION":{
					rVal = USSDAPIConstants.TransactionReturnVal.WITHDRAWAL_LIMIT_VIOLATION;
					theResponse.put("WITHDRAWAL_LIMIT_VIOLATION",strTransactionStatusDescription);
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					theResponse.put("ACCOUNT_NOT_ACTIVE","ACCOUNT_NOT_ACTIVE");
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					theResponse.put("BLOCKED","BLOCKED");
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
					theResponse.put("ERROR","ERROR");
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING
	public USSDAPIConstants.TransactionReturnVal payBill(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType, HashMap<String, String> theResponse) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strMobileNumberTo = strMobileNumberFrom;

			String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPriority = 200;

			String strBillAccountNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());
			HashMap<String, String> hmAccount = Utils.toHashMap(strBillAccountNumberHashMap);
			String strBillerAccount = hmAccount.get("ACCOUNT_IDENTIFIER");

			String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
			HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
			String strSourceAccountNo = hmAccountDetails.get("number");
			String strSourceAccountName = hmAccountDetails.get("name");
			String strSourceAccountTypeName = hmAccountDetails.get("type_name");
			String strSourceAccountLabel = hmAccountDetails.get("label");

			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_PIN.name());

			String strUtilityProviderAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

			HashMap<String, String> hmUtilityAccountDetails = Utils.toHashMap(strUtilityProviderAccountDetails);

			String strToSPProviderAccountCode = hmUtilityAccountDetails.get("code");
			String strToAccountIdentifier = hmUtilityAccountDetails.get("identifier");
			String strToAccountType = hmUtilityAccountDetails.get("type");
			String strToAccountNaming = hmUtilityAccountDetails.get("type_tag");
			String strBillerName = hmUtilityAccountDetails.get("long_tag");

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "BILL_PAYMENT";
			String strAPICategory = "BILL_PAYMENT";

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			String strSourceName = strSourceAccountName;
			String strReceiverIdentifier = strToAccountIdentifier;
			String strReceiverAccount = strToAccountIdentifier;
			String strReceiverName = strBillerName;
			String strBeneficiaryName = strSourceAccountName;

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAType(thePESAType);
			pesa.setCategory(strCategory);
			pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
			pesa.setPESAStatusDescription("New PESA");
			pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumberFrom);
            pesa.setInitiatorAccount(strMobileNumberFrom);
            pesa.setInitiatorName(strSourceName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("ACCOUNT_NO");
			pesa.setSourceIdentifier(strSourceAccountNo);
			pesa.setSourceAccount(strSourceAccountNo);
			pesa.setSourceName(strSourceName);
            pesa.setSourceReference(strTransactionID);
            pesa.setSourceApplication("CBS");
			pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("SHORT_CODE");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			pesa.setSenderOtherDetails("<DATA/>");

			pesa.setReceiverType("SHORT_CODE");
			pesa.setReceiverIdentifier(strReceiverIdentifier);
			pesa.setReceiverAccount(strReceiverAccount);
			pesa.setReceiverName(strReceiverName);
			pesa.setReceiverOtherDetails("<DATA/>");

			pesa.setBeneficiaryType("MSISDN");
			pesa.setBeneficiaryIdentifier(strMobileNumber);
			pesa.setBeneficiaryAccount(strBillerAccount);
			pesa.setBeneficiaryName(strBeneficiaryName);
			pesa.setBeneficiaryOtherDetails("<DATA/>");


			String  strTransactionDescription = "B2B Bill Payment by "+strMobileNumberFrom+" - "+strSourceAccountName+ " to "+strBillerName + " - " + strBillerAccount;
            pesa.setTransactionRemark(strTransactionDescription);
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2B);
			pesa.setCommand("BusinessPayBill");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("MBANKING_SERVER");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDateTime);
            pesa.setPesaDateCreated(strDateTime);
            pesa.setLocalDateCreated(strDateTime);

			HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID,
					pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
					pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
					pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
					strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, theUSSDRequest.getUSSDTraceID(), "MBANKING_SERVER", "USSD", strDateTime);

			String strTransactionStatus = hmRVal.get("transaction_status");
			String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
			String strTransactionDateTime = hmRVal.get("transaction_date_time");


			switch (strTransactionStatus) {
				case "SUCCESS": {
					String strMSG = "";

					String strFormattedAmount = Utils.formatDouble(strAmount, "#,###.##");
					String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

					if(PESAProcessor.sendPESA(pesa) > 0){
						strMSG = "Dear member, your Bill Payment request of KES " + strFormattedAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
						//send the receiving party an sms
						/*if (strWithdrawalToOption.equalsIgnoreCase("OTHER_NUMBER")) {
							Navision.getPort().sendSms(23, strMemberName+" has sent you "+strAmount+" ");
						}*/
					} else {
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;

						HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
								pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
								"", strDateTime);

						String strResultTransactionStatus = hmRValResult.get("transaction_status");
						String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
						String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

						if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
							strMSG = "Dear member, your Bill Payment request of KES " + strFormattedAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
						}else{
							strMSG = "Dear member, your Bill Payment request of KES " + strFormattedAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
						}
					}

					/*
					String strMSGOriginatorID = UUID.randomUUID().toString().toLowerCase();
					String strMSGRequestCorrelationID =  strOriginatorID;
					String strSourceReference = strTransactionID;
					String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
					MSGAPI.sendMSG(strMSGOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
							"MBANKING_SERVER", 210, strCategory, MSGConstants.Sensitivity.NORMAL, strMSGRequestCorrelationID, strSourceReference);
					*/
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "WITHDRAWAL_LIMIT_VIOLATION":{
					rVal = USSDAPIConstants.TransactionReturnVal.WITHDRAWAL_LIMIT_VIOLATION;
					theResponse.put("WITHDRAWAL_LIMIT_VIOLATION",strTransactionStatusDescription);
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo DONE
	public USSDAPIConstants.TransactionReturnVal bankTransferViaB2B(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType,HashMap<String, String> theResponse) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPriority = 200;

			String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
			HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
			String strSourceAccountNo = hmAccountDetails.get("number");
			String strSourceAccountName = hmAccountDetails.get("name");
			String strSourceAccountTypeName = hmAccountDetails.get("type_name");
			String strSourceAccountLabel = hmAccountDetails.get("label");

			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

			String strToBankAccountNoHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

			HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoHashMap);
			String strBankAccountToName = hmAccount.get("ACCOUNT_NAME");
			String strBankAccountTo = hmAccount.get("ACCOUNT_IDENTIFIER");


			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.name());

			String strProviderBankAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
			HashMap<String, String> hmBankAccountDetails = Utils.toHashMap(strProviderBankAccountDetails);

			String strToSPProviderAccountCode = hmBankAccountDetails.get("code");
			String strToAccountIdentifier = hmBankAccountDetails.get("identifier");
			String strToAccountType = hmBankAccountDetails.get("type");
			String strToAccountNaming = hmBankAccountDetails.get("type_tag");
			String strToBankName = hmBankAccountDetails.get("long_tag");

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "BANK_TRANSFER";
			String strAPICategory = "BANK_TRANSFER";

			String strSourceName = strSourceAccountName;
			String strReceiverIdentifier = strToAccountIdentifier;
			String strReceiverAccount = strToAccountIdentifier;
			String strReceiverName = strToBankName;
			String strBeneficiaryName = strSourceAccountName;

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAType(thePESAType);
			pesa.setCategory(strCategory);
			pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
			pesa.setPESAStatusDescription("New PESA");
			pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            pesa.setInitiatorName(strSourceName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("ACCOUNT_NO");
			pesa.setSourceIdentifier(strSourceAccountNo);
			pesa.setSourceAccount(strSourceAccountNo);
			pesa.setSourceName(strSourceAccountName);
            pesa.setSourceReference(strTransactionID);
            pesa.setSourceApplication("CBS");
			pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("SHORT_CODE");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			pesa.setSenderOtherDetails("<DATA/>");

			pesa.setReceiverType("SHORT_CODE");
			pesa.setReceiverIdentifier(strReceiverIdentifier);
			pesa.setReceiverAccount(strBankAccountTo);
			pesa.setReceiverName(strReceiverName);
			pesa.setReceiverOtherDetails("<DATA/>");

			pesa.setBeneficiaryType("MSISDN");
			pesa.setBeneficiaryIdentifier(strMobileNumber);
			pesa.setBeneficiaryAccount(strBankAccountTo);
			pesa.setBeneficiaryName(strBankAccountToName);
			pesa.setBeneficiaryOtherDetails("<DATA/>");

			String strTransactionDescription = "B2B Bank transfer to "+strReceiverName+ " A/C "+strBankAccountTo;
            pesa.setTransactionRemark(strTransactionDescription);
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2B);
			pesa.setCommand("BusinessPayBill");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("CBS");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDateTime);
            pesa.setPesaDateCreated(strDateTime);
            pesa.setLocalDateCreated(strDateTime);

			HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID,
					pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
					pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
					pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
					strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, theUSSDRequest.getUSSDTraceID(), "MBANKING_SERVER", "USSD", strDateTime);

			String strTransactionStatus = hmRVal.get("transaction_status");
			String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
			String strTransactionDateTime = hmRVal.get("transaction_date_time");

			switch (strTransactionStatus) {
				case "SUCCESS": {
					String strMSG = "";
					String strFormattedAmount = Utils.formatDouble(strAmount, "#,###.##");
					String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

					if (PESAProcessor.sendPESA(pesa) > 0) {
						strMSG = "Dear member, your Bank Transfer request of KES " + strFormattedAmount + " to " + strToBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;;
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					} else {
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;

						HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
								pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
								"", strDateTime);

						String strResultTransactionStatus = hmRValResult.get("transaction_status");
						String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
						String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

						if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
							strMSG = "Dear member, your Bank Transfer request of KES " + strFormattedAmount + " to " + strToBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;;
						}else{
							strMSG = "Dear member, your Bank Transfer request of KES " + strFormattedAmount + " to " + strToBankName + " - " + pesa.getBeneficiaryIdentifier()  + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;;
						}
					}
					/*
					String strMSGOriginatorID = UUID.randomUUID().toString().toLowerCase();
					String strMSGRequestCorrelationID =  strOriginatorID;
					String strSourceReference = strTransactionID;
					String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
					MSGAPI.sendMSG(strMSGOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
							"MBANKING_SERVER", 210, strCategory, MSGConstants.Sensitivity.NORMAL, strMSGRequestCorrelationID, strSourceReference);
					*/
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "WITHDRAWAL_LIMIT_VIOLATION":{
					rVal = USSDAPIConstants.TransactionReturnVal.WITHDRAWAL_LIMIT_VIOLATION;
					theResponse.put("WITHDRAWAL_LIMIT_VIOLATION",strTransactionStatusDescription);
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo NOT DONE
	public USSDAPIConstants.TransactionReturnVal bankTransferViaPesalink(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

			String strDate = MBankingDB.getDBDateTime().trim();

			String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPriority = 200;

			String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());


			String strServiceProviderIdentifier = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

			String strToBankAccountNoHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

			HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoHashMap);
			String strReceiverBankAccountName = hmAccount.get("ACCOUNT_NAME");
			String strReceiverBankAccountNumber = hmAccount.get("ACCOUNT_IDENTIFIER");


			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.name());
			Crypto crypto = new Crypto();
			strPIN = crypto.hash("MD5", strPIN);
			

			LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE);

			String strBankOtherDetails = "";
			String strBankName = "";

			for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
				String strProviderIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
				if(strProviderIdentifier.equals(strServiceProviderIdentifier)){
					String strProviderBranchCode = serviceProviderAccount.getProviderBranchCode();
					strBankName = serviceProviderAccount.getProviderAccountLongTag();
					strBankOtherDetails = "<DATA><BANK_CODE>"+strProviderIdentifier+"</BANK_CODE><BRANCH_CODE>"+strProviderBranchCode+"</BRANCH_CODE></DATA>";
				}
			}

			String strTransaction = "Bank Transfer Request";

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.FAMILY_BANK_PESALINK);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "BANK_TRANSFER";

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAStatusCode(10);
			pesa.setPESAStatusDescription("New PESA");
            pesa.setPESAStatusName("QUEUED");
			pesa.setPESAStatusDate(strDate);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            //pesa.setInitiatorName(strSourceName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("MSISDN");
			pesa.setSourceIdentifier(strMobileNumber);
			pesa.setSourceAccount(strAccountFrom);
			//pesa.setSourceName(strSourceName);
            pesa.setSourceReference(strTransactionID);
            pesa.setSourceApplication("CBS");
			pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("ACCOUNT_NO");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			//TODO: Set this in XML
			pesa.setSenderOtherDetails(strBankOtherDetails);

			pesa.setReceiverType("ACCOUNT_NO");
			pesa.setReceiverIdentifier(strReceiverBankAccountNumber);
			pesa.setReceiverAccount(strReceiverBankAccountNumber);
			pesa.setReceiverName(strReceiverBankAccountName);
			//TODO: For test only, change later
			pesa.setReceiverOtherDetails(strBankOtherDetails);

			pesa.setBeneficiaryType("ACCOUNT_NO");
			pesa.setBeneficiaryIdentifier(strReceiverBankAccountNumber);
			pesa.setBeneficiaryAccount(strReceiverBankAccountNumber);
			pesa.setBeneficiaryName(strReceiverBankAccountName);
			pesa.setBeneficiaryOtherDetails(strBankOtherDetails);

			String strTransactionDescription = "PesaLink Bank transfer to "+strBankName+ " A/C "+strReceiverBankAccountNumber;
            pesa.setTransactionRemark(strTransactionDescription);
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2B);
			pesa.setCommand("PESALINK");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("CBS");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDate);
            pesa.setPesaDateCreated(strDate);
            pesa.setLocalDateCreated(strDate);

			//todo - Implement Integration to CBS
			//String strWithdrawalResponse = Navision.getPort().insertMpesaTransaction(strGUID, strUSSDSessionID, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumber, strPIN, "USSD", strUSSDSessionID, "MBANKING");

			//System.out.println("NAV Status: " + strWithdrawalResponse);
			//String[] arrWithdrawalResponse = strWithdrawalResponse.split("%&:");
			//String strWithdrawalStatus = arrWithdrawalResponse[0];
			//String strMemberName = arrWithdrawalResponse[1].trim();

			String strWithdrawalStatus = "SUCCESS";
			String strMemberName = "PETER JONES";

			switch (strWithdrawalStatus) {
				case "SUCCESS": {
					pesa.setSourceName(strMemberName);

					if (PESAProcessor.sendPESA(pesa) > 0) {
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					} else {
						//todo: Implement reversal;
						//Navision.getPort().reverseWithdrawalRequest(strGUID);
					}
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	public USSDAPIConstants.TransactionReturnVal MPESAFloatPurchase(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType, HashMap<String, String> theResponse) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

			String strDateTime = MBankingDB.getDBDateTime().trim();
			String strGUID = UUID.randomUUID().toString();
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTraceID = theUSSDRequest.getUSSDTraceID();
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			int intPESAPriority = 200;

			String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.name());
			HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
			String strSourceAccountNo = hmAccountDetails.get("number");
			String strSourceAccountName = hmAccountDetails.get("name");
			String strSourceAccountTypeName = hmAccountDetails.get("type_name");
			String strSourceAccountLabel = hmAccountDetails.get("label");

			String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_ACCOUNT.name()).trim();
			String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NO.name()).trim();
			String strAgentName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.name()).trim();
			String strStoreNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_STORE_NO.name()).trim();
			String strStoreName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AGENT_NAME.name()).trim();
			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_AMOUNT.name()).trim();


			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MPESA_FLOAT_PURCHASE_PIN.name());

			String memberFullName = strSourceAccountName;

			if(memberFullName == null || memberFullName.isEmpty()){
				memberFullName = strMobileNumber;
			}

			PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_FLOAT_PURCHASE);

			long getProductID = Long.parseLong(pesaParam.getProductId());
			String strCategory = "FLOAT_PURCHASE";
			//String strAPICategory = "MPESA_WITHDRAWAL";
			String strAPICategory = "FLOAT_PURCHASE";

			String strSenderIdentifier = pesaParam.getSenderIdentifier();
			String strSenderAccount = pesaParam.getSenderAccount();
			String strSenderName = pesaParam.getSenderName();

			PESA pesa = new PESA();

			pesa.setOriginatorID(strTransactionID);
			pesa.setProductID(getProductID);
			pesa.setPESAType(thePESAType);
			pesa.setCategory(strCategory);
			pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
			pesa.setPESAStatusDescription("New PESA");
			pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            pesa.setInitiatorName(memberFullName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

			pesa.setSourceType("ACCOUNT_NO");
			pesa.setSourceIdentifier(strSourceAccountNo);
			pesa.setSourceAccount(strSourceAccountNo);
			pesa.setSourceName(strSourceAccountName);
            pesa.setSourceApplication("CBS");
            pesa.setSourceReference(strTransactionID);
			pesa.setSourceOtherDetails("<DATA/>");

			pesa.setSenderType("SHORT_CODE");
			pesa.setSenderIdentifier(strSenderIdentifier);
			pesa.setSenderAccount(strSenderAccount);
			pesa.setSenderName(strSenderName);
			pesa.setSenderOtherDetails("<DATA/>");

			pesa.setReceiverType("TILL_NUMBER");
			pesa.setReceiverIdentifier(strAgentNumber);
			pesa.setReceiverAccount(strAgentNumber);
			pesa.setReceiverName(strAgentName);
			pesa.setReceiverOtherDetails("<DATA/>");

			pesa.setBeneficiaryType("TILL_NUMBER");
			pesa.setBeneficiaryIdentifier(strStoreNumber);
			pesa.setBeneficiaryAccount(strStoreNumber);
			pesa.setBeneficiaryName(strStoreName);
			pesa.setBeneficiaryOtherDetails("<DATA/>");

			String strTransactionDescription = "MPESA Float Purchase to "+strAgentName+ " Agent No. "+strAgentNumber+" - Store No. "+strStoreNumber;
            pesa.setTransactionRemark(strTransactionDescription);
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setBatchReference(strTransactionID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
			pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
			pesa.setPESAAction(PESAConstants.PESAAction.B2B);
			pesa.setCommand("BusinessDeposit");
			pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

			pesa.setCategory(strCategory);
			pesa.setPriority(intPESAPriority);
			pesa.setSendCount(0);
			pesa.setSourceApplication("CBS");
			pesa.setSourceReference(strTransactionID);
			pesa.setPESAXMLData("<OTHER_DETAILS/>");

			pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDateTime);
            pesa.setPesaDateCreated(strDateTime);
            pesa.setLocalDateCreated(strDateTime);

			HashMap<String,String> hmRVal = CBSAPI.mobileMoneyWithdrawal(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID,
					pesa.getSenderType(), pesa.getSenderIdentifier(), pesa.getSenderAccount(), pesa.getSenderName(), pesa.getSenderOtherDetails(),
					pesa.getReceiverType(), pesa.getReceiverIdentifier(), pesa.getReceiverAccount(), pesa.getReceiverName(), pesa.getReceiverOtherDetails(),
					pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(), pesa.getBeneficiaryAccount(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
					strSourceAccountNo, strAmount, strAPICategory, strTransactionDescription, theUSSDRequest.getUSSDTraceID(), "MBANKING_SERVER", "USSD", strDateTime);

			String strTransactionStatus = hmRVal.get("transaction_status");
			String strTransactionStatusDescription = hmRVal.get("transaction_status_description");
			String strTransactionDateTime = hmRVal.get("transaction_date_time");

			switch (strTransactionStatus) {
				case "SUCCESS": {
					String strMSG = "";

					String strFormattedAmount = Utils.formatDouble(strAmount, "#,###.##");
					String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-mm-dd HH:mm:ss","dd-MMM-yyyy HH:mm:ss");

					if(PESAProcessor.sendPESA(pesa) > 0){
						//strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been sent successfully.\nRef: " + strTransactionID;
						rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
						//send the receiving party an sms
						/*if (strWithdrawalToOption.equalsIgnoreCase("OTHER_NUMBER")) {
							Navision.getPort().sendSms(23, strMemberName+" has sent you "+strAmount+" ");
						}*/
					} else {
						rVal = USSDAPIConstants.TransactionReturnVal.ERROR;

						HashMap<String,String> hmRValResult = CBSAPI.mobileMoneyResult(pesa.getOriginatorID(), strTransactionID, PESAConstants.PESAResult.FAILED.getValue(),"Transaction FAILED to be queued on the database",
								pesa.getBeneficiaryType(),pesa.getBeneficiaryIdentifier(),pesa.getBeneficiaryName(), pesa.getBeneficiaryOtherDetails(),
								"", strDateTime);

						String strResultTransactionStatus = hmRValResult.get("transaction_status");
						String strResultTransactionStatusDescription = hmRValResult.get("transaction_status_description");
						String strResultTransactionStatusDateTime = hmRValResult.get("transaction_status_date_time");

						if(strResultTransactionStatus.equalsIgnoreCase("SUCCESS")){
							//strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " has been REVERSED. Dial *882# to check your balance.\nRef: " + strTransactionID;
						}else{
							//strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.\nRef: " + strTransactionID;
						}
					}

					/*
					String strMSGOriginatorID = UUID.randomUUID().toString().toLowerCase();
					String strMSGRequestCorrelationID =  strOriginatorID;
					String strSourceReference = strTransactionID;
					String strReceiver = String.valueOf(theUSSDRequest.getUSSDMobileNo());
					MSGAPI.sendMSG(strMSGOriginatorID, MSGConstants.MSGMode.SAF, "MSISDN", strReceiver, strMSG, "USSD",
							"MBANKING_SERVER", 210, strCategory, MSGConstants.Sensitivity.NORMAL, strMSGRequestCorrelationID, strSourceReference);
					*/
					break;
				}
				case "INCORRECT_PIN":{
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "INVALID_ACCOUNT":{
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				case "INSUFFICIENT_BAL":{
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "WITHDRAWAL_LIMIT_VIOLATION":{
					rVal = USSDAPIConstants.TransactionReturnVal.WITHDRAWAL_LIMIT_VIOLATION;
					theResponse.put("WITHDRAWAL_LIMIT_VIOLATION",strTransactionStatusDescription);
					break;
				}
				case "ACCOUNT_NOT_ACTIVE":{
					rVal = USSDAPIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
					break;
				}
				case "BLOCKED":{
					rVal = USSDAPIConstants.TransactionReturnVal.BLOCKED;
					break;
				}
				default:{
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo DONE
	public USSDAPIConstants.TransactionReturnVal checkLoanQualification(USSDRequest theUSSDRequest) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {

			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
			String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE.name());
			HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
			String strLoanTypeID = hmLoanType.get("id");
			String strLoanTypeCode = hmLoanType.get("code");
			String strLoanTypeName = hmLoanType.get("name");
			String strLoanTypeMaxAmount = hmLoanType.get("max");
			String strLoanTypeMinAmount = hmLoanType.get("min");
			String strLoanTypeMaxDuration = hmLoanType.get("duration");
			String strLoanTypeInterest = hmLoanType.get("interest");

			HashMap<String,String> hmRVal = CBSAPI.checkLoanQualification(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID, strLoanTypeName, strLoanTypeID);

			String strCheckLoanQualificationStatus = hmRVal.get("request_status");

			if (strCheckLoanQualificationStatus.equals("SUCCESS")) {
				rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	public USSDAPIConstants.TransactionReturnVal checkLoanGuarantorshipAbility(USSDRequest theUSSDRequest) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {

			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			HashMap<String,String> hmRVal = CBSAPI.checkLoanGuarantorshipAbility(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionID);

			String strCheckGuarantorshipAbility = hmRVal.get("request_status");

			if (strCheckGuarantorshipAbility.equals("SUCCESS")) {
				rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING
	public USSDAPIConstants.TransactionReturnVal loanApplication(USSDRequest theUSSDRequest) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {

			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PIN.name());

			String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
			HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
			String strLoanTypeID = hmLoanType.get("id");
			String strLoanTypeCode = hmLoanType.get("code");
			String strLoanTypeName = hmLoanType.get("name");
			String strLoanTypeMaxAmount = hmLoanType.get("max");
			String strLoanTypeMinAmount = hmLoanType.get("min");
			String strLoanTypeMaxDuration = hmLoanType.get("duration");
			String strLoanTypeInterest = hmLoanType.get("interest");

			String strTransactionReference = strTransactionID;
			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());
			String strSourceReference =  theUSSDRequest.getUSSDTraceID();
			String strRequestApplication = "MBANKING_SERVER";
			String strSourceApplication = "USSD";
			String strTransactionDateTime = strDateTime;

			HashMap<String,String> hmRVal = CBSAPI.loanApplication(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID, strTransactionReference,
					strLoanTypeID, strAmount, strSourceReference, strRequestApplication, strSourceApplication, strTransactionDateTime);
			String strLoanApplicationStatus = hmRVal.get("request_status");

			switch (strLoanApplicationStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "INCORRECT_PIN": {
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				case "LOAN_APPLICATION_EXISTS":{
					rVal = USSDAPIConstants.TransactionReturnVal.LOAN_APPLICATION_EXISTS;
					break;
				}
				default: {
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	//todo PENDING
	public HashMap<String, HashMap<String, String>> getLoans(USSDRequest theUSSDRequest) {
		HashMap<String, HashMap<String, String>> loans = new HashMap<>();
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			String strAccountBalanceEnquiryStatus = "ERROR";
			HashMap<String, Object> hmRVal =  CBSAPI.getLoansInService(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID);

			try{
				strAccountBalanceEnquiryStatus = (String) hmRVal.get("request_status");
				loans = (HashMap<String, HashMap<String, String>>) hmRVal.get("loans");
			}catch (Exception e){}

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}
		return loans;
	}

	//todo DONE
	public HashMap<String, HashMap <String, String>>  getLoanTypes(USSDRequest theUSSDRequest) {
		HashMap <String, String> loan_details = new HashMap<>();
		HashMap<String, HashMap <String, String>> loan_types = new HashMap<>();
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			HashMap<Object, Object> hmRVal = CBSAPI.getLoanTypes(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID);

			loan_details = (HashMap <String, String>) hmRVal.get("loan_details");

			if(loan_details!=null){
				if(loan_details.get("request_status").equalsIgnoreCase("SUCCESS")){
					loan_types = (HashMap<String, HashMap <String, String>>) hmRVal.get("loan_types");
				}
			}

		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}
		return loan_types;
	}

	//todo PENDING
	public USSDAPIConstants.TransactionReturnVal loanRepayment(USSDRequest theUSSDRequest){
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strFundsAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_FUNDS_ACCOUNT.name());
			HashMap<String, String> hmAccount = Utils.toHashMap(strFundsAccountDetails);
			String strFundsAccountNumber = hmAccount.get("number");
			String strFundsAccountName =  hmAccount.get("name");
			String strFundsAccountTypeName = hmAccount.get("type_name");
			String strFundsAccountLabel = hmAccount.get("label");
			String strFundsAccountAvailableBalance = hmAccount.get("avail_bal");

			String strLoanDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
			HashMap<String, String> hmLoan = Utils.toHashMap(strLoanDetails);
			String strLoanID = hmLoan.get("id");
			String strLoanTypeName = hmLoan.get("type");
			String strLoanAmount= hmLoan.get("amount");
			String strLoanBalance = hmLoan.get("balance");
			String strLoanAccountLabel= hmLoan.get("label");
			String strLoanInstallmentAmount= hmLoan.get("installment");


			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strTransactionReference = strTransactionID;
			String strSourceAccount = strFundsAccountNumber;
			String strDestinationAccount = strLoanID;
			String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());

			String strSourceReference = theUSSDRequest.getUSSDTraceID();
			String strTransactionDescription = "Loan Repayment Internal Funds Transfer. Source A/C: "+strSourceAccount+" - Destination A/C: "+strDestinationAccount;

			String strAction = "IFT_LOAN_REPAYMENT";

			HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTransactionID, "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
					strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strSourceReference,
					"MBANKING_SERVER", "USSD", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);
			String strRequestStatus = hmRVal.get("transaction_status");
			String strRequestStatusDescription = hmRVal.get("transaction_status_description");

			switch (strRequestStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "INSUFFICIENT_BAL": {
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "INCORRECT_PIN": {
					rVal = USSDAPIConstants.TransactionReturnVal.INCORRECT_PIN;
					break;
				}
				default: {
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		}catch (Exception e){
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
		}

		return rVal;
	}

	//todo PENDING
	public USSDAPIConstants.TransactionReturnVal fundsTransfer(USSDRequest theUSSDRequest) {
		USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
		try {
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN.name());

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
			String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());

			String strDestination = "ACCOUNT";

			if (strToOption != null) {
				if (strToOption.equals("ID Number")) {
					strDestination = "ID";
				} else {
					strDestination = "Mobile";
				}
			}

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strTransactionReference = strTransactionID;
			String strSourceAccount = strFromAccountNumber;
			String strDestinationAccount = strToAccountNumber;

			String strSourceReference = theUSSDRequest.getUSSDTraceID();
			String strTransactionDescription = "Internal Funds Transfer. Source A/C: "+strSourceAccount+" - Destination A/C: "+strDestinationAccount;

			String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

			HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTransactionID, "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID,
					strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strSourceReference,
					"MBANKING_SERVER", "USSD", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);
			String strRequestStatus = hmRVal.get("transaction_status");
			String strRequestStatusDescription = hmRVal.get("transaction_status_description");

			switch (strRequestStatus) {
				case "SUCCESS": {
					rVal = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "ERROR": {
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
					break;
				}
				case "INSUFFICIENT_BAL": {
					rVal = USSDAPIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
					break;
				}
				case "ACC_NOT_FOUND": {
					rVal = USSDAPIConstants.TransactionReturnVal.INVALID_ACCOUNT;
					break;
				}
				default: {
					rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
			e.printStackTrace();
		}

		return rVal;
	}

	//todo DONE
	public void sendSMS(String theMobileNo, String theMSG, MSGConstants.MSGMode theMode, int thePriority, String theCategory, USSDRequest theUSSDRequest){
		try {
			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
			String strTraceID = theUSSDRequest.getUSSDTraceID();
			fnSendSMS(theMobileNo, theMSG, "YES", theMode, thePriority, theCategory, "USSD", "MBANKING_SERVER", strTransactionID, strTraceID);
		} catch (Exception e){
			System.err.println("USSDAPI.sendSMS() ERROR : " + e.getMessage());
		}
	}

	//todo DONE
	public String generateAndSendOTP(String theMobileNo, String theSessionID, USSDRequest theUSSDRequest){
		String rVal = "";
		try {
			String strMAPPConfigXML = USSDLocalParameters.getClientXMLParameters();

			InputSource source = new InputSource(new StringReader(strMAPPConfigXML));
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document xmlDocument = builder.parse(source);
			XPath configXPath = XPathFactory.newInstance().newXPath();

			String strLength = configXPath.evaluate("/OTHER_DETAILS/CUSTOM_PARAMETERS/MAPP_ACTIVATION_CODE/@LENGTH", xmlDocument, XPathConstants.STRING).toString();
			String strTTL = configXPath.evaluate("/OTHER_DETAILS/CUSTOM_PARAMETERS/MAPP_ACTIVATION_CODE/@TTL", xmlDocument, XPathConstants.STRING).toString();

			int intLength = Integer.parseInt(strLength);
			long lnTTL = Integer.parseInt(strTTL);

			long lnTTLMinutes = lnTTL / 60;

			String strMobileAppStartKey = Utils.generateRandomString(intLength);

			InMemoryCache.store(theMobileNo+strMobileAppStartKey, strMobileAppStartKey, lnTTL);

			SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
			Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (lnTTL * 1000));

			String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
			String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


			String strMSG = "Dear Member,\n" + strMobileAppStartKey + " is your mobile app activation code generated at " + strTimeGenerated + ". This activation code is valid up to " + strExpiryDate + ".\n"+strAppID;

			String strCategory = "MAPP_ACTIVATION";

			sendSMS(theMobileNo, strMSG, MSGConstants.MSGMode.EXPRESS, 200, strCategory, theUSSDRequest);
			rVal = "Your " + intLength + " digit Mobile App Activation Code has been sent to you via SMS. Complete your Mobile App Activation within " + lnTTLMinutes + " minutes.";
		} catch (Exception e) {
			System.err.println("USSDAPI.generateAndSendOTP() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	public USSDAPIConstants.TransactionReturnVal deactivateMobileApp(USSDRequest theUSSDRequest){
		USSDAPIConstants.TransactionReturnVal rval = USSDAPIConstants.TransactionReturnVal.ERROR;
		try{
			String strDateTime = MBankingDB.getDBDateTime();
			String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

			String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

			String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

			HashMap<String,String> hmRVal = CBSAPI.deactivateMobileApp(theUSSDRequest.getUSSDTraceID(), "MSISDN", strMobileNumber, strPIN,"IMSI", strSIMID);
			String strDeactivationStatus = hmRVal.get("mobile_app_activation_status");
			String strDeactivationStatusDescription = hmRVal.get("mobile_app_activation_status_description");

			switch (strDeactivationStatus){
				case "SUCCESS": {
					rval = USSDAPIConstants.TransactionReturnVal.SUCCESS;
					break;
				}
				case "NOT_FOUND": {
					rval = USSDAPIConstants.TransactionReturnVal.ERROR;
					break;
				}
			}
		} catch (Exception e){
			System.err.println(this.getClass().getSimpleName()+".deactivateMobileApp() ERROR : " + e.getMessage());
		}

		return rval;
	}

	//todo DONE
	public USSDAmountLimitParam getAmountLimitCustomParameters(MBankingConstants.ApplicationType theApplicationType, USSDAPIConstants.USSD_PARAM_TYPE theUSSDParamType) {
		USSDAmountLimitParam rVal = new USSDAmountLimitParam();
		try {
			String strUSSDParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/AMOUNT_LIMITS";

			switch (theUSSDParamType) {
				case CASH_WITHDRAWAL: {
					strUSSDParamType += "/CASH_WITHDRAWAL";
					break;
				}
				case AIRTIME_PURCHASE: {
					strUSSDParamType += "/AIRTIME_PURCHASE";
					break;
				}
				case PAY_BILL: {
					strUSSDParamType += "/PAY_BILL";
					break;
				}
				case EXTERNAL_FUNDS_TRANSFER: {
					strUSSDParamType += "/EXTERNAL_FUNDS_TRANSFER";
					break;
				}
				case MPESA_FLOAT_PURCHASE: {
					strUSSDParamType += "/MPESA_FLOAT_PURCHASE";
					break;
				}
				case INTERNAL_FUNDS_TRANSFER: {
					strUSSDParamType += "/INTERNAL_FUNDS_TRANSFER";
					break;
				}
				case DEPOSIT: {
					strUSSDParamType += "/DEPOSIT";
					break;
				}
				case APPLY_LOAN: {
					strUSSDParamType += "/APPLY_LOAN";
					break;
				}
				case PAY_LOAN: {
					strUSSDParamType += "/PAY_LOAN";
					break;
				}
			}

			String strMinimum = MBankingAPI.getValueFromLocalParams(theApplicationType, strUSSDParamType + "/MIN_AMOUNT");
			String strMaximum = MBankingAPI.getValueFromLocalParams(theApplicationType, strUSSDParamType + "/MAX_AMOUNT");

			rVal.setMinimum(strMinimum);
			rVal.setMaximum(strMaximum);
		} catch (Exception e) {
			System.err.println("USSDAPI.getAmountLimitCustomParameters() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	public String getOtherCustomParameter(MBankingConstants.ApplicationType theApplicationType, String strUSSDCustomParametersRelativeXPath) {
		String rVal = "";
		try {
			String strUSSDCustomParametersXPath = "OTHER_DETAILS/CUSTOM_PARAMETERS/" + strUSSDCustomParametersRelativeXPath;
			rVal = MBankingAPI.getValueFromLocalParams(theApplicationType, strUSSDCustomParametersXPath);

		} catch (Exception e) {
			System.err.println("USSDAPI.getOtherCustomParameter() ERROR : " + e.getMessage());
		}
		return rVal;
	}

	//todo PENDING - The name of the function and what it does ...
	public boolean checkEmployerFunctionalityEnabled(USSDRequest theUSSDRequest, String theTransaction){
		boolean blCheckEmployerRestriction = false;
		try {
			String strUserPhoneNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
			//blCheckEmployerRestriction = Navision.getPort().employerRestriction(strUserPhoneNumber, theTransaction);
		} catch (Exception e){
			e.printStackTrace();
		}
		return blCheckEmployerRestriction;
	}

	public static String truncateString(String strValue, int expectedLength) {
		if (strValue != null && !strValue.isEmpty()) {
			strValue = strValue.trim();
			return strValue.substring(0, Math.min(strValue.length(), expectedLength));
		}
		return "";
	}

}
