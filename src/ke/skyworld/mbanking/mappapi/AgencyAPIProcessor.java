package ke.skyworld.mbanking.mappapi;

import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;

public class AgencyAPIProcessor {
    public MAPPResponse processAgencyAPI(MAPPRequest theMAPPRequest){

        MAPPResponse theMAPPResponse = null;

        AgencyAPI theAgencyAPI = new AgencyAPI();
        MAPPAPI theMAPPAPI = new MAPPAPI();

        try{

        } catch (Exception e){
            System.err.println("AgencyAPIProcessor.processAgencyAPI() ERROR : " + e.getMessage());
        }finally{
            theAgencyAPI = null;
        }

        return  theMAPPResponse;
    }
}
