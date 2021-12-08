import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class InputData {
    private static int uniqueWordCount;
    private static HashMap<String, Integer> map;
    private static String filename;
    private static int[][] matrixA; // 0 is start of sentence
    private static int[][] matrixB;
    private static Random rand;
    private static ArrayList<String> arrayList;

    InputData(String filename) throws FileNotFoundException{
       this.filename = filename;
        uniqueWordCount = 0;
        this.map = new HashMap<>();
       countWords();
        this.arrayList = new ArrayList<String>(uniqueWordCount + 1);
        //System.out.println(arrayList.size());
        //System.out.println(uniqueWordCount);
        arrayInitialize();
        //System.out.println(arrayList.toString());
        matrixA = new int[uniqueWordCount+1][uniqueWordCount+2]; //note: extra column is to store row sum
        matrixB = new int[uniqueWordCount+1][uniqueWordCount+2];
        rand = new Random();
    }

    // 1) get a count of unique words, so we can make matrix
    // 2) map each unique word to an in from 1 -> numWords
    public static void countWords() throws FileNotFoundException{
        Scanner input = new Scanner(new File(filename));
        String current;
       while(input.hasNext()){
           current = input.next();
           current = current.toLowerCase();
           if (!map.containsKey(current)){ //we haven't come across word yet
               uniqueWordCount++;
               map.put(current, uniqueWordCount);
           }
       }
    }

    public static void arrayInitialize() throws FileNotFoundException{
        Scanner input = new Scanner(new File(filename));
        String current;
        int uniqueWordRunningSum = 0;
        arrayList.add(null);
        while(input.hasNext()) {
            current = input.next();
            current = current.toLowerCase();
            if (!arrayList.contains(current)) {
                uniqueWordRunningSum++;
                arrayList.add(current);
            }
        }
    }

    //Second traversal goals
    // 1) divide each line by tabs
    // 2) create correct matrix (A) and incorrect matrix (B)
    //      that contain the nubmer of times each word transition is made in correct and incorrect case
    public static void transitionCounts() throws FileNotFoundException{
        Scanner input = new Scanner(new File(filename));
        String currentLine;
        while(input.hasNextLine()){
            currentLine = input.nextLine();
            String[] splitArray = currentLine.split("\t"); // returns array with left and right
            String[] good = splitArray[0].split("\\s+"); // turn good and bad into lists
            String[] bad = splitArray[1].split("\\s+");
            trainModel(good, matrixA);
            trainModel(bad, matrixB);
        }
    }

    private static void trainModel(String[] sentence, int[][] matrix){
        //figure out what in the word is at
        int previousWord = 0;
        for (int i = 0; i < sentence.length; i++){
            int currentWord = map.get(sentence[i].toLowerCase());
            matrix[previousWord][currentWord] += 1; // increment count by 1
            matrix[previousWord][uniqueWordCount + 1] += 1; //increment row sum by 1
            previousWord = currentWord;
        }
    }

    //evaluates sentence using naive bayes probability
    // returns probability of being correct sentence
    private static BigDecimal evaluateSentenceCorrect(String[] sentence) {
        BigDecimal goodProbability = new BigDecimal(0.0); //
        BigDecimal badProbability = new BigDecimal(0.0);
        //P(Correct | Sequence) = P(Sentence | Correct) * P(Correct)/(P(S|C) + P(S|B))
        int previousWord = 0;
        for(int i = 0; i < sentence.length; i++){
            int word = 0;
            if(map.containsKey(sentence[i])) {
                word = map.get(sentence[i].toLowerCase());
            }
            int goodRowSum = matrixA[previousWord][uniqueWordCount + 1];
            int badRowSum = matrixB[previousWord][uniqueWordCount + 1];
            goodProbability = goodProbability.add(new BigDecimal(((double)matrixA[previousWord][word] + 1) / (double)(goodRowSum + 2))); //using laPlace smoothing
            badProbability = badProbability.add(new BigDecimal(((double)matrixB[previousWord][word] + 1) / (double)(badRowSum + 2)));
            previousWord = word;
        }
        return goodProbability.divide(goodProbability.add(badProbability), RoundingMode.HALF_UP);
    }


    //evaluates sentence using naive bayes probability
    // returns probability of being incorrect (non-english) sentence
    private static BigDecimal evaluateSentenceIncorrect(String[] sentence) {
        BigDecimal goodProbability = new BigDecimal(0.0); //
        BigDecimal badProbability = new BigDecimal(0.0);
        //P(Correct | Sequence) = P(Sentence | Correct) /(P(S|C) + P(S|B))
        int previousWord = 0;
        for(int i = 0; i < sentence.length; i++){
            int word = 0;
            if(map.containsKey(sentence[i])) {
                word = map.get(sentence[i].toLowerCase());
            }
            int goodRowSum = matrixA[previousWord][uniqueWordCount + 1];
            int badRowSum = matrixB[previousWord][uniqueWordCount + 1];
            goodProbability = goodProbability.add(new BigDecimal(((double)matrixA[previousWord][word] + 1) / (double)(goodRowSum + 2))); //using laPlace smoothing
            badProbability = badProbability.add(new BigDecimal(((double)matrixB[previousWord][word] + 1) / (double)(badRowSum + 2)));
            previousWord = word;
        }
        return badProbability.divide(goodProbability.add(badProbability), RoundingMode.HALF_UP);
    }



    public static void evaluateCorpus(String filename) throws FileNotFoundException{
        //go through file line by line
        //split by tab
        //compute probability english for both sides
        //prints 'A' to document if the first is english, and 'B' if the second is english
        Scanner input = new Scanner(new File(filename));
        PrintStream output = new PrintStream(new File("part1.txt"));
        String[] left =null;
        String[] right =null;
        while(input.hasNextLine()){
            String currentLine = input.nextLine();
            String[] splitArray = currentLine.split("\t"); // returns array with left and right
            left = splitArray[0].split("\\s+"); // turn good and bad into lists
            right = splitArray[1].split("\\s+");
            if (evaluateSentenceCorrect(left).compareTo(evaluateSentenceCorrect(right)) >= 0){
                output.println("A");
            } else {
                output.println("B");
            }
        }
    }

    //this method generates corrupted sentences from good sentences and outputs them
    //choses a transition (eg word 1 -> word 2) randomly
    //replace the second word with the transition that is most likely to be in matrixB
    //                                              compared to that of matrixA
    public static void badEnglish(String inputFile) throws FileNotFoundException{
        //go through file
        //split by tab
        //go through words in left sentence
        //choose a first word randomly, find the max difference btw matrixB and A
        Scanner input = new Scanner(new File(inputFile));
        PrintStream output = new PrintStream(new File("part2.txt"));


        while(input.hasNextLine()) {
            String currentLine = input.nextLine();
            String[] splitArray = currentLine.split("\t"); // returns array with left and right
            String[] left = splitArray[0].split("\\s+"); // turn good and bad into lists
            int word1 = rand.nextInt(left.length);
            if (left.length <= 1) {
                output.print(splitArray[0] + "\t" + "z");
            } else {
                if(word1 + 1 >= left.length) {
                    word1 = word1 - 1;
                }
                int word2 = word1 + 1;
                //prob Bad
                int maxBadWord = word2;
                double maxBadWordProb = 0; //actually the max probability DIFFERENCE btw good and bad
                for (int i = 1; i <= uniqueWordCount; i++) {
                    double currentBadWordProb = (double) matrixB[word1][i] / (double) matrixB[word1][uniqueWordCount + 1];
                    double currentGoodWordProb = (double) matrixA[word1][i] / (double) matrixA[word1][uniqueWordCount + 1];
                    double probDifference = currentBadWordProb - currentGoodWordProb;
                    if (probDifference > maxBadWordProb) {
                        maxBadWord = i;
                        maxBadWordProb = probDifference;
                    }
                }

                //edit sentence
                output.print(splitArray[0] + "\t");
                for (int i = 0; i <= word1; i++) {
                    output.print(left[i] + " ");
                }
                if (maxBadWord == map.get(left[word2].toLowerCase())) { //if max bad is same as original
                    String originalWord = left[word2];
                    if (originalWord.length() <= 1) {
                        output.print("x ");
                    } else {
                        for (int i = originalWord.length() / 2; i < originalWord.length(); i++) {
                            output.print(originalWord.charAt(i));
                        }
                        for (int j = 0; j < originalWord.length() / 2; j++) {
                            output.print(originalWord.charAt(j));
                        }
                        output.print(" ");
                    }
                } else { // MaxBadWord is different from OG word
                    output.print(arrayList.get(maxBadWord) + " ");
                }
                for (int i = word2 + 1; i < left.length; i++) {
                    output.print(left[i] + " ");
                }
                output.println();
            }
        }
    }



    public static void main(String[] args) throws FileNotFoundException {
        //InputData inputData = new InputData("train.txt");
        InputData inputData = new InputData("train.txt");
        //System.out.println(map.toString());
        inputData.transitionCounts();
        //System.out.println("unique words " + uniqueWordCount);

        inputData.evaluateCorpus("test.rand.txt");
        inputData.badEnglish("train.txt");


//        System.out.println(matrixA.length);
//        System.out.println(matrixA[0][31]);
//
//        System.out.println("Matrix A");
//        for(int i = 0 ; i <  uniqueWordCount + 1; i++){
//            for(int j = 0; j < uniqueWordCount + 2; j++){
//                System.out.print(matrixA[i][j] + ", ");
//            }
//            System.out.println();
//        }
//        System.out.println("Matrix B");
//        for(int i = 0 ; i < uniqueWordCount + 1; i++){
//            for(int j = 0; j < uniqueWordCount + 2; j++){
//                System.out.print(matrixB[i][j] + ", ");
//            }
//            System.out.println();
//        }


    }
}
