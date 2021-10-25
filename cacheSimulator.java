import java.io.*;
import java.math.BigInteger;

class cacheSimulator {
    // <CACHE_SIZE> <ASSOC> <REPLACEMENT> <WB>  <TRACE_FILE>  
    static int[][] LRU;
    static int[][] FIFO;
    static int[][] dirtyBit;
    static long[][] tagBits;
    static int numMisses = 0;
    static int numHits = 0;
    static int numOperations = 0;
    static int numMemoryReads = 0;
    static int numMemoryWrites = 0;

    static void simulation(int cache_size, int assoc, int replacement, int wb, String trace_file, int block_size) throws IOException {
        String replacement_algorithm;
        String line;
        // BigInteger cacheAddress;
        Long cacheAddress;
        int val, setNumber, num_sets, block = 0;
        boolean isMiss;
        double missRatio = 0.0;

        File file = new File(trace_file);
        BufferedReader trace = new BufferedReader(new FileReader(file));

        if (replacement == 1) {
            replacement_algorithm = "FIFO";
        } else {
            replacement_algorithm = "LRU";
        }

        // Num set definition
        num_sets = cache_size / (assoc * block_size);

        if (num_sets == 0) {
            System.out.println("Error: Set size is equal to 0");
            return;
        }

        LRU = new int[num_sets][assoc];
        FIFO = new int[num_sets][assoc];
        dirtyBit = new int[num_sets][assoc];
        tagBits = new long[num_sets][assoc];

        if (LRU == null || FIFO == null || dirtyBit == null || tagBits == null) {
            System.out.println("Error: unable to allocate cache");
            return;
        }

        while ((line = trace.readLine()) != null) {
            char operation = line.charAt(0);
            String address = line.substring(4, line.length() - 1);
            boolean isWriteBack = (wb == 1) ? true : false;
            cacheAddress = Long.parseLong(address); //new BigInteger(address, 16);
            setNumber = (int)(cacheAddress / block_size) % num_sets;
            isMiss = true;

            // For every associative cache
            for (int i = 0; i < assoc; i++) {
                block = (int)(cacheAddress / block_size);
                
                // If the current address is in the cache 
                if (tagBits[setNumber][i] ==  block) {
                    isMiss = false;
                    numHits++;
                    numOperations++;

                    if (isWriteBack) {
                        dirtyBit[setNumber][i] = 1;
                    } else {
                        numMemoryWrites++;
                    }

                }
            }
            if (isMiss) {
                numMisses++;
                numOperations++;
                numMemoryReads++;
                if (operation == 'W' && !isWriteBack){
                    numMemoryWrites++;
                }

                if (replacement_algorithm == "LRU") {
                    // Make Helper Function for LRU
                    LRUAlgorithm(assoc, setNumber, operation, isWriteBack, block);
                    // Maybe Make some global varirbles or class variables
                } else {
                    // Make helper function for FIFO
                    FIFOAlgorithm(assoc, setNumber, operation, isWriteBack, block);
                }
                    
            }
        }   
        missRatio = (numMisses / (double)numOperations);
        System.out.println("Miss Ratio: " + missRatio);
        System.out.println("Number of Misses: " + numMisses);
        System.out.println("Number of Hits: " + numHits);
        System.out.println("Number of Writes to Memory: " + numMemoryWrites);
        System.out.println("Number of Reads from Memory: " + numMemoryReads);

        // Print operations and polish everything
        trace.close();
    
    }
    public static void LRUAlgorithm(int assoc, int setNumber, char operation, boolean isWriteBack, int block) {
        int index = 0;
        for (int i = 0; i < assoc; i++) {
            if (LRU[setNumber][i] >= LRU[setNumber][index]){
                index = i;
            }
        }

        // Checks dirty bit to see if a memory write needs to occur
        // This only occurs on write-back policy
        if (dirtyBit[setNumber][index] == 1){
            numMemoryWrites++;
            dirtyBit[setNumber][index] = 0;
        }

        // Tests if to replace the dirty bit (a new write operation is taking place) 
        if (operation == 'W' && isWriteBack) {
            dirtyBit[setNumber][index] = 1;
        }

        // Replaces the cache tag with new tag
        tagBits[setNumber][index] = block;

        // changes lru to match current state
        for (int i = 0; i < assoc; i++) {
            LRU[setNumber][i]++;
        }
        
        LRU[setNumber][index] = 0;

    }
    public static void FIFOAlgorithm(int assoc, int setNumber, char operation, boolean isWriteBack, int block) {
        int index = 0;
        for (int i = 0; i < assoc; i++) {
            if (FIFO[setNumber][i] >= FIFO[setNumber][index]){
                index = i;
            }
        }
        // Checks dirty bit to see if a memory write needs to occur
        // This only occurs on write-back policy
        if (dirtyBit[setNumber][index] == 1){
            numMemoryWrites++;
            dirtyBit[setNumber][index] = 0;
        }
        // Tests if to replace the dirty bit (a new write operation is taking place) 
        if (operation == 'W' && isWriteBack) {
            dirtyBit[setNumber][index] = 1;
        }
        // Replaces the cache tag with new tag
        tagBits[setNumber][index] = block;
        // changes lru to match current state
        for (int i = 0; i < assoc; i++) {
            FIFO[setNumber][i]++;
        }
        
        FIFO[setNumber][index] = 0;

    }
   


   

    public static void main(String[] args) {
        int cache_size, assoc, replacement, wb;
        // Constant block_size;
        int block_size = 64;
        String trace_file;
        if (args.length < 5) {
            System.out.println("Error, too few arguments");
            return;
        }
        cache_size = Integer.parseInt(args[0]);
        assoc = Integer.parseInt(args[1]);
        replacement = Integer.parseInt(args[2]);
        wb = Integer.parseInt(args[3]);
        trace_file = args[4];

        if (replacement < 0 || replacement > 1 || wb < 0|| wb > 1) {
            System.out.println("Input Error");
            return;
        }
        try {
            simulation(cache_size, assoc,  replacement, wb, trace_file, block_size);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // System.out.println(cache_size + ", " + assoc + ", " + replacement + ", " + wb + ", " + trace_file);

    }
}