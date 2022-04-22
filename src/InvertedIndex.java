import java.io.*;
import java.util.*;

class DictEntry2 {

    public int doc_freq = 0; // number of documents that contain the term
    public int term_freq = 0; //number of times the term is mentioned in the collection
    public HashSet<Integer> postingList;

    DictEntry2() {
        postingList = new HashSet<>();
    }
}

class Index2 {

    Map<Integer, String> sources;  // store the doc_id and the file name
    HashMap<String, DictEntry2> index; // THe inverted index

    Index2() {
        sources = new HashMap<>();
        index = new HashMap<>();
    }

    public void printDictionary() {
        Iterator it = index.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            DictEntry2 dd = (DictEntry2) pair.getValue();
            HashSet<Integer> hset = dd.postingList;// (HashSet<Integer>) pair.getValue();
            System.out.print("** [" + pair.getKey() + "," + dd.doc_freq + "] <" + dd.term_freq + "> =--> ");
            Iterator<Integer> it2 = hset.iterator();
            while (it2.hasNext()) {
                System.out.print(it2.next() + ", ");
            }
            System.out.println("");
            //it.remove(); // avoids a ConcurrentModificationException
        }
        System.out.println("------------------------------------------------------");
        System.out.println("*** Number of terms = " + index.size());
    }

    public void buildIndex(String[] files) {
        int i = 0;
        for (String fileName : files) {
            try ( BufferedReader file = new BufferedReader(new FileReader(fileName))) {
                sources.put(i, fileName);
                String ln;
                while ((ln = file.readLine()) != null) {
                    String[] words = ln.split("\\W+");
                    for (String word : words) {
                        word = word.toLowerCase();
                        // check to see if the word is not in the dictionary
                        if (!index.containsKey(word)) {
                            index.put(word, new DictEntry2());
                        }
                        // add document id to the posting list
                        if (!index.get(word).postingList.contains(i)) {
                            index.get(word).doc_freq += 1; //set doc freq to the number of doc that contain the term
                            index.get(word).postingList.add(i); // add the posting to the posting:ist
                        }
                        //set the term_fteq in the collection
                        index.get(word).term_freq += 1;
                    }
                }
                printDictionary();
            } catch (IOException e) {
                System.out.println("File " + fileName + " not found. Skip it");
            }
            i++;
        }
    }



    //function get the intersect between two posting lists
    HashSet<Integer> intersect(HashSet<Integer> pL1, HashSet<Integer> pL2) {
        HashSet<Integer> answer = new HashSet<>();
        Iterator<Integer> iteratorP1 = pL1.iterator();
        Iterator<Integer> iteratorP2 = pL2.iterator();
        int docId1 = 0, docId2 = 0;

        if(iteratorP1.hasNext())
            docId1=iteratorP1.next();
        if(iteratorP2.hasNext())
            docId2=iteratorP2.next();

        iteratorP1 = pL1.iterator();
        iteratorP2 = pL2.iterator();
        while (true){
            if(docId1 == docId2 ){
                answer.add(docId1);
                if(iteratorP1.hasNext())
                    docId1=iteratorP1.next();
                if(iteratorP2.hasNext())
                    docId2=iteratorP2.next();
                if(!iteratorP1.hasNext() && !iteratorP2.hasNext())
                    break;
            }
            else if(docId1<docId2 && iteratorP1.hasNext()){
                docId1=iteratorP1.next();
            }
            else if(docId2<docId1 && iteratorP2.hasNext()){
                docId2=iteratorP2.next();
            }
            else break;
        }

        if(docId1==docId2)
            answer.add(docId1);
        return answer;
    }

    //function get the union between two posting lists
    HashSet<Integer> union(HashSet<Integer> pL1, HashSet<Integer> pL2) {
        HashSet<Integer> answer = new HashSet<>();
        Iterator<Integer> iteratorP1 = pL1.iterator();
        Iterator<Integer> iteratorP2 = pL2.iterator();
        int docId1 = 0, docId2 = 0;

        if (iteratorP1.hasNext())
            docId1 = iteratorP1.next();
        if (iteratorP2.hasNext())
            docId2 = iteratorP2.next();

        iteratorP1 = pL1.iterator();
        iteratorP2 = pL2.iterator();

        while (iteratorP1.hasNext()){
            answer.add(docId1);
            docId1=iteratorP1.next();
        }
        while (iteratorP2.hasNext()){
            answer.add(docId2);
            docId2=iteratorP2.next();
        }
        answer.add(docId1);
        answer.add(docId2);
        return answer;
    }

    //function get the complement of posting list
    HashSet<Integer> complement(HashSet<Integer> pL){
        HashSet<Integer> answer = new HashSet<>();
        Iterator<Integer> iteratorP = pL.iterator();
        int docId = 0;

        if (iteratorP.hasNext())
            docId = iteratorP.next();

        iteratorP = pL.iterator();

        sources.forEach((key, value) -> answer.add(key));

        while (iteratorP.hasNext()){
            answer.remove(docId);
            docId=iteratorP.next();
        }
        answer.remove(docId);
        return answer;
    }

    //function get the query and split the phrase into words
    // and call intersect, union and complement functions if necessary
    public void query(String phrase) {
        String[] words = phrase.split("\\W+");
        ArrayList<HashSet<Integer>>postingList=new ArrayList<>();
        ArrayList<String> booleanExpression=new ArrayList<>();

        System.out.println("All words:");
        //putting all posting list in array list
        // and all (and,or) in booleanExpression array list
        //and preforming complement if necessary
        for(int i=0;i<words.length;i++){
            if(words[i].equals("NOT")){
                i++;
                postingList.add(complement(index.get(words[i].toLowerCase()).postingList));
                System.out.println(words[i-1]+words[i]+": "+complement(index.get(words[i].toLowerCase()).postingList));
            }
            else if(words[i].equals("AND") || words[i].equals("OR"))
                booleanExpression.add(words[i]);
            else {
                postingList.add(index.get(words[i].toLowerCase()).postingList);
                System.out.println(words[i] + ": " + index.get(words[i].toLowerCase()).postingList);
            }
        }
        System.out.println("---------------------------");

        int i=0,booleanIetrator=0;
        HashSet<Integer> res=null;
        //preforming (and,or) if needed
        while (i<postingList.size()){
            if(i==0 && booleanExpression.size()>0){
                if(booleanExpression.get(booleanIetrator).equals("AND"))
                    res=intersect(postingList.get(i),postingList.get(i+1));
                if(booleanExpression.get(booleanIetrator).equals("OR"))
                    res=union(postingList.get(i),postingList.get(i+1));
                i+=2;
            }
            else if(i>0 && booleanExpression.size()>0){
                if(booleanExpression.get(booleanIetrator).equals("AND"))
                    res=intersect(res,postingList.get(i));
                if(booleanExpression.get(booleanIetrator).equals("OR"))
                    res=union(res,postingList.get(i));
                i++;
            }
            else if(postingList.size()==1){
                res=postingList.get(0);
                i++;
            }
            booleanIetrator++;
        }
        System.out.println("Result: "+res);
        System.out.println("--------------------------------------");
    }
}

