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

    private ArrayList<SingleProofreadingError> errorList;
    private SingleProofreadingError error, oldError;
    private List<String> ignoreOnceErrors= new ArrayList<String>();
    private List<String> ignoreRuleErrors;
    private int offset=0;
    private int beginOfLastError=0;
    private int endOfLastError=0;
    private boolean newParagraph=true;
    private SingleProofreadingError[] emptyError = new SingleProofreadingError[0];
    private String oldText="";
    private String oldErrorOutput="";





    public final SingleProofreadingError[] getError(ProofreadingResult prr){
        //.replaceAll("[^\\sa-zA-Z0-9áúíÉéóÓÁÍÚ,.-']","*");
       String sentence=prr.aText.substring(prr.nStartOfSentencePosition, (prr.nBehindEndOfSentencePosition)).replaceAll("’", "'");
       sentence="\""+sentence+"\"";
       sentence.replaceAll("\\\n", "");
       SingleProofreadingError[] error=new SingleProofreadingError[1];
       System.out.println("SEN :"+sentence);


       if(sentence.equals("")){
           return getEmptyError();
       }
       else{
           errorList=checkGrammer2(sentence, prr.nStartOfSentencePosition);
           if(errorList.size()==1)
             error=errorList.toArray(error);
       }

       if(error[0]==null){
           return getEmptyError();
       }
       else{
           //error[0].nErrorStart+=prr.nStartOfSentencePosition-1;
           return error;
       }
      
   }


   private SingleProofreadingError[] getEmptyError(){
       endOfLastError=0;
       beginOfLastError=0;
       newParagraph=true;
       return emptyError;
   }



 
    public ArrayList<SingleProofreadingError> checkGrammer2(String sentence, int offset){
       // sentence=sentence.replace("\'", "'");
        if(sentence.equals(oldText)){
            ignoreOnceErrors.add(oldErrorOutput);
        }

        this.offset=offset;
        String perlCommand=new String("echo "+sentence+"  | /opt/gramadoir/gram-ga.exe --ionchod=utf-8 --moltai --api");
        String [] commands = {"bash", "-c", perlCommand };
        String output = "";
        String s1="";
        System.out.println("SENTENCE: "+sentence);
        try{
            errorList=new ArrayList<SingleProofreadingError>();
            Process p = Runtime.getRuntime().exec(commands);
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
	    BufferedReader br = new BufferedReader(isr);
           
            while (output!= null){
                System.out.println("OP :"+output);
                if(!output.startsWith("<E")){
                    output=br.readLine();
                    continue;
                }
                else{
                    error=createDosError(output, sentence);
                    s1=output.substring(0, 45).concat(output.substring(output.length()-30, output.length()));
                    if((ignoreOnceErrors !=null) && (ignoreOnceErrors.contains(s1))){
                        output=br.readLine();
                        continue;
                    }
                    else{
                        errorList.add(error);
                        break;
                    }
                }
            }
            isr.close();
            br.close();
        }catch(Exception e){
           showError(e);
            }
          oldErrorOutput=s1;
          oldText=sentence;
        return errorList;
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
                 error.nErrorStart=(start+offset);
            }
            else if(count==4){
                 end=Integer.parseInt(m.group().replaceAll("^\"|\"$", ""));
                 error.nErrorLength=(end-start)+1;
                 error.aRuleIdentifier=text.substring((start+1), (end+2)).trim();
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
                 else if(error.aFullComment.contains("Non-Standard form of")){
                     String sug= error.aFullComment.replace("Non-Standard form of /","").replace("/","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Foirm neamhchaighdeánach de")){
                     String sug= error.aFullComment.replace("Foirm neamhchaighdeánach de «","").replace("»","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("An raibh")){
                     String sug= error.aFullComment.replace("An raibh «","").replace("» ar intinn agat?","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Réamhlitir")){
                     String sug= error.aFullComment.replace("Réamhlitir «","").replace("» ar iarraidh","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Focal anaithnid: «")){
                     String str= error.aFullComment.replace("Focal anaithnid: «","").replace("»?","").replaceAll("^\"|\"$", "");
                     
                     String[] suggestions=str.split(", ");
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Focal ceart ach aimsítear é níos minice in ionad «")){
                     String sug= error.aFullComment.replace("Focal ceart ach aimsítear é níos minice in ionad «","").replace("»","").replaceAll("^\"|\"$", "");
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Séimhiú ar iarraidh")){
                     System.out.println("STRING: "+error.aRuleIdentifier);
                     int space=error.aRuleIdentifier.lastIndexOf(" ");

                     String s1= error.aRuleIdentifier.substring(0, space+2);
                     String s2= error.aRuleIdentifier.substring(space+2);
                     String sug =s1+"h"+s2;
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                  else if(error.aFullComment.contains("Séimhiú gan ghá")){
                     System.out.println("STRING: "+error.aRuleIdentifier);
                     int space=error.aRuleIdentifier.lastIndexOf(" ");

                     String s1= error.aRuleIdentifier.substring(0, space+2);
                     String s2= error.aRuleIdentifier.substring(space+3);
                     String sug =s1+s2;
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Bunaithe ar fhocal mílitrithe go coitianta")){
                     System.out.println("STRING: "+error.aRuleIdentifier);
                     String s=error.aFullComment;
                     String sug=s.substring((s.lastIndexOf("(")+1), s.lastIndexOf(")"));
                     String[] suggestions={sug};
                     error.aSuggestions = suggestions;
                 }
                 else if(error.aFullComment.contains("Urú nó séimhiú ar iarraidh")){
                     System.out.println("STRING: "+error.aRuleIdentifier);
                     int space=error.aRuleIdentifier.lastIndexOf(" ");


                     String s1= error.aRuleIdentifier.substring(0, space+2);
                     String s2= error.aRuleIdentifier.substring(space+2);
                     String s3= error.aRuleIdentifier.substring(0, space+1);
                     String s4= error.aRuleIdentifier.substring(space+1);

                     String[] suggestions = new String[2];

                     String sug2, sug1;
                     if(s1.endsWith("G")){
                        sug2 =s3+"n"+s4;
                     }
                     else if(s1.endsWith("c")){
                         sug2=s3+"g"+s4;
                     }
                     else {
                         sug2="";
                     }
                     suggestions[0]=s1+"h"+s2;
                     suggestions[1]=sug2;
                     

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

