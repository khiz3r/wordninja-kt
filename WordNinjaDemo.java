import io.github.wordninja.WordNinja;

import java.util.List;
import java.util.Scanner;

public class WordNinjaDemo {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("WordNinja Demo — type a concatenated string to split it into words.");
        System.out.println("Type 'exit' to quit.\n");

        while (true) {
            System.out.print("Enter text: ");
            String input = scanner.nextLine();

            if (input == null || input.equalsIgnoreCase("exit")) {
                System.out.println("Bye!");
                break;
            }

            if (input.isBlank()) {
                System.out.println("(empty input, try again)\n");
                continue;
            }

            // Split into words
            List<String> tokens = WordNinja.getInstance().split(input);
            System.out.println("Split result   : " + tokens);

            // Bonus: word ratio + natural-language check
            double ratio = WordNinja.getInstance().wordRatio(input);
            boolean natural = WordNinja.getInstance().isNaturalLanguage(input);
            System.out.printf("Word ratio     : %.2f%n", ratio);
            System.out.println("Natural language? " + natural);
            System.out.println();
        }

        scanner.close();
    }
}