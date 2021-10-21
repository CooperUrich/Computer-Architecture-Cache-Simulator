import java.io.*;
import java.util.Scanner;

class cacheSimulator {
    // <CACHE_SIZE> <ASSOC> <REPLACEMENT> <WB>  <TRACE_FILE>  
    void simulation(int cache_size, int assoc, int replacement, int wb, String trace_file, int block_size) throws IOException {
        String replacement_algorithm;
        String line;
        long cacheAddress;
        int val, setNumber, num_sets;
        boolean isMiss;
        int numMisses = 0;
        int numHits = 0;
        int numMemoryReads = 0;
        int numMemoryWrites = 0;
        double miss_ratio = 0.0;
        int[][] LRU;
        int[][] FIFO;
        int[][] dirtyBit;
        long[][] tagBits;

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
             cacheAddress = Long.valueOf(address);
            
            setNumber = (int)(cacheAddress / block_size) % num_sets;

            isMiss = true;

            for (int i = 0; i < assoc; i++) {
                int temp = (int)cacheAddress / block_size;
                
                // If the current address is in the cache 
                if (tagBits[setNumber][i] ==  temp) {
                    isMiss = false;
                    numHits++;

                    if (isWriteBack) {
                        dirtyBit[setNumber][i] = 1;
                    } else {
                        numMemoryWrites++;
                    }

                }
            }

            if (isMiss) {
                numMisses++;
                numMemoryReads++;
                if (operation == 'W' && !isWriteBack)
                    numMemoryWrites++;
                    
            }

        }   


    
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

        System.out.println(cache_size + ", " + assoc + ", " + replacement + ", " + wb + ", " + trace_file);




    }
}