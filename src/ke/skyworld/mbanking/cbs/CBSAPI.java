package ke.skyworld.mbanking.cbs;

import ke.skyworld.mbanking.mbankingapi.MBankingAPIUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class CBSAPI {

    public static HashMap<String,String> checkUser(String theTraceID, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier){

        /*
				REQUEST:
				{
				"action": "CHECK_USER",
					"payload": {
					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"identifier_type": "MSISDN",
					"identifier": "254712345678",
					"device_identifier_type": "IMSI",
					"device_identifier": "1099200912931023"
					}
				}

				RESPONSE:
				{
					"transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"transaction_status_date_time": "2021-04-07 19:34:07",
					"user_status": "ACTIVE",
					"auth_action": "NONE",
					"auth_action_valid_date": "",
					"login_flag": "",
					"login_attempts": 0,
					"otp_flag": "",
					"otp_attempts": "0"
				}
				 */

        HashMap<String,String> hmRVal = new HashMap<>();
        hmRVal.put("user_status", "ERROR");
        hmRVal.put("auth_action", "NONE");
        hmRVal.put("auth_action_valid_date", "");

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "CHECK_USER");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strCheckStatus = String.valueOf(jsonResponse.get("user_status"));
                    hmRVal.put("user_status", strCheckStatus);

                    if(strCheckStatus.equalsIgnoreCase("ACTIVE")){
                        String strAuthAction = String.valueOf(jsonResponse.get("auth_action"));
                        String strAuthActionValidDate = String.valueOf(jsonResponse.get("auth_action_valid_date"));

                        hmRVal.put("auth_action", strAuthAction);
                        hmRVal.put("auth_action_valid_date", strAuthActionValidDate);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("CBSAPI.checkUser(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("CBSAPI.checkUser(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> MOCheckUser(String theTraceID, String theIdentifierType, String theIdentifier){

        /*
				REQUEST:
                {
                  "action": "MO_CHECK_USER",
                  "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678"
                  }
                }

				RESPONSE:
                {
                  "user_status": "FOUND/NOT_FOUND"
                }
				 */

        HashMap<String,String> hmRVal = new HashMap<>();
        hmRVal.put("user_status", "NOT_FOUND");

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "MO_CHECK_USER");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strCheckStatus = jsonResponse.get("user_status").toString();
                    //String strAuthAction = jsonResponse.get("auth_action").toString();
                    //String strAuthActionValidDate = jsonResponse.get("auth_action_valid_date").toString();
                    hmRVal.put("user_status", strCheckStatus);
                    //hmRVal.put("auth_action", strAuthAction);
                    //hmRVal.put("auth_action_valid_date", strAuthActionValidDate);
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("CBSAPI.MOCheckUser(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            System.out.println("CBSAPI.MOCheckUser(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> userLogin(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier){

        /*
			REQUEST:
			{
				"action": "LOGIN",
				"payload": {
				  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
				  "identifier_type": "MSISDN",
				  "identifier": "254721913958",
				  "pin": "1234",
				  "device_identifier_type": "IMSI",
				  "device_identifier": "1099200912931023"
				}
			  }

			  RESPONSE:
				{
				   "transaction_destination_reference": "29003593-97e8-11eb-8044-000c299a0fc6",
				   "transaction_status_date_time": "2021-04-07 21:28:14",
				   "login_status": "INCORRECT_PIN",
				   "login_attempts": 0,
				   "auth_action_valid_date": "2021-04-07 21:28:14"
				}

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("login_status", "ERROR");
        hmRVal.put("login_attempts", "0");
        hmRVal.put("auth_action_valid_date", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "LOGIN");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strLoginStatus = String.valueOf(jsonResponse.get("login_status"));
                    hmRVal.put("login_status", strLoginStatus);

                    if(strLoginStatus.equalsIgnoreCase("ACTIVE")) {
                        String strLoginAttempts = String.valueOf(jsonResponse.get("login_attempts"));
                        String strAuthActionValidDate = String.valueOf(jsonResponse.get("auth_action_valid_date"));

                        hmRVal.put("login_attempts", strLoginAttempts);
                        hmRVal.put("auth_action_valid_date", strAuthActionValidDate);
                    }

                }catch (Exception e){
                    System.out.println("CBSAPI.userLogin(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.userLogin(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> setAuthSecurityParameters(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                                   String theAuthSecurityType, String theAuthAction, String theAuthActionValidDate, String theAuthFlag, String theDateTime){
        return setAuthSecurityParameters(theTraceID, theIdentifierType, theIdentifier, thePIN, theDeviceIdentifierType, theDeviceIdentifier,
                theAuthSecurityType, 0, theAuthAction, theAuthActionValidDate, theAuthFlag, theDateTime);
    }

    public static HashMap<String,String> setAuthSecurityParameters(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                                   String theAuthSecurityType, int authAttempts, String theAuthAction, String theAuthActionValidDate, String theAuthFlag, String theDateTime){

                        /*
                        REQUEST:
						  {
								"action": "SET_AUTH_SECURITY_PARAMETERS",
								"payload": {
								  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
								  "identifier_type": "MSISDN",
								  "identifier": "254721913958",
								  "pin": "1234",
								  "device_identifier_type": "IMSI",
								  "device_identifier": "1099200912931023",

								  "auth_security_type": "PASSWORD",
								  "auth_action": "",
								  "auth_action_valid_date": "2020-12-08 09:34:33",
								  "auth_flag": "NONE",
								  "auth_attempts": 0,
								  "date_time": "2020-12-08 09:34:33"
								}
							  }

							  RESPONSE:
							  {
								"set_auth_security_parameters_status": "SUCCESS ",
								"set_auth_security_parameters_status_description": "Auth Status Parameters set successfully",
								"date_time": "2020-12-08 09:34:33"
							}
						 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("set_auth_security_parameters_status", "ERROR");
        hmRVal.put("set_auth_security_parameters_status_description", "ERROR");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "SET_AUTH_SECURITY_PARAMETERS");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);

            jsonRequestPayload.put("auth_security_type", theAuthSecurityType);
            jsonRequestPayload.put("auth_action", theAuthAction);
            jsonRequestPayload.put("auth_action_valid_date", theAuthActionValidDate);
            jsonRequestPayload.put("auth_flag", theAuthFlag);
            if(theAuthSecurityType.equalsIgnoreCase("OTP")){jsonRequestPayload.put("auth_attempts", String.valueOf(authAttempts));}
            jsonRequestPayload.put("date_time", theDateTime);

            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strSetAuthSecurityParametersStatus = jsonResponse.get("set_auth_security_parameters_status").toString();
                    String strSetAuthSecurityParametersStatusDescription = jsonResponse.get("set_auth_security_parameters_status_description").toString();

                    hmRVal.put("set_auth_security_parameters_status", strSetAuthSecurityParametersStatus);
                    hmRVal.put("set_auth_security_parameters_status_description", strSetAuthSecurityParametersStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.setAuthSecurityParameters(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.setAuthSecurityParameters(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, String> getAuthSecurityParameters(String theTraceID, String theIdentifierType, String theIdentifier, String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theAuthSecurityType){

       /*
			REQUEST:
			  {
                "action": "GET_AUTH_SECURITY_PARAMETERS",
                "payload": {
                  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                  "identifier_type": "MSISDN",
                  "identifier": "254713000249",
                  "pin": "1111",
                  "device_identifier_type": "IMSI",
                  "device_identifier": "3750960482635630",
                  "auth_security_type": "PASSWORD"
                }
              }


			RESPONSE - POSITIVE RESPONSE:

            {
                "transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "transaction_status_date_time": "2021-07-25 20:37:44",
                "auth_action": "NONE",
                "auth_action_valid_date": "",
                "request_status": "SUCCESS",
                "auth_security_type": "PASSWORD",
                "auth_flag": "",
                "auth_attempts": "0"
            }
			*/

        HashMap<String, String> hmAuthSecurityParameters = new HashMap<>();
        hmAuthSecurityParameters.put("request_status","ERROR");

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_AUTH_SECURITY_PARAMETERS");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("auth_security_type", theAuthSecurityType);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = String.valueOf(jsonResponse.get("request_status"));
                    String strAuthAction = "";
                    String strAuthActionValidDate = "";
                    String strAuthSecurityType = "";
                    String strAuthFlag = "";
                    String strAuthAttempts = "";

                    if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                        strAuthAction = String.valueOf(jsonResponse.get("auth_action"));
                        strAuthActionValidDate = String.valueOf(jsonResponse.get("auth_action_valid_date"));
                        strAuthSecurityType = String.valueOf(jsonResponse.get("auth_security_type"));
                        strAuthFlag = String.valueOf(jsonResponse.get("auth_flag"));
                        strAuthAttempts = String.valueOf(jsonResponse.get("auth_attempts"));
                    }

                    hmAuthSecurityParameters.put("request_status",strRequestStatus);
                    hmAuthSecurityParameters.put("auth_action", strAuthAction);
                    hmAuthSecurityParameters.put("auth_action_valid_date", strAuthActionValidDate);
                    hmAuthSecurityParameters.put("auth_security_type", strAuthSecurityType);
                    hmAuthSecurityParameters.put("auth_flag", strAuthFlag);
                    hmAuthSecurityParameters.put("auth_attempts", strAuthAttempts);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            } else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.getAuthSecurityParameters(): " + e.getMessage());
        }

        return hmAuthSecurityParameters;
    }

    public static HashMap<String,String> setUserPIN(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                    String theNewPIN, String theIdentityType, String theIdentity){

        /*
			REQUEST:
			  {
					"action": "SET_PIN",
					"payload": {
					  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					  "identifier_type": "MSISDN",
				  	  "identifier": "254721913958",
					  "pin": "1234",
					  "device_identifier_type": "IMSI",
				  	  "device_identifier": "1099200912931023",
					  "new_pin": "4321",
					  "identity_type": "NATIONAL_ID",
					  "identity": "1234566"
					}
				  }
			 RESPONSE:
				{
				   "transaction_destination_reference": "99be9773-97ec-11eb-8044-000c299a0fc6",
				   "transaction_status_date_time": "2021-04-07 22:00:01",
				   "login_status": "SET_PIN",
				   "login_attempts": 0,
				   "set_pin_status": "SUCCESS",
				   "set_pin_status_description": "PIN set successfully"
				}

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("login_status", "ERROR");
        hmRVal.put("login_attempts", "0");
        hmRVal.put("set_pin_status", "ERROR");
        hmRVal.put("set_pin_status_description", "ERROR");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "SET_PIN");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("new_pin", theNewPIN);
            jsonRequestPayload.put("identity_type", theIdentityType);
            jsonRequestPayload.put("identity", theIdentity);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strLoginStatus = jsonResponse.get("login_status").toString();
                    String strLoginAttempts = jsonResponse.get("login_attempts").toString();
                    String strSetPinStatus = jsonResponse.get("set_pin_status").toString();
                    String strSetPinStatusDescription= jsonResponse.get("set_pin_status_description").toString();

                    hmRVal.put("login_status", strLoginStatus);
                    hmRVal.put("login_attempts", strLoginAttempts);
                    hmRVal.put("set_pin_status", strSetPinStatus);
                    hmRVal.put("set_pin_status_description", strSetPinStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.setUserPIN(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.setUserPIN(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, String> activateMobileApp(String theTraceID, String theIdentifierType, String theIdentifier, String thePIN, String theAppID){

       /*
			REQUEST:
			    {
                    "action": "ACTIVATE_MOBILE_APP ",
                    "payload": {
                     "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                      "identifier_type": "MSISDN",
                      "identifier": "254712345678",
                      "pin": "1234",
                      "app_id": "1099200912931023",
                      "activate_with_kyc": "NO"
                    }
                  }


			RESPONSE - POSITIVE RESPONSE:

            {
                "transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "transaction_status_date_time": "2021-07-25 20:55:39",
                "login_status": "SUCCESS",
                "auth_action_valid_date": "",
                "login_flag": "",
                "login_attempts": 0,
                "otp_flag": "",
                "otp_attempts": "0",
                "auth_action": "NONE",
                "mobile_app_activation_status": "SUCCESS",
                "mobile_app_activation_status_description": "Mobile App Activated successfully"
            }
			*/

        HashMap<String, String> hmMobileAppActivation = new HashMap<>();
        hmMobileAppActivation.put("mobile_app_activation_status","ERROR");
        hmMobileAppActivation.put("mobile_app_activation_status_description","ERROR");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "ACTIVATE_MOBILE_APP");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("app_id", theAppID);
            jsonRequestPayload.put("activate_with_kyc", "NO");
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strMobileAppActivationStatus = String.valueOf(jsonResponse.get("mobile_app_activation_status"));
                    String strMobileAppActivationStatusDesc = String.valueOf(jsonResponse.get("mobile_app_activation_status_description"));
                    hmMobileAppActivation.put("mobile_app_activation_status",strMobileAppActivationStatus);
                    hmMobileAppActivation.put("mobile_app_activation_status_description",strMobileAppActivationStatusDesc);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            } else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.activateMobileApp(): " + e.getMessage());
        }

        return hmMobileAppActivation;
    }

    public static  HashMap<String, String> activateMobileAppWithKYC(String theTraceID, String theIdentifierType, String theIdentifier, String thePIN, String theAppID, String theIdentityType, String theIdentity){

       /*
			REQUEST:
			    {
                    "action": "ACTIVATE_MOBILE_APP ",
                    "payload": {
                     "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                      "identifier_type": "MSISDN",
                      "identifier": "254712345678",
                      "pin": "1234",
                      "app_id": "1099200912931023",
                      "activate_with_kyc": "YES/NO",
                      "identity_type": "NATIONAL_ID",
                      "identity": "23994857"
                    }
                  }


			RESPONSE - POSITIVE RESPONSE:

            {
                "transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "transaction_status_date_time": "2021-07-25 20:55:39",
                "login_status": "SUCCESS",
                "auth_action_valid_date": "",
                "login_flag": "",
                "login_attempts": 0,
                "otp_flag": "",
                "otp_attempts": "0",
                "auth_action": "NONE",
                "mobile_app_activation_status": "SUCCESS",
                "mobile_app_activation_status_description": "Mobile App Activated successfully"
            }
			*/

        HashMap<String, String> hmMobileAppActivation = new HashMap<>();
        hmMobileAppActivation.put("mobile_app_activation_status","ERROR");
        hmMobileAppActivation.put("mobile_app_activation_status_description","ERROR");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "ACTIVATE_MOBILE_APP");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("app_id", theAppID);
            jsonRequestPayload.put("activate_with_kyc", "YES");
            jsonRequestPayload.put("identity_type", theIdentityType);
            jsonRequestPayload.put("identity", theIdentity);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strMobileAppActivationStatus = String.valueOf(jsonResponse.get("mobile_app_activation_status"));
                    String strMobileAppActivationStatusDesc = String.valueOf(jsonResponse.get("mobile_app_activation_status_description"));
                    hmMobileAppActivation.put("mobile_app_activation_status",strMobileAppActivationStatus);
                    hmMobileAppActivation.put("mobile_app_activation_status_description",strMobileAppActivationStatusDesc);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            } else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.activateMobileAppWithKYC(): " + e.getMessage());
        }

        return hmMobileAppActivation;
    }

    public static HashMap<String,String> changeUserPIN(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theNewPIN){

        /*
			  REQUEST:
			  {
				"action": "CHANGE_PIN",
				"payload": {
				  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
				  "identifier_type": "MSISDN",
				  "identifier": "254721913958",
				  "pin": "1234",
				  "device_identifier_type": "IMSI",
				  "device_identifier": "1099200912931023",
				  "new_pin": "4321"
				}
			  }

			  RESPONSE:
			  {
				"change_pin_status": "SUCCESS/INVALID_ACCOUNT/INCORRECT_PIN/INVALID_NEW_PIN/ERROR",
				"change_pin_status_description": "PIN changed successfully"
				}
			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("change_pin_status", "ERROR");
        hmRVal.put("change_pin_status_description", "");

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "CHANGE_PIN");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("new_pin", theNewPIN);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strChangePinStatus = jsonResponse.get("set_pin_status").toString();
                    String strChangePinStatusDescription = jsonResponse.get("set_pin_status_description").toString();

                    hmRVal.put("change_pin_status", strChangePinStatus);
                    hmRVal.put("change_pin_status_description", strChangePinStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.changeUserPIN(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.changeUserPIN(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, Object>  accountBalanceEnquiry(String theTraceID, String theTransactionReference, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theAccountType){

       /*
			REQUEST:
			{
				"action": "ACCOUNT_BALANCE_ENQUIRY",
				"payload": {
					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"transaction_reference": "QMS039U05",
					"identifier_type": "MSISDN",
					"identifier": "254712345678",
					"pin": "1234",
					"device_identifier_type": "IMSI/APP_ID",
					"device_identifier": "1099200912931023",
					"account_type": "FOSA"
					}
				}

			RESPONSE:
                {
                   "transaction_destination_reference": "186ec3e5-a041-11eb-8044-000c299a0fc6",
                   "transaction_status_date_time": "2021-04-18 15:25:01",
                   "login_status": "SUCCESS",
                   "auth_action_valid_date": "",
                   "login_flag": "",
                   "login_attempts": 0,
                   "otp_flag": "",
                   "otp_attempts": "0",
                   "auth_action": "NONE",
                   "request_status": "SUCCESS",
                   "accounts": [
                      {
                         "account_name": "JAMES MUTHURI MBAABU",
                         "account_number": "5-04-14461-00",
                         "account_type": "FOSA",
                         "account_balance": 4688.88,
                         "account_type_class": "Savings Accounts",
                         "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                         "account_label": "CUSTOMERS(5-04-14461-00)"
                      }
                   ]
                }
			*/

        String strRequestStatus = "ERROR";

        HashMap<String, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_status","ERROR");
        hmRVal.put("accounts",null);

        HashMap<String, HashMap <String, String>> hmAccounts = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "ACCOUNT_BALANCE_ENQUIRY");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("account_type", theAccountType);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;


            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    strRequestStatus = jsonResponse.get("request_status").toString();
                    hmRVal.put("request_status",strRequestStatus);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmAccounts = new HashMap<>();
                JSONArray dataArray= jsonResponse.getJSONArray("accounts");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    /*
                     "account_name": "JAMES MUTHURI MBAABU",
                         "account_number": "5-04-14461-00",
                         "account_type": "FOSA",
                         "account_balance": 4688.88,
                         "account_type_class": "Savings Accounts",
                         "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                         "account_label": "CUSTOMERS(5-04-14461-00)"
                     */
                    String strAccountType =  object.getString("account_type");
                    String strAccountName = object.getString("account_name");
                    String strAccountNumber = object.getString("account_number");
                    String strAccountBalance= String.valueOf(object.getDouble("account_balance"));
                    String strAccountTypeClass = object.getString("account_type_class");
                    String strAccountTypeName = object.getString("account_type_name");
                    String strAccountLabel = object.getString("account_label");


                    HashMap<String, String> hmAccount = new HashMap<>();
                    hmAccount.put("account_type",strAccountType);
                    hmAccount.put("account_name",strAccountName);
                    hmAccount.put("account_number",strAccountNumber);
                    hmAccount.put("account_balance",strAccountBalance);
                    hmAccount.put("account_type_class",strAccountTypeClass);
                    hmAccount.put("account_type_name",strAccountTypeName);
                    hmAccount.put("account_label",strAccountLabel);

                    hmAccounts.put(strAccountNumber,hmAccount);
                }

                hmRVal.put("accounts",hmAccounts);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.accountBalanceEnquiry(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, Object>  singleAccountBalanceEnquiry(String theTraceID, String theTransactionReference, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theAccountType, String theAccountNumber){

       /*
			REQUEST:
			{
				"action": "SINGLE_ACCOUNT_BALANCE_ENQUIRY",
				"payload": {
					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"transaction_reference": "QMS039U05",
					"identifier_type": "MSISDN",
					"identifier": "254712345678",
					"pin": "1234",
					"device_identifier_type": "IMSI/APP_ID",
					"device_identifier": "1099200912931023",
					"account_number": "5-04-07879-00"
					}
				}

			RESPONSE:
                {
                    "transaction_destination_reference": "df3e7cf5-1e4b-41ef-a22f-66",
                    "transaction_status_date_time": "2021-08-28 23:12:38",
                    "login_status": "SUCCESS",
                    "auth_action_valid_date": "",
                    "login_flag": "",
                    "login_attempts": 0,
                    "otp_flag": "",
                    "otp_attempts": "0",
                    "auth_action": "NONE",
                    "request_status": "SUCCESS",
                    "request_status_description": "SUCCESS",
                    "default_fosa_acc": "5-02-00009-00",
                    "account_balance": {
                        "account_type": "FOSA",
                        "account_name": "ATIENO NUNDU JANE",
                        "account_label": "CUSTOMERS(5-04-07879-00)",
                        "account_number": "5-04-07879-00",
                        "account_balance": 120237.21
                    }
                }
			*/

        String strRequestStatus = "ERROR";

        HashMap<String, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_status","ERROR");
        hmRVal.put("accounts",null);

        HashMap<String, HashMap <String, String>> hmAccounts = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "SINGLE_ACCOUNT_BALANCE_ENQUIRY");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("account_type", theAccountType);
            jsonRequestPayload.put("account_number", theAccountNumber);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;


            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    strRequestStatus = jsonResponse.get("request_status").toString();
                    hmRVal.put("request_status",strRequestStatus);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){

                String strAccountType =  jsonResponse.getJSONObject("account_balance").getString("account_type");
                String strAccountName = jsonResponse.getJSONObject("account_balance").getString("account_name");
                String strAccountNumber = jsonResponse.getJSONObject("account_balance").getString("account_number");
                String strAccountBalance= String.valueOf(jsonResponse.getJSONObject("account_balance").getDouble("account_balance"));
                String strAccountLabel = jsonResponse.getJSONObject("account_balance").getString("account_label");

                hmRVal.put("account_type",strAccountType);
                hmRVal.put("account_name",strAccountName);
                hmRVal.put("account_number",strAccountNumber);
                hmRVal.put("account_balance",strAccountBalance);
                hmRVal.put("account_label",strAccountLabel);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.accountBalanceEnquiry(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, Object>  MOAccountBalanceEnquiry(String theRequestCorrelationID, String theSourceReference, String theIdentifierType, String theIdentifier){

       /*
			REQUEST:
              {
                "action": "MO_ACCOUNT_BALANCE_ENQUIRY",
                "payload": {
                 "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                  "identifier_type": "MSISDN",
                  "identifier": "254712345678",
                  "transaction_reference": "QMS039U05"
                }
              }


			RESPONSE:
              {
                "request_status": "SUCCESS/ERROR",
                "account_balances": [
                  {
                                     "account_name": "JAMES MUTHURI MBAABU",
                                     "account_number": "5-04-14461-00",
                                     "account_type": "FOSA",
                                     "account_balance": 4688.88,
                                     "account_type_class": "Savings Accounts",
                                     "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                                     "account_label": "CUSTOMERS(5-04-14461-00)"
                                  }
                ]
              }

			*/

        String strRequestStatus = "ERROR";

        HashMap<String, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_status","ERROR");
        hmRVal.put("accounts",null);

        HashMap<String, HashMap <String, String>> hmAccounts = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "MO_ACCOUNT_BALANCE_ENQUIRY");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theRequestCorrelationID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("transaction_reference", theSourceReference);

            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;


            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    strRequestStatus = jsonResponse.get("request_status").toString();
                    hmRVal.put("request_status",strRequestStatus);
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmAccounts = new HashMap<>();
                JSONArray dataArray= jsonResponse.getJSONArray("account_balances");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    /*
                     "account_name": "JAMES MUTHURI MBAABU",
                         "account_number": "5-04-14461-00",
                         "account_type": "FOSA",
                         "account_balance": 4688.88,
                         "account_type_class": "Savings Accounts",
                         "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                         "account_label": "CUSTOMERS(5-04-14461-00)"
                     */
                    String strAccountType =  object.getString("account_type");
                    String strAccountName = object.getString("account_name");
                    String strAccountNumber = object.getString("account_number");
                    String strAccountBalance= String.valueOf(object.getDouble("account_balance"));
                    String strAccountTypeClass = object.getString("account_type_class");
                    String strAccountTypeName = object.getString("account_type_name");
                    String strAccountLabel = object.getString("account_label");


                    HashMap<String, String> hmAccount = new HashMap<>();
                    hmAccount.put("account_type",strAccountType);
                    hmAccount.put("account_name",strAccountName);
                    hmAccount.put("account_number",strAccountNumber);
                    hmAccount.put("account_balance",strAccountBalance);
                    hmAccount.put("account_type_class",strAccountTypeClass);
                    hmAccount.put("account_type_name",strAccountTypeName);
                    hmAccount.put("account_label",strAccountLabel);

                    hmAccounts.put(strAccountNumber,hmAccount);
                }

                hmRVal.put("accounts",hmAccounts);
            }

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("CBSAPI.accountBalanceEnquiry(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<Object, Object>  accountMiniStatement(String theTraceID, String theTransactionReference, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                                 String theStatementType, int theMaxNumberOfTransactions, String theStartDate, String theEndDate, String theAccountType, String theAccountNumber){

       /*
			REQUEST:
			{
                "action": "ACCOUNT_STATEMENT",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "transaction_reference": "QMS039U05",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023",
                    "statement_type": "MINI_STATEMENT/FULL_STATEMENT",
                    "max_number_of_transactions": 100,
                    "start_date": "2021-01-01 20:00:00",
                    "end_date": "2021-01-03 20:00:00",
                    "account_type": "FOSA/BOSA",
                    "account_number": "41-02392-0093-01"
                 }
            }

			RESPONSE:
            {
               "transactions": [
                  {
                     "transaction_date_time": "2021-01-29 15:29:32",
                     "transaction_description": "WITHDRAWAL EXCISE DUTY",
                     "transaction_reference": "97671131178999_82:Duty",
                     "transaction_date": "29/01/2021",
                     "transaction_time": "15:29:32",
                     "transaction_amount": "-09.00",
                     "running_balance": "2,985.26"
                  },
                  {
                     "transaction_date_time": "2021-02-02 16:04:15",
                     "transaction_description": "WITHDRAWAL Withdrawal From 254725399767",
                     "transaction_reference": "97671166608976_24",
                     "transaction_date": "02/02/2021",
                     "transaction_time": "16:04:15",
                     "transaction_amount": "-2,100.00",
                     "running_balance": "885.26"
                  },
                  {
                     "transaction_date_time": "2021-02-02 16:04:15",
                     "transaction_description": " MPESA Charge: Withdrawal From 254725399767",
                     "transaction_reference": "97671166608976_24:Charge",
                     "transaction_date": "02/02/2021",
                     "transaction_time": "16:04:15",
                     "transaction_amount": "-22.40",
                     "running_balance": "862.86"
                  },
                  {
                     "transaction_date_time": "2021-02-02 16:04:15",
                     "transaction_description": "Commission: WITHDRAWAL",
                     "transaction_reference": "97671166608976_24:Comm",
                     "transaction_date": "02/02/2021",
                     "transaction_time": "16:04:15",
                     "transaction_amount": "-45.00",
                     "running_balance": "817.86"
                  },
                  {
                     "transaction_date_time": "2021-02-02 16:04:15",
                     "transaction_description": "WITHDRAWAL EXCISE DUTY",
                     "transaction_reference": "97671166608976_24:Duty",
                     "transaction_date": "02/02/2021",
                     "transaction_time": "16:04:15",
                     "transaction_amount": "-09.00",
                     "running_balance": "808.86"
                  }
               ],
               "account_type": "FOSA",
               "account_name": "JACKSON OCHIENG ONYANGO",
               "account_number": "5-04-05925-00",
               "available_balance": 308.86,
               "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
               "account_type_class": "Savings Accounts",
               "request_status": "SUCCESS"
            }
			*/

        HashMap<Object, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_details",null);
        hmRVal.put("transactions",null);

        HashMap<String, String> hmRequestDetails = new HashMap<>();
        hmRequestDetails.put("request_status","ERROR");

        LinkedHashMap<String, HashMap<String, String>> hmTransactions = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "ACCOUNT_STATEMENT");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("statement_type", theStatementType);
            jsonRequestPayload.put("max_number_of_transactions", String.valueOf(theMaxNumberOfTransactions));
            jsonRequestPayload.put("start_date", theStartDate);
            jsonRequestPayload.put("end_date", theEndDate);
            jsonRequestPayload.put("account_type", theAccountType);
            jsonRequestPayload.put("account_number", theAccountNumber);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus  = (String) jsonResponse.get("request_status");
                    String strAccountType  = (String)  jsonResponse.get("account_type");
                    String strAccountName  = (String)  jsonResponse.get("account_name");
                    String strAccountNumber  = (String)  jsonResponse.get("account_number");
                    String strAvailableBalance  = String.valueOf(jsonResponse.getDouble("available_balance"));
                    String strAccountTypeName  = (String)  jsonResponse.get("account_type_name");
                    String strAccountTypeClass  = (String)  jsonResponse.get("account_type_class");

                    hmRequestDetails.put("request_status",strRequestStatus);
                    hmRequestDetails.put("account_type",strAccountType);
                    hmRequestDetails.put("account_name",strAccountName);
                    hmRequestDetails.put("account_number",strAccountNumber);
                    hmRequestDetails.put("available_balance",strAvailableBalance);
                    hmRequestDetails.put("account_type_name",strAccountTypeName);
                    hmRequestDetails.put("account_type_class",strAccountTypeClass);

                    hmRVal.put("request_details",hmRequestDetails);
                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Error converting String to JSON");
                    System.out.println("JSON Request: "+strJSONRequest);
                    System.out.println("JSON Response: "+jsonResponse);
                }
            }else {
                System.out.println("Received NULL Response");
            }

            String strRequestStatus = hmRequestDetails.get("request_status");

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmTransactions = new LinkedHashMap<>();

                JSONArray dataArray= jsonResponse.getJSONArray("transactions");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    String strTransactionDateTime =  object.getString("transaction_date_time");
                    String strTransactionReference =  object.getString("transaction_reference");
                    String strTransactionDate = object.getString("transaction_date");
                    String strTransactionTime = object.getString("transaction_time");
                    String strTransactionAmount = object.getString("transaction_amount");
                    String strRunningBalance = object.getString("running_balance");
                    String strTransactionDescription = object.getString("transaction_description");

                    HashMap<String, String> hmTransaction = new HashMap<>();
                    hmTransaction.put("transaction_date_time",strTransactionDateTime);
                    hmTransaction.put("transaction_reference",strTransactionReference);
                    hmTransaction.put("transaction_date",strTransactionDate);
                    hmTransaction.put("transaction_time",strTransactionTime);
                    hmTransaction.put("transaction_amount",strTransactionAmount);
                    hmTransaction.put("running_balance",strRunningBalance);
                    hmTransaction.put("transaction_description",strTransactionDescription);
                    hmTransactions.put(Integer.toString(i),hmTransaction);
                }

                hmRVal.put("transactions",hmTransactions);
            }

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("CBSAPI.accountMiniStatement(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static LinkedHashMap<String, LinkedHashMap <String, String>> getBankAccounts(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theAccountType){

        /*
			REQUEST:
			{
				"action": "GET_ACCOUNTS",
				"payload": {
					"api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
					"account_type": "FOSA/BOSA/WITHDRAWABLE",
					"identifier_type": "MSISDN",
					"identifier": "254712345678",
					"pin": "1234",
					"device_identifier_type": "IMSI/APP_ID",
					"device_identifier": "1099200912931023"
					}
				}

				RESPONSE:
                {
                   "transaction_destination_reference": "e66ec8a6-a044-11eb-8044-000c299a0fc6",
                   "transaction_status_date_time": "2021-04-18 15:52:15",
                   "login_status": "SUCCESS",
                   "auth_action_valid_date": "",
                   "login_flag": "",
                   "login_attempts": 0,
                   "otp_flag": "",
                   "otp_attempts": "0",
                   "auth_action": "NONE",
                   "request_status": "SUCCESS",
                   "accounts": [
                      {
                         "account_status": "ACTIVE",
                         "account_active": "YES",
                         "account_name": "JAMES MUTHURI MBAABU",
                         "account_number": "5-04-14461-00",
                         "account_type": "FOSA",
                         "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                         "account_type_class": "Savings Accounts",
                         "account_available_balance": 4688.88,
                         "account_balance": 5188.88,
                         "account_label": "CUSTOMERS (5-04-14461-00)"
                      }
                   ],
                   "no_of_accounts": 1
                }
			 */

        LinkedHashMap<String, LinkedHashMap<String, String>> accounts = new LinkedHashMap<>();

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_ACCOUNTS");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("account_type", theAccountType);

            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            String strRequestStatus = "ERROR";
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    strRequestStatus = jsonResponse.get("request_status").toString();
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            accounts = new LinkedHashMap<>();
            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                JSONArray dataArray= jsonResponse.getJSONArray("accounts");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    String strMSGAccountNumber =  object.getString("account_number");
                    String strMSGAccountName = object.getString("account_name");
                    String strMSGAccountType = object.getString("account_type");
                    String strMSGAccountTypeName = object.getString("account_type_name");
                    String strMSGAccountLabel = object.getString("account_label");
                    String strMSGAccountAvailableBalance = String.valueOf(object.getDouble("account_available_balance"));

                    LinkedHashMap<String, String> account = new LinkedHashMap<>();
                    account.put("number",strMSGAccountNumber);
                    account.put("name",strMSGAccountName);
                    account.put("type",strMSGAccountType);
                    account.put("type_name",strMSGAccountTypeName);
                    account.put("label",strMSGAccountLabel);
                    account.put("avail_bal",strMSGAccountAvailableBalance);
                    accounts.put(strMSGAccountNumber,account);
                }
            }
            accounts = (accounts.size() > 0) ? accounts : null;
        }catch (Exception e){
            System.out.println("CBSAPI.getBankAccounts(): " + e.getMessage());
        }

        return accounts;
    }

    public static HashMap<String,String> mobileMoneyWithdrawal(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theTransactionReference,
                                                               String theSenderIdentifierType, String theSenderIdentifier,String theSenderAccount, String theSenderName, String theSenderOtherDetails,
                                                               String theReceiverIdentifierType, String theReceiverIdentifier,String theReceiverAccount, String theReceiverName, String theReceiverOtherDetails,
                                                               String theBeneficiaryIdentifierType, String theBeneficiaryIdentifier,String theBeneficiaryAccount, String theBeneficiaryName, String theBeneficiaryOtherDetails,
                                                               String theAccountNumber, String theAmount, String theCategory, String theTransactionDescription, String theSourceReference, String theRequestApplication, String theSourceApplication, String theTransactionDateTime){

        
        /*
			REQUEST:
            {
               "action": "WITHDRAWAL",
               "payload": {
                  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                  "identifier_type": "MSISDN",
                  "identifier": "254712345678",
                  "pin": "1234",
                  "device_identifier_type": "IMSI/APP_ID",
                  "device_identifier": "1099200912931023",
                  "transaction_reference": "QMS039U05",
                  "transaction_sender_details": {
                     "identifier_type": "SHORT_CODE",
                     "identifier": "200123",
                     "account": "200123",
                     "name": "SACCO B2B",
                     "other_details": ""
                  },
                  "transaction_receiver_details": {
                     "identifier_type": "SHORT_CODE",
                     "identifier": "400200",
                     "account": "400200",
                     "name": "Cooperative Bank",
                     "other_details": ""
                  },
                  "transaction_beneficiary_details": {
                     "identifier_type": "ACCOUNT_NO",
                     "identifier": "0112345678909",
                     "account": "0112345678909",
                     "name": "John Doe Omolo",
                     "other_details": ""
                  },
                  "account_number": "41-02392-0093-01",
                  "amount": 1500,
                  "category": " BANK_TRANSFER",
                  "transaction_description": "Cash Withdrawal by John Doe Omolo (254712345678) to 254712345678",
                  "source_reference": "USSD_36027106_8",
                  "request_application": "MAPP",
                  "source_application": "MBANKING",
                  "transaction_date_time": "2020-01-02 12:34:45"
               }
            }
			  RESPONSE:
				{
                    "transaction_status": "SUCCESS ",
                    "transaction_status_description": "Withdrawal processed successfully",
                    "transaction_date_time": "2020-01-02 12:34:46"
                }

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("transaction_status", "ERROR");
        hmRVal.put("transaction_status_description", "");
        hmRVal.put("transaction_date_time", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "WITHDRAWAL");
            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);

            JSONObject jsonSender = new JSONObject();
            jsonSender.put("identifier_type", theSenderIdentifierType);
            jsonSender.put("identifier", theSenderIdentifier);
            jsonSender.put("account", theSenderAccount);
            jsonSender.put("name", theSenderName);
            jsonSender.put("other_details", theSenderOtherDetails);
            jsonRequestPayload.put("transaction_sender_details", jsonSender);

            JSONObject jsonReceiver = new JSONObject();
            jsonReceiver.put("identifier_type", theReceiverIdentifierType);
            jsonReceiver.put("identifier", theReceiverIdentifier);
            jsonReceiver.put("account", theReceiverAccount);
            jsonReceiver.put("name", theReceiverName);
            jsonReceiver.put("other_details", theReceiverOtherDetails);
            jsonRequestPayload.put("transaction_receiver_details", jsonReceiver);

            JSONObject jsonBeneficiary = new JSONObject();
            jsonBeneficiary.put("identifier_type", theBeneficiaryIdentifierType);
            jsonBeneficiary.put("identifier", theBeneficiaryIdentifier);
            jsonBeneficiary.put("account", theBeneficiaryAccount);
            jsonBeneficiary.put("name", theBeneficiaryName);
            jsonBeneficiary.put("other_details", theBeneficiaryOtherDetails);
            jsonRequestPayload.put("transaction_beneficiary_details", jsonBeneficiary);

            jsonRequestPayload.put("account_number", theAccountNumber);
            jsonRequestPayload.put("amount", theAmount);
            jsonRequestPayload.put("category", theCategory);
            jsonRequestPayload.put("transaction_description", theTransactionDescription);
            jsonRequestPayload.put("source_reference", theSourceReference);
            jsonRequestPayload.put("request_application", theRequestApplication);
            jsonRequestPayload.put("source_application", theSourceApplication);
            jsonRequestPayload.put("transaction_date_time", theTransactionDateTime);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strTransactionStatus = jsonResponse.get("transaction_status").toString();
                    String strTransactionStatusDescription = jsonResponse.get("transaction_status_description").toString();
                    String strTransactionDateTime = jsonResponse.get("transaction_date_time").toString();

                    hmRVal.put("transaction_status", strTransactionStatus);
                    hmRVal.put("transaction_status_description", strTransactionStatusDescription);
                    hmRVal.put("transaction_date_time", strTransactionDateTime);

                }catch (Exception e){
                    System.out.println("CBSAPI.mobileMoneyWithdrawal(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.mobileMoneyWithdrawal(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> mobileMoneyResult(String theApiRequestID, String theTransactionReference, String theTransactionStatus,String theTransactionStatusDescription,
                                                           String theBeneficiaryIdentifierType,String theBeneficiaryIdentifier,String theBeneficiaryName, String theBeneficiaryOtherDetails,
                                                           String theDestinationReference,String theTransactionStatusDateTime){

        /*
			REQUEST:
			{
              "action": "WITHDRAWAL_RESULT",
              "payload": {
                "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "transaction_reference": "OH50CRUHJK",
                "transaction_status": "CONFIRMED/REVERSE_CONFIRMED",
                "transaction_status_description": "Transaction Completed Successfully",
                "transaction_beneficiary_details": {
                  "identifier_type": "MSISDN",
                  "identifier":"254712345678",
                  "name": "JOHN DOE",
                  "other_details": ""
                },
                "destination_reference": "OH50CRUHJK",
                "transaction_date_time": "2020-01-02 12:34:45"
              }
            }

			  RESPONSE:
				{
                    "transaction_status": "SUCCESS/TRANSACTION_DOES_NOT_EXIST/ERROR",
                    "transaction_status_description": "Result processed successfully",
                    "transaction_status_date_time": "2020-01-02 12:34:46"
                  }


			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("transaction_status", "ERROR");
        hmRVal.put("transaction_status_description", "");
        hmRVal.put("transaction_status_date_time", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "WITHDRAWAL_RESULT");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id", theApiRequestID);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("transaction_status", theTransactionStatus);
            jsonRequestPayload.put("transaction_status_description",  theTransactionStatusDescription);

            JSONObject jsonBeneficiary = new JSONObject();
            jsonBeneficiary.put("beneficiary_identifier_type", theBeneficiaryIdentifierType);
            jsonBeneficiary.put("beneficiary_identifier", theBeneficiaryIdentifier);
            jsonBeneficiary.put("beneficiary_name", theBeneficiaryName);
            jsonBeneficiary.put("beneficiary_other_details", theBeneficiaryOtherDetails);

            jsonRequestPayload.put("transaction_beneficiary_details",  jsonBeneficiary);

            jsonRequestPayload.put("destination_reference", theDestinationReference);
            jsonRequestPayload.put("transaction_date_time", theTransactionStatusDateTime);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strTransactionStatus = jsonResponse.get("transaction_status").toString();
                    String strTransactionStatusDescription = jsonResponse.get("transaction_status_description").toString();
                    String strTransactionStatusDateTime = jsonResponse.get("transaction_status_date_time").toString();

                    hmRVal.put("transaction_status", strTransactionStatus);
                    hmRVal.put("transaction_status_description", strTransactionStatusDescription);
                    hmRVal.put("transaction_status_date_time", strTransactionStatusDateTime);

                }catch (Exception e){
                    System.out.println("CBSAPI.mobileMoneyResult(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.mobileMoneyResult(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> mobileMoneyDeposit(String theApiRequestID, String theTransactionReference, String theIdentifierType, String theIdentifier, String theSenderName, String theReceiverType, String theReceiverIdentifier, String theBeneficiaryAccount,Double theAmount,
                                                           String theTransactionDescription,String theSourceReference,String theRequestApplication,String theSourceApplication,String theTransactionStatusDateTime){

        /*
			REQUEST:
			{
                "action":
                "FOSA_DEPOSIT/BOSA_DEPOSIT/LOAN_REPAYMENT_DEPOSIT/OTHER_DEPOSIT",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "sender_name": "John Doe Omolo",
                    "transaction_reference": "PAB9IMA0FJ",
                    "beneficiary_account": "41-02392-0093-01",
                    "amount": 2500.00,
                    "transaction_description": "Pay Bill (1234567) From 254712345678",
                    "source_reference": "PAB9IMA0FJ",
                    "request_application": "MPESA_BROKER",
                    "source_application": "MPESA_BROKER",
                    "transaction_date_time": "2020-01-02 12:34:45"
                }
            }
			  RESPONSE:
				{
                    "transaction_status": "SUCCESS ",
                    "transaction_status_description": "Deposit processed successfully",
                    "transaction_destination_reference": "2fc5e950-769b-4942-a748",
                    "transaction_date_time": "2020-01-02 12:34:46"
                }

			 */

        //UPDATED
        /*
                REQUEST:
                {
                     "action":
                     "FOSA_DEPOSIT/BOSA_DEPOSIT/LOAN_REPAYMENT_DEPOSIT/OTHER_DEPOSIT",
                     "payload": {
                         "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                         "identifier_type": "MSISDN",
                         "identifier": "254712345678",
                         "sender_name": "John Doe Omolo",
                         "receiver_type": "SHORT_CODE",
                         "receiver_identifier": "531800",
                         "transaction_reference": "PAB9IMA0FJ",
                         "beneficiary_account": "41-02392-0093-01",
                         "amount": 2500.00,
                         "transaction_description": "Pay Bill (1234567) From 254712345678",
                         "source_reference": "PAB9IMA0FJ",
                         "request_application": "MPESA_BROKER",
                         "source_application": "MPESA_BROKER",
                         "transaction_date_time": "2020-01-02 12:34:45"
                     }
                 }

                RESPONSE:
                {
                     "transaction_status": "SUCCESS ",
                     "transaction_status_description": "Deposit processed successfully",
                     "transaction_destination_reference": "2fc5e950-769b-4942-a748",
                     "transaction_date_time": "2020-01-02 12:34:46"
                 }

        */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("transaction_status", "ERROR");
        hmRVal.put("transaction_status_description", "");
        hmRVal.put("transaction_destination_reference", "");
        hmRVal.put("transaction_status_date_time", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "OTHER_DEPOSIT");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id", theApiRequestID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("sender_name", theSenderName);
            jsonRequestPayload.put("receiver_type", theReceiverType);
            jsonRequestPayload.put("receiver_identifier", theReceiverIdentifier);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("beneficiary_account", theBeneficiaryAccount);
            jsonRequestPayload.put("amount", theAmount);
            jsonRequestPayload.put("transaction_description", theTransactionDescription);
            jsonRequestPayload.put("source_reference", theSourceReference);
            jsonRequestPayload.put("request_application", theRequestApplication);
            jsonRequestPayload.put("source_application", theSourceApplication);
            jsonRequestPayload.put("transaction_date_time", theTransactionStatusDateTime);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strTransactionStatus = jsonResponse.get("transaction_status").toString();
                    String strTransactionStatusDescription = jsonResponse.get("transaction_status_description").toString();
                    String strTransactionDestinationReference = jsonResponse.get("transaction_destination_reference").toString();
                    String strTransactionStatusDateTime = jsonResponse.get("transaction_status_date_time").toString();

                    hmRVal.put("transaction_status", strTransactionStatus);
                    hmRVal.put("transaction_status_description", strTransactionStatusDescription);
                    hmRVal.put("transaction_destination_reference", strTransactionDestinationReference);
                    hmRVal.put("transaction_status_date_time", strTransactionStatusDateTime);

                }catch (Exception e){
                    System.out.println("CBSAPI.mobileMoneyDeposit(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.mobileMoneyDeposit(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> internalFundsTransfer(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                               String theTransactionReference, String theSourceAccount, String theDestinationAccount, String theAmount, String theSourceReference,
                                                               String theRequestApplication, String theSourceApplication, String theTransactionDescription, String theTransactionDateTime, String theAction){

        /*
			REQUEST:
			{
                "action": "IFT_ACCOUNT_TO_ACCOUNT/IFT_LOAN_REPAYMENT/IFT_SCHEMES_DEPOSIT",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023",
                    "transaction_reference": "U12345678S9",
                    "source_account": "41-02392-0093-01",
                    "destination_account": "U:203813:03:2020",
                    "amount": 35000.00,
                    "source_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "request_application": "USSD",
                    "source_application": "MBANKING",
                    "transaction_description": "Loan Repayment Internal Funds Transfer. Source A/C: 41-02392-0093-01 - Destination A/C: U:203813:03:2020",
                    "transaction_date_time": "2020-01-02 12:34:45"
                    }
                }

			  RESPONSE: ???
				{
                    "request_status": "SUCCESS ",
                    "request_status_description": "Internal Funds Transfer request received successfully"
                }


                ???
            {
               "transaction_destination_reference": "aec0a796-9fc8-11eb-8044-000c299a0fc6",
               "transaction_status_date_time": "2021-04-18 01:03:04",
               "login_status": "SUCCESS",
               "auth_action_valid_date": "",
               "login_flag": "",
               "login_attempts": 0,
               "otp_flag": "",
               "otp_attempts": "0",
               "auth_action": "NONE",
               "ift_dest_accounts": [],
               "request_status": "USER_NOT_FOUND"
            }

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("request_status", "ERROR");
        hmRVal.put("request_status_description", "ERROR");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", theAction);

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);

            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("source_account", theSourceAccount);
            jsonRequestPayload.put("destination_account", theDestinationAccount);
            jsonRequestPayload.put("amount", theAmount);
            jsonRequestPayload.put("source_reference", theSourceReference);
            jsonRequestPayload.put("request_application", theRequestApplication);
            jsonRequestPayload.put("source_application", theSourceApplication);
            jsonRequestPayload.put("transaction_description", theTransactionDescription);
            jsonRequestPayload.put("transaction_date_time", theTransactionDateTime);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strTransactionStatus= jsonResponse.get("transaction_status").toString();
                    String strTransactionStatusDescription = "";//jsonResponse.get("transaction_status_description").toString(); todo: TONY TO STANDARDIZE API TO RETURN THIS ON ALL CALLS - CURRENTLY NOT WORKING ON 'USER_NOT_FOUND'

                    hmRVal.put("transaction_status", strTransactionStatus);
                    hmRVal.put("transaction_status_description", strTransactionStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.internalFundsTransfer(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            System.out.println("CBSAPI.internalFundsTransfer(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<Object, Object>  getUserDetails(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theUserIdentifierType, String theUserIdentifier){

       /*
			REQUEST:
			{
				{
                    "action": "GET_USER_DETAILS",
                    "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023",
                    "user_identifier_type": "MSISDN",
                    "user_identifier": "254712345678"
                }
            }

			RESPONSE - POSITIVE RESPONSE:

            {
               "transaction_destination_reference": "48ef23ac-a05f-11eb-8044-000c299a0fc6",
               "transaction_status_date_time": "2021-04-18 19:01:07",
               "login_status": "SUCCESS",
               "auth_action_valid_date": "",
               "login_flag": "",
               "login_attempts": 0,
               "otp_flag": "",
               "otp_attempts": "0",
               "auth_action": "NONE",
               "ift_dest_accounts": [
                  {
                     "account_status": "ACTIVE",
                     "account_active": "YES",
                     "account_name": "JACKSON ONYANGO OCHIENG",
                     "account_number": "5-02-00013-00",
                     "account_type_class": "Savings Accounts",
                     "account_type_name": "JIPANGE ACCOUNTS",
                     "account_label": "JIPANGE AC(5-02-00013-00)"
                  },
                  {
                     "account_status": "ACTIVE",
                     "account_active": "YES",
                     "account_name": "JACKSON OCHIENG ONYANGO",
                     "account_number": "5-04-05925-00",
                     "account_type_class": "Savings Accounts",
                     "account_type_name": "CUSTOMERS CURRENT ACCOUNTS",
                     "account_label": "CUSTOMERS(5-04-05925-00)"
                  },
                  {
                     "account_status": "ACTIVE",
                     "account_active": "YES",
                     "account_name": "JACKSON OCHIENG",
                     "account_number": "5-06-03233-00",
                     "account_type_class": "Savings Accounts",
                     "account_type_name": "FOSA SAVE SCHEME",
                     "account_label": "FOSA SAVE(5-06-03233-00)"
                  }
               ],
               "request_status": "SUCCESS",
               "request_status_details": "SUCCESS",
               "member_number": "06438",
               "full_name": "Magero Otieno",
               "identifier_type": "MSISDN",
               "identifier": "254721913958",
               "identity_type": "NATIONAL_ID",
               "identity": "111111"
            }
			*/

        HashMap<Object, Object> hmRVal = new HashMap<>();
        hmRVal.put("user_details",null);
        hmRVal.put("accounts",null);

        HashMap<String, String> hmUserDetails = new HashMap<>();
        hmUserDetails.put("request_status","ERROR");

        HashMap<String, HashMap <String, String>> hmAccounts = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_USER_DETAILS");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("user_identifier_type", theUserIdentifierType);
            jsonRequestPayload.put("user_identifier", theUserIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = (String) jsonResponse.get("request_status");
                    String strMemberNumber = "";
                    String strFullName = "";
                    String strIdentifierType = "";
                    String strIdentifier = "";
                    String strIdentityType = "";
                    String strIdentity = "";

                    if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                        strMemberNumber = (String)  jsonResponse.get("member_number");
                        strFullName = (String)  jsonResponse.get("full_name");
                        strIdentifierType = (String)  jsonResponse.get("identifier_type");
                        strIdentifier = (String)  jsonResponse.get("identifier");
                        strIdentityType = (String)  jsonResponse.get("identity_type");
                        strIdentity = (String)  jsonResponse.get("identity");
                    }

                    hmUserDetails.put("request_status",strRequestStatus);
                    hmUserDetails.put("member_number",strMemberNumber);
                    hmUserDetails.put("full_name",strFullName);
                    hmUserDetails.put("identifier_type",strIdentifierType);
                    hmUserDetails.put("identifier",strIdentifier);
                    hmUserDetails.put("identity_type",strIdentityType);
                    hmUserDetails.put("identity",strIdentity);

                    hmRVal.put("user_details",hmUserDetails);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            String strRequestStatus = hmUserDetails.get("request_status");

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmAccounts = new HashMap<>();

                JSONArray dataArray= jsonResponse.getJSONArray("ift_dest_accounts");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    /*
                     "account_status": "ACTIVE",
                     "account_active": "YES",
                     "account_name": "JACKSON ONYANGO OCHIENG",
                     "account_number": "5-02-00013-00",
                     "account_type_class": "Savings Accounts",
                     "account_type_name": "JIPANGE ACCOUNTS",
                     "account_label": "JIPANGE AC(5-02-00013-00)"

                     */

                    String strAccountStatus = object.getString("account_status");
                    String strAccountActive = object.getString("account_active");
                    String strAccountName = object.getString("account_name");
                    String strAccountNumber = object.getString("account_number");
                    String strAccountType_class = object.getString("account_type_class");
                    String strAccountType_name = object.getString("account_type_name");
                    String strAccountLabel = object.getString("account_label");

                    HashMap<String, String> hmAccount = new HashMap<>();
                    //hmAccount.put("status",strAccountStatus);
                    //hmAccount.put("active",strAccountActive);
                    hmAccount.put("name",strAccountName);
                    hmAccount.put("number",strAccountNumber);
                    //hmAccount.put("type_class",strAccountType_class);
                    hmAccount.put("type_name",strAccountType_name);
                    hmAccount.put("label",strAccountLabel);

                    hmAccounts.put(strAccountNumber,hmAccount);
                }

                hmRVal.put("accounts",hmAccounts);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.getUserDetails(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<Object, Object> verifyBusinessShortCode(String theTraceID, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier, String theUserIdentifierType, String theUserIdentifier, String theBusinessShortCode){

       /*
			REQUEST:
			{
              "action": "VALIDATE_BUSINESS_SHORT_CODE",
              "payload": {
                "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "identifier_type": "MSISDN",
                "identifier": "254712345678",
                "device_identifier_type": "IMSI/APP_ID",
                "device_identifier": "1099200912931023",
                "business_short_code": "500100"
              }
            }


			RESPONSE - POSITIVE RESPONSE:
            {
              "request_status": "SUCCESS/SHORT_CODE_NOT_FOUND/ERROR",
              "business_short_code": "500100",
              "business_name": "John's Hardware",
              "associated_account": {
                "account_name": "Salary Account",
                "account_label": "Salary Account (41-02392-0093-01)",
                "account_number": "41-02392-0093-01",
                "account_balance": 1600.00
              }
            }

			*/

        HashMap<Object, Object> hmRVal = new HashMap<>();
        hmRVal.put("associated_account",null);

        HashMap<String, String> hmAssociatedAccount = new HashMap<>();

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "VALIDATE_BUSINESS_SHORT_CODE");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("business_short_code", theBusinessShortCode);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            /*String strJSONResponse = "{\n" +
                    "              \"request_status\": \"SUCCESS\",\n" +
                    "              \"business_short_code\": \"500100\",\n" +
                    "              \"business_name\": \"John's Hardware\",\n" +
                    "              \"associated_account\": {\n" +
                    "                \"account_name\": \"Salary Account\",\n" +
                    "                \"account_label\": \"Salary Account (41-02392-0093-01)\",\n" +
                    "                \"account_number\": \"41-02392-0093-01\",\n" +
                    "                \"account_balance\": 1600.00\n" +
                    "              }\n" +
                    "            }";*/
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = (String) jsonResponse.get("request_status");
                    String strBusinessShortCode = "";
                    String strBusinessName = "";

                    if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                        strBusinessShortCode = (String)  jsonResponse.get("business_short_code");
                        strBusinessName = (String)  jsonResponse.get("business_name");
                    }

                    hmRVal.put("request_status", strRequestStatus);
                    hmRVal.put("business_short_code", strBusinessShortCode);
                    hmRVal.put("business_name", strBusinessName);

                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            String strRequestStatus = (String) hmRVal.get("request_status");

            if(jsonResponse != null) {
                if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                    JSONObject associatedAccount = jsonResponse.getJSONObject("associated_account");
                    String strAccountName = associatedAccount.getString("account_name");
                    String strAccountLabel = associatedAccount.getString("account_label");
                    String strAccountNumber = associatedAccount.getString("account_number");
                    String strAccountBalance = String.valueOf(associatedAccount.getDouble("account_balance"));

                    hmAssociatedAccount.put("account_name", strAccountName);
                    hmAssociatedAccount.put("account_label", strAccountLabel);
                    hmAssociatedAccount.put("account_number", strAccountNumber);
                    hmAssociatedAccount.put("account_balance", strAccountBalance);

                    hmRVal.put("associated_account", hmAssociatedAccount);
                }
            }

        }catch (Exception e){
            System.out.println("CBSAPI.verifyBusinessShortCode(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<String, Object>  getLoansInService(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier){

       /*
			REQUEST:
			{
                "action": "GET_LOANS_IN_SERVICE",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023"
                }
            }

			RESPONSE:
            {
               "transaction_destination_reference": "b818c1e9-a1eb-11eb-8044-000c299a0fc6",
               "transaction_status_date_time": "2021-04-20 18:18:54",
               "login_status": "SUCCESS",
               "auth_action_valid_date": "",
               "login_flag": "",
               "login_attempts": 0,
               "otp_flag": "",
               "otp_attempts": "0",
               "auth_action": "NONE",
               "request_status": "SUCCESS",
               "request_status_description": "SUCCESS",
               "loans": [
                  {
                     "loan_id": "L:065870:04:2019",
                     "loan_type_id": "L",
                     "loan_type_name": "ENDELEA LOAN",
                     "loan_amount": 32600,
                     "loan_balance": 4061,
                     "interest_balance": 370.76,
                     "loan_defaulted_amount": 2695,
                     "loan_issued_date": "2019-05-02 00:00",
                     "loan_end_date": "2021-05-02 00:00",
                     "loan_performance": "",
                     "loan_performance_description": "",
                     "account_label": "ENDELEA LO(L:065870:04:2019)",
                     "installment_amount": 1358
                  },
                  {
                     "loan_id": "V:083669:01:2021",
                     "loan_type_id": "V",
                     "loan_type_name": "SALARY ADVANCE",
                     "loan_amount": 2200,
                     "loan_balance": 2200,
                     "interest_balance": 220,
                     "loan_defaulted_amount": 2200,
                     "loan_issued_date": "2021-02-01 00:00",
                     "loan_end_date": "2021-03-01 00:00",
                     "loan_performance": "",
                     "loan_performance_description": "",
                     "account_label": "SALARY ADV(V:083669:01:2021)",
                     "installment_amount": 2200
                  }
               ]
            }


			*/

        String strRequestStatus = "ERROR";

        HashMap<String, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_status","ERROR");
        hmRVal.put("loans",null);

        HashMap<String, HashMap <String, String>> hmLoans = null;

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_LOANS_IN_SERVICE");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;


            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    strRequestStatus = jsonResponse.get("request_status").toString();
                    hmRVal.put("request_status",strRequestStatus);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmLoans = new HashMap<>();
                JSONArray dataArray= jsonResponse.getJSONArray("loans");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    /*
                    "loan_id": "L:065870:04:2019",
                     "loan_type_id": "L",
                     "loan_type_name": "ENDELEA LOAN",
                     "loan_amount": 32600,
                     "loan_balance": 4061,
                     "interest_balance": 370.76,
                     "loan_defaulted_amount": 2695,
                     "loan_issued_date": "2019-05-02 00:00",
                     "loan_end_date": "2021-05-02 00:00",
                     "loan_performance": "",
                     "loan_performance_description": "",
                     "account_label": "ENDELEA LO(L:065870:04:2019)",
                     "installment_amount": 1358
                     */

                    String strLoanID =  object.getString("loan_id");
                    String strLoanTypeID =  object.getString("loan_type_id");
                    String strLoanTypeName =  object.getString("loan_type_name");
                    String strLoanAmount =  String.valueOf(object.getDouble("loan_amount"));
                    String strLoanBalance =  String.valueOf(object.getDouble("loan_balance"));
                    String strInterestBalance =  String.valueOf(object.getDouble("interest_balance"));
                    String strLoanDefaultedAmount =  String.valueOf(object.getDouble("loan_defaulted_amount"));
                    String strLoanIssuedDate =  object.getString("loan_issued_date");
                    String strLoanEndDate =  object.getString("loan_end_date");
                    String strLoanPerformance = object.getString("loan_performance");
                    String strLoanPerformanceDescription = object.getString("loan_performance_description");
                    String strLoanAccountLabel = object.getString("account_label");
                    String strLoanInstallmentAmount= String.valueOf(object.getDouble("installment_amount"));

                    HashMap<String, String> hmLoan = new HashMap<>();
                    hmLoan.put("id",strLoanID);
                    //hmLoan.put("type_id",strLoanTypeID);
                    hmLoan.put("type",strLoanTypeName);
                    hmLoan.put("amount",strLoanAmount);
                    hmLoan.put("balance",strLoanBalance);
                    hmLoan.put("interest",strInterestBalance);
                    //hmLoan.put("loan_defaulted_amount",strLoanDefaultedAmount);
                    //hmLoan.put("loan_issued_date",strLoanIssuedDate);
                    //hmLoan.put("loan_end_date",strLoanEndDate);
                    //hmLoan.put("loan_performance",strLoanPerformance);
                    //hmLoan.put("loan_performance_description",strLoanPerformanceDescription);
                    hmLoan.put("label",strLoanAccountLabel);
                    hmLoan.put("installment",strLoanInstallmentAmount);
                    hmLoans.put(strLoanID,hmLoan);

                }

                hmRVal.put("loans",hmLoans);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.getLoansInService(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<Object, Object>  loanMiniStatement(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,
                                                                 String theStatementType, int theMaxNumberOfTransactions, String theStartDate, String theEndDate, String theLoanID){

       /*
			REQUEST:
			{
                "action": "LOAN_STATEMENT",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023",
                    "statement_type": "MINI_STATEMENT/FULL_STATEMENT",
                    "max_number_of_transactions": 100,
                    "start_date": "2021-01-01 20:00:00",
                    "end_date": "2021-01-03 20:00:00",
                    "loan_id": "U:203813:03:2020"
                }
            }

			RESPONSE - POSITIVE RESPONSE:
            {
               "request_status": "SUCCESS",
               "loan_id": "L:065870:04:2019",
               "loan_type_id": "L",
               "loan_type_name": "ENDELEA LOAN",
               "loan_amount": 32600,
               "loan_balance": 4061,
               "interest_balance": 370.76,
               "account_type": "LOAN",
               "transactions": [
                  {
                     "transaction_date_time": "2021-01-01 00:00:00",
                     "transaction_reference": "BF02",
                     "transaction_date": "2021-01-01",
                     "transaction_time": "00:00:00",
                     "transaction_description": "Opening Bal.",
                     "transaction_amount": 5420,
                     "running_balance": 5420,
                     "int_transaction_amount": -8.8,
                     "int_running_balance": -8.8,
                     "other_details": ""
                  },
                  {
                     "transaction_date_time": "2021-01-28 15:32:41",
                     "transaction_reference": "PYBTC134",
                     "transaction_date": "2021-01-28",
                     "transaction_time": "15:32:41",
                     "transaction_description": "Interest Repay From FLAMINGO(NAIV) PAYMENT[ ENDELEA LOAN ]",
                     "transaction_amount": 0,
                     "running_balance": 5420,
                     "int_transaction_amount": -380.44,
                     "int_running_balance": -389.24,
                     "other_details": ""
                  },
                  {
                     "transaction_date_time": "2021-01-28 15:32:41",
                     "transaction_reference": "PYBTC134",
                     "transaction_date": "2021-01-28",
                     "transaction_time": "15:32:41",
                     "transaction_description": "Loan Repay From FLAMINGO(NAIV) PAYMENT[ ENDELEA LOAN ]",
                     "transaction_amount": -1359,
                     "running_balance": 4061,
                     "int_transaction_amount": 0,
                     "int_running_balance": -389.24,
                     "other_details": ""
                  },
                  {
                     "transaction_date_time": "2021-01-31 02:29:49",
                     "transaction_reference": "20210131_22949",
                     "transaction_date": "2021-01-31",
                     "transaction_time": "02:29:49",
                     "transaction_description": "Interest Charged January",
                     "transaction_amount": 0,
                     "running_balance": 4061,
                     "int_transaction_amount": 380,
                     "int_running_balance": -9.24,
                     "other_details": ""
                  },
                  {
                     "transaction_date_time": "2021-02-02 07:21:08",
                     "transaction_reference": "20210202_7218",
                     "transaction_date": "2021-02-02",
                     "transaction_time": "07:21:08",
                     "transaction_description": "Interest Charged February",
                     "transaction_amount": 0,
                     "running_balance": 4061,
                     "int_transaction_amount": 380,
                     "int_running_balance": 370.76,
                     "other_details": ""
                  }
               ]
            }

			*/

        HashMap<Object, Object> hmRVal = new HashMap<>();
        hmRVal.put("request_details",null);
        hmRVal.put("transactions",null);

        HashMap<String, String> hmRequestDetails = new HashMap<>();
        hmRequestDetails.put("request_status","ERROR");

        LinkedHashMap<String, HashMap <String, String>> hmTransactions = null;

        try {


            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "LOAN_STATEMENT");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);

            jsonRequestPayload.put("statement_type", theStatementType);
            jsonRequestPayload.put("max_number_of_transactions", theMaxNumberOfTransactions);
            jsonRequestPayload.put("start_date", theStartDate);
            jsonRequestPayload.put("end_date", theEndDate);
            jsonRequestPayload.put("loan_id", theLoanID);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);

                    String strRequest_status  = (String) jsonResponse.get("request_status");
                    String strLoanID  = (String)  jsonResponse.get("loan_id");
                    String strLoanTypeID  = (String)  jsonResponse.get("loan_type_id");
                    String strLoanTypeName  = (String)  jsonResponse.get("loan_type_name");
                    String strLoanAmount  = String.valueOf(jsonResponse.getDouble("loan_amount"));
                    String strLoanBalance  = String.valueOf(jsonResponse.get("loan_balance"));
                    String strInterestBalance  = String.valueOf(jsonResponse.get("interest_balance"));
                    String strAccountType  = (String) jsonResponse.get("account_type");

                    hmRequestDetails.put("request_status",strRequest_status);
                    hmRequestDetails.put("loan_id",strLoanID);
                    hmRequestDetails.put("loan_type_id",strLoanTypeID);
                    hmRequestDetails.put("loan_type_name",strLoanTypeName);
                    hmRequestDetails.put("loan_amount",strLoanAmount);
                    hmRequestDetails.put("loan_balance",strLoanBalance);
                    hmRequestDetails.put("interest_balance",strInterestBalance);
                    hmRequestDetails.put("account_type",strAccountType);

                    hmRVal.put("request_details",hmRequestDetails);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            String strRequestStatus = hmRequestDetails.get("request_status");

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmTransactions = new LinkedHashMap<>();

                JSONArray dataArray= jsonResponse.getJSONArray("transactions");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    String strTransactionDateTime =  object.getString("transaction_date_time");
                    String strTransactionReference =  object.getString("transaction_reference");
                    String strTransactionDate = object.getString("transaction_date");
                    String strTransactionTime = object.getString("transaction_time");
                    String strTransactionDescription = object.getString("transaction_description");
                    String strTransactionAmount = String.valueOf(object.getDouble("transaction_amount"));
                    String strRunningBalance = String.valueOf(object.getDouble("running_balance"));
                    String strIntTransactionBalance = String.valueOf(object.getDouble("int_transaction_amount"));
                    String strIntRunningBalance = String.valueOf(object.getDouble("int_running_balance"));


                    HashMap<String, String> hmTransaction = new HashMap<>();
                    hmTransaction.put("transaction_date_time",strTransactionDateTime);
                    hmTransaction.put("transaction_reference",strTransactionReference);
                    hmTransaction.put("transaction_date",strTransactionDate);
                    hmTransaction.put("transaction_time",strTransactionTime);
                    hmTransaction.put("transaction_description",strTransactionDescription);
                    hmTransaction.put("transaction_amount",strTransactionAmount);
                    hmTransaction.put("running_balance",strRunningBalance);
                    hmTransaction.put("int_transaction_amount",strIntTransactionBalance);
                    hmTransaction.put("int_running_balance",strIntRunningBalance);
                    hmTransactions.put(Integer.toString(i),hmTransaction);
                }

                hmRVal.put("transactions",hmTransactions);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.loanMiniStatement(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> checkLoanQualification(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theTransactionReference, String theLoanName, String theLoanTypeID){

        /*
			REQUEST:
			{
                "action": "CHECK_LOAN_LIMIT",
                "payload": {
                "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                "identifier_type": "MSISDN",
                "identifier": "254712345678",
                "pin": "1234",
                "device_identifier_type": "IMSI/APP_ID",
                "device_identifier": "1099200912931023",
                "transaction_reference":"ef6582cc-b972-457a-998f-7fb6fa932167",
                "loan_name": "SUPA LOAN",
                "loan_type_id": "T"
                }
            }
			  RESPONSE:
				{
                    "request_status": "SUCCESS/INCORRECT_PIN/ERROR",
                    "request_status_description": "Check loan limit request received successfully"
                }

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("request_status", "ERROR");
        hmRVal.put("request_status_description", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "CHECK_LOAN_LIMIT");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("loan_name", theLoanName);
            jsonRequestPayload.put("loan_type_id", theLoanTypeID);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = jsonResponse.get("request_status").toString();
                    String strRequestStatusDescription = jsonResponse.get("request_status_description").toString();

                    hmRVal.put("request_status", strRequestStatus);
                    hmRVal.put("request_status_description", strRequestStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.checkLoanQualification(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            System.out.println("CBSAPI.checkLoanQualification(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> checkLoanGuarantorshipAbility(String theTraceID, String theIdentifierType, String theIdentifier, String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, String theTransactionReference){

        /*
			REQUEST:
            {
                "action": "GET_GUARANTORSHIP_ABILITY_ENQUIRY",
                "payload": {
                  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                  "identifier_type": "MSISDN",
                  "identifier": "254712345678",
                  "pin": "1234",
                  "device_identifier_type": "IMSI",
                  "device_identifier": "123456789123456",
                  "transaction_reference":"ef6582cc-b972-457a-998f-7fb6fa932167"
                }
             }
			  RESPONSE:
				{
                    "transaction_destination_reference": "be7091a5-1768-46ed-a4ba-db239d364706",
                    "transaction_status_date_time": "2021-11-16 02:05:50",
                    "login_status": "SUCCESS",
                    "auth_action_valid_date": "",
                    "login_flag": "",
                    "login_attempts": 0,
                    "otp_flag": "",
                    "otp_attempts": "0",
                    "auth_action": "NONE",
                    "request_status": "SUCCESS",
                    "request_status_description": "SUCCESS",
                    "default_fosa_acc": "5-04-00071-00",
                    "amount": 0.0
                }
			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("request_status", "ERROR");
        hmRVal.put("request_status_description", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_GUARANTORSHIP_ABILITY_ENQUIRY");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();

            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = jsonResponse.get("request_status").toString();
                    String strRequestStatusDescription = jsonResponse.get("request_status_description").toString();

                    hmRVal.put("request_status", strRequestStatus);
                    hmRVal.put("request_status_description", strRequestStatusDescription);

                }catch (Exception e){
                    e.printStackTrace();
                    System.out.println("CBSAPI.checkLoanQualification(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("CBSAPI.checkLoanQualification(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static  HashMap<Object, Object>  getLoanTypes(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier){

       /*
			REQUEST:
			{
                "action": " GET_LOAN_TYPES_MOBILE",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023"
                }
            }

			RESPONSE - POSITIVE RESPONSE:
            {
               "transaction_destination_reference": "a40dcd37-a2d5-11eb-8044-000c299a0fc6",
               "transaction_status_date_time": "2021-04-21 22:13:23",
               "login_status": "SUCCESS",
               "auth_action_valid_date": "",
               "login_flag": "",
               "login_attempts": 0,
               "otp_flag": "",
               "otp_attempts": "0",
               "auth_action": "NONE",
               "loan_types": [
                  {
                     "loan_type_id": "A",
                     "loan_type_code": "017",
                     "loan_type_name": "MOBILE SALARY ADVANCE",
                     "loan_type_max_ussd_amount": 10000,
                     "loan_type_min_ussd_amount": 0,
                     "loan_type_max_amount": 50000,
                     "loan_type_max_duration": 1,
                     "loan_type_interest": 10
                  },
                  {
                     "loan_type_id": "J",
                     "loan_type_code": "027",
                     "loan_type_name": "MOBILE LOAN",
                     "loan_type_max_ussd_amount": 0,
                     "loan_type_min_ussd_amount": 0,
                     "loan_type_max_amount": 50000,
                     "loan_type_max_duration": 1,
                     "loan_type_interest": 10
                  }
               ],
               "request_status": "SUCCESS"
            }

			*/

        HashMap<Object, Object> hmRVal = new HashMap<>();
        hmRVal.put("loan_details",null);
        hmRVal.put("loan_types",null);

        HashMap<String, String> hmLoanDetails = new HashMap<>();
        hmLoanDetails.put("request_status","ERROR");

        HashMap<String, HashMap <String, String>> hmLoanTypes = null;

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "GET_LOAN_TYPES_MOBILE");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;

            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = (String) jsonResponse.get("request_status");
                    hmLoanDetails.put("request_status",strRequestStatus);
                    hmRVal.put("loan_details",hmLoanDetails);
                }catch (Exception e){
                    System.out.println("Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

            String strRequestStatus = hmLoanDetails.get("request_status");

            if(strRequestStatus.equalsIgnoreCase("SUCCESS")){
                hmLoanTypes = new HashMap<>();

                JSONArray dataArray= jsonResponse.getJSONArray("loan_types");
                for(int i = 0; i < dataArray.length(); i++) {
                    JSONObject object = dataArray.getJSONObject(i);

                    String strLoanTypeID =  object.getString("loan_type_id");
                    String strLoanTypeCode =  object.getString("loan_type_code");
                    String strLoanTypeName = object.getString("loan_type_name");
                    String strLoanTypeMaxUssdAmount = String.valueOf(object.getDouble("loan_type_max_ussd_amount"));
                    String strLoanTypeMinUssdAmount= String.valueOf(object.getDouble("loan_type_min_ussd_amount"));
                    String strLoanTypeMaxAmount = String.valueOf(object.getDouble("loan_type_max_amount"));
                    String strLoanTypeMaxDuration = String.valueOf(object.getDouble("loan_type_max_duration"));
                    String strLoanTypeInterest = String.valueOf(object.getDouble("loan_type_interest"));

                    HashMap<String, String> hmLoanType = new HashMap<>();
                    hmLoanType.put("id",strLoanTypeID);
                    hmLoanType.put("code",strLoanTypeCode);
                    hmLoanType.put("name",strLoanTypeName);
                    hmLoanType.put("max",strLoanTypeMaxUssdAmount);
                    hmLoanType.put("min",strLoanTypeMinUssdAmount);
                    hmLoanType.put("duration",strLoanTypeMaxDuration);
                    hmLoanType.put("interest",strLoanTypeInterest);
                    hmLoanTypes.put(strLoanTypeID,hmLoanType);
                }

                hmRVal.put("loan_types",hmLoanTypes);
            }

        }catch (Exception e){
            System.out.println("CBSAPI.getLoanTypes(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> loanApplication(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier,String theTransactionReference,
                                                         String theloanTypeID, String theAmount, String theSourceReference, String theRequestApplication, String theSourceApplication, String theTransactionDateTime){

        /*
			REQUEST:
			{
                "action": "LOAN_APPLICATION",
                "payload": {
                    "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "identifier_type": "MSISDN",
                    "identifier": "254712345678",
                    "pin": "1234",
                    "device_identifier_type": "IMSI/APP_ID",
                    "device_identifier": "1099200912931023",
                    "transaction_reference": "U12345678S9",
                    "loan_type_id": "N",
                    "amount": 120000.00,
                    "source_reference": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
                    "request_application": "USSD",
                    "source_application": "MBANKING",
                    "transaction_date_time": "2020-01-02 12:34:45"
                }
             }
			  RESPONSE:
				{
                    "request_status": "SUCCESS/INCORRECT_PIN/LOAN_APPLICATION_EXISTS/ERROR",
                    "request_status_description": "Loan application request received successfully"
                }

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("request_status", "ERROR");
        hmRVal.put("request_status_description", "");

        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "LOAN_APPLICATION");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequestPayload.put("transaction_reference", theTransactionReference);
            jsonRequestPayload.put("loan_type_id", theloanTypeID);
            jsonRequestPayload.put("amount", theAmount);
            jsonRequestPayload.put("source_reference", theSourceReference);
            jsonRequestPayload.put("request_application", theRequestApplication);
            jsonRequestPayload.put("source_application", theSourceApplication);
            jsonRequestPayload.put("transaction_date_time", theTransactionDateTime);

            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {

                    jsonResponse = new JSONObject(strJSONResponse);
                    String strRequestStatus = jsonResponse.get("request_status").toString();
                    String strRequestStatusDescription = jsonResponse.get("request_status_description").toString();

                    hmRVal.put("request_status", strRequestStatus);
                    hmRVal.put("request_status_description", strRequestStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.loanApplication(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }
        }catch (Exception e){
            System.out.println("CBSAPI.loanApplication(): " + e.getMessage());
        }

        return hmRVal;
    }

    public static HashMap<String,String> deactivateMobileApp(String theTraceID, String theIdentifierType, String theIdentifier,String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier){

        /*
			REQUEST:
			{


				"action": "DEACTIVATE_MOBILE_APP",
				"payload": {
				  "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
				  "identifier_type": "MSISDN",
				  "identifier": "254721913958",
				  "pin": "1234",
				  "device_identifier_type": "IMSI",
				  "device_identifier": "1099200912931023"
				}
			  }

			  RESPONSE:
			  {
                    "mobile_app_activation_status": "SUCCESS ",
                    "mobile_app_activation_status_description": "Mobile App Deactivated successfully"
             }

			 */

        HashMap<String,String> hmRVal = new HashMap<>();

        hmRVal.put("login_status", "ERROR");
        hmRVal.put("login_attempts", "0");
        hmRVal.put("auth_action_valid_date", "");

        try {

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("action", "DEACTIVATE_MOBILE_APP");

            JSONObject jsonRequestPayload = new JSONObject();
            jsonRequestPayload.put("api_request_id",  theTraceID);
            jsonRequestPayload.put("identifier_type", theIdentifierType);
            jsonRequestPayload.put("identifier", theIdentifier);
            jsonRequestPayload.put("pin", thePIN);
            jsonRequestPayload.put("device_identifier_type", theDeviceIdentifierType);
            jsonRequestPayload.put("device_identifier", theDeviceIdentifier);
            jsonRequest.put("payload", jsonRequestPayload);

            String strJSONRequest = jsonRequest.toString();
            String strJSONResponse = MBankingAPIUtils.jsonHttpsPost(strJSONRequest);

            JSONObject jsonResponse = null;
            if(strJSONResponse!=null){
                try {
                    jsonResponse = new JSONObject(strJSONResponse);
                    String strMobileAppActivationStatus = jsonResponse.get("mobile_app_activation_status").toString();
                    String strMobileAppActivationStatusDescription = jsonResponse.get("mobile_app_activation_status_description").toString();

                    hmRVal.put("mobile_app_activation_status", strMobileAppActivationStatus);
                    hmRVal.put("mobile_app_activation_status_description", strMobileAppActivationStatusDescription);

                }catch (Exception e){
                    System.out.println("CBSAPI.userLogin(): Error converting String to JSON");
                }
            }else {
                System.out.println("Received NULL Response");
            }

        }catch (Exception e){
            System.out.println("CBSAPI.deactivateMobileApp(): " + e.getMessage());
        }

        return hmRVal;
    }


}
