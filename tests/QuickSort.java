import java.io.*;
import java.util.*;

public class QuickSort {
    public static void quickSort(int arr[], int begin, int end) {
        if (begin >= end)
            return;
        int partitionIndex = partition(arr, begin, end);
        quickSort(arr, begin, partitionIndex - 1);
        quickSort(arr, partitionIndex + 1, end);
    }

    private static int partition(int arr[], int begin, int end) {
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

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java QuickSort <filename>");
            return;
        }

        int[] array = new int[100_000_000];
        int i = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    array[i++] = Integer.parseInt(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            return;
        } catch (NumberFormatException e) {
            System.err.println("Invalid number in the file: " + e.getMessage());
            return;
        }

        quickSort(array, 0, array.length - 1);
    }
}