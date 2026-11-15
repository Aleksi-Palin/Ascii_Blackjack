import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class game {

    public static void main(String[] args) {
        PopulateCards cardPopulator = new PopulateCards();
        Scanner scanner = new Scanner(System.in);
        boolean playAgain = true;

        while (playAgain) {
            playRound(cardPopulator, scanner);
            System.out.print("Play again? (y/n): ");
            playAgain = scanner.nextLine().trim().equalsIgnoreCase("y");
        }

        scanner.close();
    }

    private static void playRound(PopulateCards cardPopulator, Scanner scanner) {
        Card[] deck = cardPopulator.createCards();
        shuffle(deck);
        int nextCard = 0;

        List<Card> playerHand = new ArrayList<>();
        List<Card> dealerHand = new ArrayList<>();

        dealerHand.add(deck[nextCard++]);
        playerHand.add(deck[nextCard++]);
        dealerHand.add(deck[nextCard++]);
        playerHand.add(deck[nextCard++]);
        showHands(cardPopulator, playerHand, dealerHand);

        boolean playerStanding = false;
        boolean playerBust = false;

        while (!playerStanding) {
            showHands(cardPopulator, playerHand, dealerHand);

            if (handValue(playerHand) > 21) {
                playerBust = true;
                break;
            }

            System.out.print("Hit or stand? (h/s): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("h")) {
                playerHand.add(deck[nextCard++]);
            } else if (choice.equals("s")) {
                playerStanding = true;
            } else {
                System.out.println("Please enter h to hit or s to stand.");
            }
        }

        clearScreen();

        if (!playerBust) {
            showAllHands(cardPopulator, playerHand, dealerHand);
            pauseAfterDeal();

            while (handValue(dealerHand) < 17) {
                dealerHand.add(deck[nextCard++]);
                showAllHands(cardPopulator, playerHand, dealerHand);
                pauseAfterDeal();
            }
        }

        printResult(playerHand, dealerHand, playerBust);
    }

    private static void showHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand
    ) {
        clearScreen();

        if (!dealerHand.isEmpty()) {
            System.out.println("\nDealer's visible card:");
            cardPopulator.printCardsSideBySide(dealerHand.get(0));
        }

        if (!playerHand.isEmpty()) {
            System.out.println("\nYour hand (" + handValue(playerHand) + "):");
            cardPopulator.printCardsSideBySide(
                playerHand.toArray(new Card[0])
            );
        }
    }

    private static void showAllHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand
    ) {
        clearScreen();

        System.out.println("\nDealer's hand (" + handValue(dealerHand) + "):");
        cardPopulator.printCardsSideBySide(
            dealerHand.toArray(new Card[0])
        );

        System.out.println("\nYour hand (" + handValue(playerHand) + "):");
        cardPopulator.printCardsSideBySide(
            playerHand.toArray(new Card[0])
        );
    }

    private static void pauseAfterDeal() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void clearScreen() {
        System.out.print("\u001B[2J\u001B[3J\u001B[H");
        System.out.flush();
    }

    private static void shuffle(Card[] deck) {
        Random random = new Random();

        for (int i = deck.length - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            Card temporary = deck[i];
            deck[i] = deck[swapIndex];
            deck[swapIndex] = temporary;
        }
    }

    private static int handValue(List<Card> hand) {
        int value = 0;
        int aces = 0;

        for (Card card : hand) {
            int rank = card.getRank();
            if (rank == 1) {
                value += 11;
                aces++;
            } else {
                value += Math.min(rank, 10);
            }
        }

        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }

        return value;
    }

    private static void printResult(
        List<Card> playerHand,
        List<Card> dealerHand,
        boolean playerBust
    ) {
        int playerValue = handValue(playerHand);
        int dealerValue = handValue(dealerHand);

        if (playerBust) {
            System.out.println("You bust. Dealer wins.");
        } else if (dealerValue > 21 || playerValue > dealerValue) {
            System.out.println("You win!");
        } else if (playerValue < dealerValue) {
            System.out.println("Dealer wins.");
        } else {
            System.out.println("Push. It is a tie.");
        }
    }
}
