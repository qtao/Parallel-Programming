#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <mpi.h>
#define INIT_TAG 1000
#define DATA_TAG_1 1001
#define DATA_TAG_2 1002
#define PRINT_TAG 1003
#define NUM_ITERATIONS 64

 /* Global constant data */
#define TOTAL_ELMS 256 
const int dimension = 16;     // assume a square grid
const int global_grid[ TOTAL_ELMS ] = { 0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	                                    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };

void print_grid(const int* grid, int num_rows);
int get_pos(int y_max, int y, int x);
int is_power_of_two(unsigned int x);
void send_data(const int* my_grid, int rank, int size, int portion, int num_elms);
void receive_data(int my_grid[], int rank, int size, int portion, int num_elms);

int main(int argc, char* argv[]) {
	int rank, size;
	MPI_Init(&argc, &argv);
	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
	MPI_Comm_size(MPI_COMM_WORLD, &size);
	int portion = dimension / size;
	int num_elms = portion * dimension;
	// The local process' grid contains its data as well as space for two
	// uninitialized rows (one "above" local data, one "below")
	int my_grid[num_elms + dimension * 2];
	if (rank == 0) {
		if (size < 0 || !is_power_of_two(size) || size > dimension) {
			printf(
				"The number of processes (%d) must be a positive power of 2 and less than %d.\n",
				size, dimension);
			return -1;
		}
		puts("Initial Grid");
		print_grid(global_grid, dimension);
		// Distribute portions to compute with
		memcpy(&my_grid[dimension], global_grid, num_elms * sizeof(int));
		for(int num = 1; num < size; num++) {
			int start_pos = num_elms * num;
			MPI_Send((void*)&global_grid[start_pos], num_elms, MPI_INT, num, INIT_TAG, MPI_COMM_WORLD);
		}
	} else {
		// Recieve portion of array to compute with if not driver process
		MPI_Recv(&my_grid[dimension], num_elms, MPI_INT, 0, INIT_TAG, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
	}

	for (int itr_num = 0; itr_num < NUM_ITERATIONS; itr_num++) {
		/*
			Upper Process (lower row numbers)
			Process
			Lower Process (higher row numbers)
		 */
		if (size > 1) {
			if (rank % 2 == 0) {
				send_data(my_grid, rank, size, portion, num_elms);
				receive_data(my_grid, rank, size, portion, num_elms);
			} else {
				receive_data(my_grid, rank, size, portion, num_elms);
				send_data(my_grid, rank, size, portion, num_elms);
			}
		} else {
			// If there is only one process, then don't send/receive
			// Since there is only one process, just copy data from local array
			memcpy(&my_grid[dimension + num_elms], &my_grid[dimension], sizeof(int) * dimension);
			memcpy(my_grid, &my_grid[num_elms], sizeof(int) * dimension);
		}

		int y_max = portion + 2; // used for get_pos() position wrapping
		int updated_grid[num_elms]; // used to store changes to the local data
		for (int row = 1; row < (portion + 1); row++) {
			for (int col = 0; col < dimension; col++) {
				int num_neighbors =
					my_grid[get_pos(y_max, row - 1, col - 1)] +
					my_grid[get_pos(y_max, row - 1, col    )] +
					my_grid[get_pos(y_max, row - 1, col + 1)] +
					my_grid[get_pos(y_max, row    , col - 1)] +
					my_grid[get_pos(y_max, row    , col + 1)] +
					my_grid[get_pos(y_max, row + 1, col - 1)] +
					my_grid[get_pos(y_max, row + 1, col    )] +
					my_grid[get_pos(y_max, row + 1, col + 1)];

				int alive = my_grid[get_pos(y_max, row, col)];
				if (num_neighbors < 2 || num_neighbors > 3) {
					alive = 0;
				} else if (num_neighbors == 3) {
					alive = 1;
				}
				updated_grid[get_pos(y_max, row - 1, col)] = alive;
			}
		}
		// copy updated grid to correct portion of my_grid
		memcpy(&my_grid[dimension], updated_grid, sizeof(int) * num_elms);

		if (rank == 0) {
			// collect data and print it
			printf("Iteration %d: updated grid\n", itr_num);
			print_grid(&my_grid[dimension], portion);
			for (int num = 1; num < size; num++) {
				int temp_grid[num_elms];
				MPI_Recv(&temp_grid, num_elms, MPI_INT, num, PRINT_TAG, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
				print_grid(temp_grid, portion);
			}
		} else {
			// send locally calculated data to the driver process to print
			MPI_Send(&my_grid[dimension], num_elms, MPI_INT, 0, PRINT_TAG, MPI_COMM_WORLD);
		}
	}

	MPI_Finalize();
    return 0;
}

void send_data(const int* my_grid, int rank, int size, int portion, int num_elms) {
	int upper_proc = rank - 1;
	if (upper_proc < 0) upper_proc = size - 1;
	int lower_proc = (rank + 1) % size;
	// Send upper then lower
	MPI_Send((void*)&my_grid[dimension], dimension, MPI_INT, upper_proc, DATA_TAG_1, MPI_COMM_WORLD);
	MPI_Send((void*)&my_grid[num_elms], dimension, MPI_INT, lower_proc, DATA_TAG_2, MPI_COMM_WORLD);
}

void receive_data(int my_grid[], int rank, int size, int portion, int num_elms) {
	int upper_proc = rank - 1;
	if (upper_proc < 0) upper_proc = size - 1;
	int lower_proc = (rank + 1) % size;
	// Receive upper then lower - upper being sent is recieved as lower
	MPI_Recv(&my_grid[num_elms + dimension], dimension, MPI_INT, lower_proc, DATA_TAG_1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
	MPI_Recv(my_grid, dimension, MPI_INT, upper_proc, DATA_TAG_2, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
}

void print_grid(const int* grid, int num_rows) {
	int total_size = dimension * num_rows;
	for (int i = 0; i < total_size; i++) {
		printf("%d ", grid[i]);
		if ((i + 1) % dimension == 0) {
			putchar('\n');
		}
	}
}

// Converts coordinates ("matrix" form) into the global array position 
int get_pos(int y_max, int y, int x) {
	// Check if wrapping is needed and if it is then silently do it
	if (x < 0) {
		x = dimension - 1;
	} else if (x >= dimension) {
		x = 0;
	}
	if (y < 0) {
		y = y_max - 1;
	} else if (y >= y_max) {
		y = 0;
	}
	// Return the global array position for these coordinates
	return (y * dimension) + x;
}

// Checks if an unsigned integer is a power of two
int is_power_of_two (unsigned int x) {
  return ((x != 0) && !(x & (x - 1)));
}