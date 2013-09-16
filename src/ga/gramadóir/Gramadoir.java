/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ga.gramadóir;

import com.sun.star.linguistic2.ProofreadingResult;
import com.sun.star.linguistic2.SingleProofreadingError;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
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


    public ArrayList<SingleProofreadingError> checkGrammer(String text){
        errors=runGramadoir(text);
        
        return errors;
    }

    public ArrayList<SingleProofreadingError> runGramadoir(String text){
        String perlCommand=new String("echo "+text+"  | /home/gramadoir/gram-ga.exe --ionchod=utf-8 --api");
        String [] commands = {"bash", "-c", perlCommand };
        String nextLine = "";
        try{
           errors=new ArrayList<SingleProofreadingError>();
           Process p = Runtime.getRuntime().exec(commands);
           InputStreamReader isr = new InputStreamReader(p.getInputStream());
	   BufferedReader br = new BufferedReader(isr);
           
           while (nextLine!= null){
               System.out.println(nextLine);
             if(!nextLine.startsWith("<error")){
                 nextLine=br.readLine();
                 continue;
             }
             else{
               error=parseErrorString(text, nextLine);
               errors.add(error);
               nextLine=br.readLine();
             }
           }
           isr.close();
           br.close();
        }catch(Exception e){
           showError(e);
       }
        //SingleProofreadingError nullError = new SingleProofreadingError();
        //errors.add(null);
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


}