public class InvertedIndex {

    public static void main(String[] args) throws IOException {
        Index2 index = new Index2();
        String phrase;
        String direct="..\\docs";

        index.buildIndex(new String[]{
                direct+"\\100.txt",
                direct+"\\101.txt",
                direct+"\\102.txt",
                direct+"\\103.txt",
                direct+"\\104.txt",
                direct+"\\105.txt",
                direct+"\\106.txt",
                direct+"\\107.txt",
                direct+"\\108.txt",
                direct+"\\109.txt"
        });


        // examples
        index.query("NOT mohamed");
        index.query("mohamed AND book");
        index.query("mohamed OR ahmed");
        index.query("mohamed OR ahmed AND book");
        index.query("mohamed AND NOT ahmed OR book");
        index.query("i AND NOT he OR she");

        //input
        do {
            System.out.println("Print search phrase: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            phrase = in.readLine();
            index.query(phrase);
        } while (!phrase.isEmpty());
    }
}


//        while (iteratorP1.hasNext() && iteratorP2.hasNext()){
//            if(docId1 == docId2){
//                answer.add(docId1);
//                docId1=iteratorP1.next();
//                docId2=iteratorP2.next();
//            }
//            else if(docId1<docId2){
//                docId1=iteratorP1.next();
//            }
//            else if(docId2<docId1){
//                docId2=iteratorP2.next();
//            }
//        }
//        while (!iteratorP1.hasNext() && iteratorP2.hasNext()){
//            if(docId1 == docId2){
//                answer.add(docId1);
//                docId2=iteratorP2.next();
//                break;
//            }
//            else if(docId2<docId1){
//                docId2=iteratorP2.next();
//            }
//            else break;
//        }
//        while (iteratorP1.hasNext() && !iteratorP2.hasNext()){
//            if(docId1 == docId2){
//                answer.add(docId1);
//                docId1=iteratorP1.next();
//                break;
//            }
//            else if(docId1<docId2){
//                docId1=iteratorP1.next();
//            }
//            else break;
//        }