/*
This is probably some legal info and will be ignored by the parser
 */

/**
 * This is a class def
 *
 * @author  Bob Roberts
 * @date    01/01/01
 * @group   Group Name
 * @group-content   Content
 */
public class ClassName implements InterfaceName{

    /**
     * This is a property
     */
    public String property1{get;set;}

    /**
     * @description Using the legacy way of adding a description
     */
    private String variable1 = "";//Just ignore this comment here

    /**
     * This is a method
     * @author  Bob Roberts
     * @date    01/01/01
     * @param param1    Param and a tab
     * @param param2 Param and a space
     * @return The return value
     */
    @Override
    public String OverriddenMethod(String param1, Decimal param2){
        return null;
    }

    /**
     * An inner class
     */
    private class InnerClass{

    }

}