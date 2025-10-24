#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int partition(int arr[], int begin, int end) {
	int pivot = arr[end];
	int i = begin - 1;

	for (int j = begin; j < end; j++) {
		if (arr[j] <= pivot) {
			i++;
			int tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
		}
	}

	int tmp = arr[i + 1];
	arr[i + 1] = arr[end];
	arr[end] = tmp;

	return i + 1;
}

void quickSort(int arr[], int begin, int end) {
	if (begin >= end){
		return;
	}

	int partitionIndex = partition(arr, begin, end);
	quickSort(arr, begin, partitionIndex - 1);
	quickSort(arr, partitionIndex + 1, end);
}

int main(int argc, char *argv[]) {
	if (argc < 2) {
		fprintf(stderr, "Usage: %s <filename>\n", argv[0]);
		return 1;
	}

	FILE *file = fopen(argv[1], "r");
	if (!file) {
		perror("Error opening file");
		return 1;
	}

	int *array = malloc(sizeof(int) * 1000000);
	if (!array) {
		perror("Memory allocation failed");
		fclose(file);
		return 1;
	}

	int count = 0;
	while (count < 1000000 && fscanf(file, "%d", &array[count]) == 1) {
		count++;
	}

	fclose(file);

	quickSort(array, 0, count - 1);

	free(array);
	return 0;
}