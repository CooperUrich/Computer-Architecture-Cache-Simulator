#include <iostream>
#include <string>
#include <fstream>
using namespace std;

void lru_algorithm(int, int, char, int, long long unsigned int, int);
void fifo_algorithm(int, int, char, int, long long unsigned int, int);
void virtual_cache(int, int, int, int, string);

long long unsigned int ** tag_addresses;
int ** fifo;
int ** lru;
int ** dirty_bit;
int miss_count = 0;
int hit_count = 0;
int operation_count = 0;
int mem_reads = 0;
int mem_writes = 0;
// Block size for cache simulation, change when necessary 
int block_size = 64;

void cache_simulator (int cache_size, int assoc, int replacement, int wb, string trace_file)
{
	string str, policy;
	char operation;
	int val, set_number, num_sets, idx;
	bool miss;
	double miss_ratio = 0.0;
	long long unsigned int cache_address, block_number;

	num_sets = cache_size/(assoc*block_size);
	
	if (num_sets <= 0) {
		throw "Can not allocate space for 0 num_sets";
		return;
	}

	// Open trace file
	ifstream trace(trace_file);

	if (!trace.is_open()) {
		throw "Error opening file";
	}

	// Associativity can not be less than 1
	if (assoc < 1) {
		throw "Associativity is invalid, recompile";
	}

	// create the all of the arrays to store all of the info
	fifo = new int *[num_sets];
	
	lru = new int *[num_sets];
	
	dirty_bit = new int *[num_sets];
	
	tag_addresses = new long long unsigned int*[num_sets];

	// Assign replacement policy string
	(replacement == 0) ? policy = "LRU" : policy = "FIFO";

	// Allocating all sets
	for (int i = 0; i < num_sets; i++){
		dirty_bit[i] = new int[assoc];
		
		lru[i] = new int[assoc];
		
		fifo[i] = new int[assoc];
		
		tag_addresses[i] = new long long unsigned int[assoc];
		
		// precautionary null checks
		if (dirty_bit[i] == NULL || tag_addresses[i] == NULL || fifo[i] == NULL || lru[i] == NULL )
			throw "Cache Memory could not be allocated.";
	}

	

	// Runs the cache on the trace file
	while (!trace.eof()) {
		
		// Read in Operation ('W' or 'R') and cache address
		trace >> operation >> str;
		
		// Convert the string to hex, and then long long int
		cache_address = stoull(str, 0, 16);
		
		// Calculates the set and block numbers
		block_number = (cache_address / block_size);
		set_number = block_number % num_sets;

		// cout << "Block: " << block_number << endl;
		// cout << "Cache Address: " << (cache_address  / block_size)<< endl;
		// Initialize to a miss, so it flags all hits
		miss = true;
		
		// Tests for cache hits
		for (int i = 0; i < assoc; i++){
			
			// If the block number is contained in the array, it is a hit
			if (block_number == tag_addresses[set_number][i]){
				
				miss = false;
				hit_count++;
				operation_count++;
				
				// Cache hits on write operations
				if (operation == 'W') {
					
					if (wb == 0) {
						mem_writes++;
					}

					else if (wb == 1) {
						dirty_bit[set_number][i] = 1;
					}
				}

				// Cache hit on LRU Replacement policy
				if (policy == "LRU"){
					
					idx = lru[set_number][i];

					for (int j = 0; j < assoc; j++){
						
						if (lru[set_number][j] <= idx) {
							lru[set_number][j]++;
						}

					}
					lru[set_number][i] = 0;
				}
			}
		}

		// Cache Miss
		if (miss) {
			// increment miss and operation for final ratio
			miss_count++;
			operation_count++;
			// We read from memory on every miss
			mem_reads++;

			// Tests for miss on write-through policy
			if (operation == 'W' && wb == 0) {
				mem_writes++;
			}

			// Cache miss on LRU Replacement policy
			if (policy == "LRU") {
				// Gets the index of the max value (least recently used)
				lru_algorithm(assoc, set_number, operation, wb, cache_address, block_number) ;
			}

			if (policy == "FIFO") {
				// Gets the position of the earliest element to be placed
				fifo_algorithm(assoc, set_number, operation, wb, cache_address, block_number);
			}

		}
		// On a chache hit
		if (!miss) {
			// Do nothing
		}
	}

	// Calculate Miss ration
	miss_ratio = (double)miss_count / (double)operation_count;

	// print out statistics
	cout << "Miss ratio:\t" << miss_ratio << endl;
	cout << "Memory reads:\t" <<  mem_reads << endl;
	cout << "Memory writes:\t" <<  mem_writes << endl;
	
	// Close trace file so file is not corrupted
	trace.close();
	return;
}

void fifo_algorithm(int assoc, int set_number, char op, int wb, long long unsigned int cache_address, int block_number) {
	int idx = 0;
	
	tag_addresses[set_number][idx] = block_number;
	for (int i = 0; i < assoc; i++) {
		
		if (fifo[set_number][i] >= fifo[set_number][idx]) {
			idx = i;
		}

	}
	if (dirty_bit[set_number][idx] == 1){
		mem_writes++;
		dirty_bit[set_number][idx] = 0;
	}
	if (op == 'W' && wb == 1) {
		dirty_bit[set_number][idx] = 1;
	}
	// increment the fifo structure to match state
	for (int i = 0; i < assoc; i++) {
		fifo[set_number][i]++;
	}

	// Set index to 0
	fifo[set_number][idx] = 0;
}

void lru_algorithm(int assoc, int set_number, char op, int wb, long long unsigned int cache_address, int block_number) {
	int idx = 0;
	for (int i = 0; i < assoc; i++) {
		// 
		if (lru[set_number][i] >= lru[set_number][idx]) {
			idx = i;
		}

	}

	// Check to see if there needs to be write to memory
	if (dirty_bit[set_number][idx] == 1) {
		// If so, increment the memory writes
		mem_writes++;
		// Write it to memory
		dirty_bit[set_number][idx] = 0;
	}

	// Tests if to replace the dirty bit (a new write operation is taking place) 
	if (op == 'W' && wb == 1) { 
		dirty_bit[set_number][idx] = 1;
	}

	// Replaces the cache tag with new tag
	tag_addresses[set_number][idx] = block_number;

	// changes lru to match current state
	for (int i = 0; i < assoc; i++) {
		lru[set_number][i]++;
	}

	// Set index to 0
	lru[set_number][idx] = 0;
}

int main (int argc, char ** argv)
{
	// Block size is a constant and is not affected by CL arguments
	int cache_size, assoc, replacement, wb;
	string trace_file;

	if (argc <= 5) {
		cout << "Not enough arguments. Please recompile with the required arguments" << endl;
		return 0;
	}

	// Take in arguments
	cache_size = atoi(argv[1]);
	
	assoc = atoi(argv[2]);
	
	replacement = atoi(argv[3]);
	
	wb = atoi(argv[4]);
	
	trace_file = argv[5];

	try {
		cache_simulator(cache_size, assoc, replacement, wb, trace_file);
	}
	catch (const char *e) {
		cout << e << endl;
	}

	return 0;
}
