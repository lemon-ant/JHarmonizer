// @jharmonizer:fully-off
package deep.skip;

public class DeepFileFullyOff{
    public static void main(String[] args){
        System.out.println(new AnotherDeepType().describe());
    }
}
class AnotherDeepType{String describe(){return "deep";}}
