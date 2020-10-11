package Helpers;

public class StringHelper {
    public static String getCode(int num)
    {
        String code = num + "";
        for (int j = code.length(); j < 4; j++)
        {
            code = "0" + code;
        }
        return code;
    }
}
