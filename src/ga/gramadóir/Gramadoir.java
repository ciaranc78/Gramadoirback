/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ga.gramadóir;

import com.sun.star.linguistic2.SingleProofreadingError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ciarán Campbell
 *
 */

public class Gramadoir extends Main {

    private ArrayList<SingleProofreadingError> errors;
    private SingleProofreadingError error;
    private Hashtable<Integer, String> ignoreOnceErrors;
    private List<String> ignoreRuleErrors;

    public ArrayList<SingleProofreadingError> checkGrammer(String text, Hashtable< Integer, String> once,
                                                             ArrayList<String> rule){

        ignoreOnceErrors=once;
        ignoreRuleErrors=rule;
        String perlCommand=new String("echo "+text+"  | /opt/gramadoir/gram-ga.exe --ionchod=utf-8 --api");
        String [] commands = {"bash", "-c", perlCommand };
        String nextLine = "";
        try{
            errors=new ArrayList<SingleProofreadingError>();
            Process p = Runtime.getRuntime().exec(commands);
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
	    BufferedReader br = new BufferedReader(isr);
           
            while (nextLine!= null){
                System.out.println(nextLine);
                if(!nextLine.startsWith("<E")){
                    nextLine=br.readLine();
                    continue;
                }
                else{
                    error=createDosError(nextLine, text);
                    if(ignoreRuleErrors.contains(error.aRuleIdentifier) ||
                            (ignoreOnceErrors.containsKey(error.nErrorStart)&&
                                (ignoreOnceErrors.containsValue(error.aRuleIdentifier)))){
                        nextLine=br.readLine();
                        continue;
                    }
                    else{
                        errors.add(error);
                        break;
                    }
                }
            }
            isr.close();
            br.close();
        }catch(Exception e){
           showError(e);
            }
        return errors;
    }
    
    private SingleProofreadingError parseErrorString(String text, String string){
       
        SingleProofreadingError error = new SingleProofreadingError();
        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(string);
        int count=0;

        while (m.find()) {
            String s=m.group().replaceAll("\"", "");
            if (count==1){
                error.nErrorStart=Integer.parseInt(s);
            }
            else if (count==3){
                int end=Integer.parseInt(s);
                error.aRuleIdentifier=text.substring(error.nErrorStart, (end+1));
            }
            else if(count==4){
                 //error.aRuleIdentifier=m.group();
            }
            else if(count==5){
                 error.aFullComment=s;
                 if (error.aFullComment.contains("Do you mean")){
                     String sug= error.aFullComment.replace("Do you mean /","").replace("/?","");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("You should use")){
                     String sug= error.aFullComment.replace("You should use /","").replace("/ here instead","");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Derived incorrectly from the root")){
                     String sug= error.aFullComment.replace("Derived incorrectly from the root /","").replace("/","");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Valid word but")){
                     String sug= error.aFullComment.replace("Valid word but /","").replace("/ is more common","");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Non-standard form of")){
                     String sug= error.aFullComment.replace("Non-standard form of /","").replace("/","");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }


            }
            else if (count==6){
                error.aShortComment=m.group().replaceAll("^\"|\"$", "").trim();
            }
            else if (count==8){
                error.nErrorLength=Integer.parseInt(m.group().replaceAll("^\"|\"$", ""));
            }

            count++;
         }
        error.nErrorType=2;
        return error;
    }

private SingleProofreadingError createDosError(String string, String text){

        SingleProofreadingError error = new SingleProofreadingError();
        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(string);
        int count=0;


        int start=0;
        int end=0;
        while (m.find()) {
            if (count==2){
                 start=Integer.parseInt(m.group().replaceAll("^\"|\"$", ""));
                 error.nErrorStart=start;
            }
            else if(count==4){
                 end=Integer.parseInt(m.group().replaceAll("^\"|\"$", ""));
                 error.nErrorLength=(end-start)+1;
                 error.aRuleIdentifier=text.substring(error.nErrorStart, (end+1));
            }
           else if(count==7){
                 error.aFullComment=m.group();
                 if (error.aFullComment.contains("Do you mean")){
                     String sug= error.aFullComment.replace("Do you mean /","").replace("/?","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("You should use")){
                     String sug= error.aFullComment.replace("You should use /","").replace("/ here instead","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Derived incorrectly from the root")){
                     String sug= error.aFullComment.replace("Derived incorrectly from the root /","").replace("/","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Valid word but")){
                     String sug= error.aFullComment.replace("Valid word but /","").replace("/ is more common","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Non-standard form of")){
                     String sug= error.aFullComment.replace("Non-standard form of /","").replace("/","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }


            }
            else if (count==5){
                error.aShortComment=m.group().replaceAll("^\"|\"$", "").trim();
            }/*
            else if (count==8){
                //error.nErrorLength=Integer.parseInt(m.group().replaceAll("^\"|\"$", ""));
            }*/

            count++;
         }
        return error;
    }

}

