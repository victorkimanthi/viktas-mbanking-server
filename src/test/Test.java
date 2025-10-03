
package test;

import ke.skyworld.crypto.DataEncryption;
import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.mbanking.ussdapi.APIUtils;

public class Test {
    public static void main(String[] args)  {

        try {
            String strTempAccount = APIUtils.getCurrentDate("yyMMddHHmmss"); //0001301001442
            System.out.println(strTempAccount);

            DataEncryption theDataEncryption = new DataEncryption();
        }catch (Exception e){
            System.err.println("Error on Test.main(): " +e.getMessage());
        }

    }
}