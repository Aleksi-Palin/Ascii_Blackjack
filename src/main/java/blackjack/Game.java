package blackjack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Game {

    private static boolean animationsEnabled;

    private static final Card HIDDEN_CARD = new Card(
        0,
        "Spades",
        String.join(System.lineSeparator(),
            " _____",
            "|#####|",
            "|#####|",
            "|#####|",
            "|#####|",
            "|#####|"
        )
    );

    public static void main(String[] args) {
        PopulateCards cardPopulator = new PopulateCards();
        Scanner scanner = new Scanner(System.in);
        boolean noAnimation = hasNoAnimationFlag(args);
        boolean forcedAnimation = hasAnimationFlag(args);
        boolean autoAnimation = Animator.supportsAnsi();
        animationsEnabled = !noAnimation && (forcedAnimation || autoAnimation);
        if (!animationsEnabled && !noAnimation && !forcedAnimation) {
            System.out.println("Animations off: not a terminal, use --anim to force");
        }

        int bankroll = 100;
        boolean playAgain = true;
        while (playAgain && bankroll > 0) {
            RoundResult result = playRound(cardPopulator, scanner, bankroll);
            bankroll = result.bankroll;
            if (result.quit || bankroll == 0) {
                break;
            }
            System.out.print("Play again? (y/n): ");
            String answer = readLine(scanner);
            if (answer == null) {
                break;
            }
            playAgain = answer.trim().equalsIgnoreCase("y");
        }
        if (bankroll == 0) {
            System.out.println("Out of chips");
        }
        scanner.close();
    }

    private static RoundResult playRound(
        PopulateCards cardPopulator,
        Scanner scanner,
        int bankroll
    ) {
        int bet = readBet(scanner, bankroll);
        if (bet < 0) {
            return new RoundResult(bankroll, true);
        }

        bankroll -= bet;
        Card[] deck = cardPopulator.createCards();
        shuffle(deck);
        int nextCard = 0;
        List<Card> playerHand = new ArrayList<>();
        List<Card> dealerHand = new ArrayList<>();

        Animator.startRound(animationsEnabled);
        dealerHand.add(deck[nextCard++]);
        animateDeal(cardPopulator, playerHand, dealerHand, false, false);
        playerHand.add(deck[nextCard++]);
        animateDeal(cardPopulator, playerHand, dealerHand, true, false);
        dealerHand.add(deck[nextCard++]);
        animateDeal(cardPopulator, playerHand, dealerHand, false, false);
        playerHand.add(deck[nextCard++]);
        animateDeal(cardPopulator, playerHand, dealerHand, true, false);
        showHands(cardPopulator, playerHand, dealerHand, bankroll, bet, false);

        int playerValue = handValue(playerHand);
        int dealerValue = handValue(dealerHand);
        boolean playerBlackjack = Rules.isBlackjack(playerHand, playerValue);
        boolean dealerBlackjack = Rules.isBlackjack(dealerHand, dealerValue);
        if (playerBlackjack || dealerBlackjack) {
            reveal(cardPopulator, playerHand, dealerHand);
            if (playerBlackjack && dealerBlackjack) {
                bankroll = Rules.settlePush(bankroll, bet);
                return finishRound(bankroll, "push",
                    "Push. Both hands have blackjack.");
            }
            if (playerBlackjack) {
                bankroll += Rules.blackjackPayout(bet);
                return finishRound(bankroll, "blackjack",
                    "Blackjack! You win 3:2 (payout "
                        + Rules.blackjackProfit(bet)
                        + (bet % 2 == 0 ? "" : ", rounded down")
                        + ")"
                    );
            }
            return finishRound(bankroll, "lose", "Dealer has blackjack. Dealer wins.");
        }

        boolean playerBust = false;
        boolean playerStanding = false;
        int wager = bet;
        while (!playerStanding) {
            if (handValue(playerHand) > 21) {
                playerBust = true;
                break;
            }

            boolean canDouble = playerHand.size() == 2 && bankroll >= bet;
            System.out.print(canDouble
                ? "Hit, stand or double? (h/s/d): "
                : "Hit or stand? (h/s): ");
            String choice = readLine(scanner);
            if (choice == null) {
                return new RoundResult(bankroll, true);
            }
            choice = choice.trim().toLowerCase();
            if ("h".equals(choice)) {
                playerHand.add(deck[nextCard++]);
                animateDeal(cardPopulator, playerHand, dealerHand, true, false);
                showHands(cardPopulator, playerHand, dealerHand, bankroll, wager, false);
            } else if ("s".equals(choice)) {
                playerStanding = true;
            } else if ("d".equals(choice) && canDouble) {
                bankroll -= bet;
                wager = bet * 2;
                playerHand.add(deck[nextCard++]);
                animateDeal(cardPopulator, playerHand, dealerHand, true, false);
                showHands(cardPopulator, playerHand, dealerHand, bankroll, wager, false);
                playerStanding = true;
            } else {
                System.out.println(canDouble
                    ? "Please enter h, s or d."
                    : "Please enter h to hit or s to stand.");
            }
        }

        playerBust = handValue(playerHand) > 21;
        if (!playerBust) {
            reveal(cardPopulator, playerHand, dealerHand);
            dealerTurn(
                cardPopulator,
                deck,
                playerHand,
                dealerHand,
                nextCard,
                bankroll,
                wager
            );
        } else {
            reveal(cardPopulator, playerHand, dealerHand);
        }

        return settleRound(playerHand, dealerHand, bankroll, wager);
    }

    private static void dealerTurn(
        PopulateCards cardPopulator,
        Card[] deck,
        List<Card> playerHand,
        List<Card> dealerHand,
        int nextCard,
        int bankroll,
        int wager
    ) {
        boolean dealerDrew = false;
        while (handValue(dealerHand) < 17) {
            dealerHand.add(deck[nextCard++]);
            animateDeal(cardPopulator, playerHand, dealerHand, false, true);
            dealerDrew = true;
        }
        if (!animationsEnabled && dealerDrew) {
            showHands(cardPopulator, playerHand, dealerHand, bankroll, wager, true);
        }
    }

    private static RoundResult settleRound(
        List<Card> playerHand,
        List<Card> dealerHand,
        int bankroll,
        int wager
    ) {
        Rules.Outcome roundOutcome = Rules.outcome(
            handValue(playerHand),
            handValue(dealerHand)
        );
        String outcome;
        String resultText;
        if (roundOutcome == Rules.Outcome.BUST) {
            outcome = "bust";
            resultText = "You bust. Dealer wins.";
        } else if (roundOutcome == Rules.Outcome.WIN) {
            bankroll = Rules.settleWin(bankroll, wager);
            outcome = "win";
            resultText = "You win!";
        } else if (roundOutcome == Rules.Outcome.LOSE) {
            outcome = "lose";
            resultText = "Dealer wins.";
        } else {
            bankroll = Rules.settlePush(bankroll, wager);
            outcome = "push";
            resultText = "Push. It is a tie.";
        }
        Animator.playOutcome(outcome, animationsEnabled);
        printResult(resultText, bankroll);
        return new RoundResult(bankroll, false);
    }

    private static RoundResult finishRound(
        int bankroll,
        String outcome,
        String resultText
    ) {
        Animator.playOutcome(outcome, animationsEnabled);
        printResult(resultText, bankroll);
        return new RoundResult(bankroll, false);
    }

    private static void reveal(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand
    ) {
        Animator.animateReveal(
            cardPopulator,
            playerHand,
            dealerHand,
            HIDDEN_CARD,
            handValue(playerHand),
            handValue(dealerHand),
            animationsEnabled
        );
    }

    private static int readBet(Scanner scanner, int bankroll) {
        while (true) {
            System.out.print("Bet (1-" + bankroll + ", q to quit): ");
            String input = readLine(scanner);
            if (input == null || input.trim().equalsIgnoreCase("q")) {
                return -1;
            }
            try {
                int bet = Integer.parseInt(input.trim());
                if (bet >= 1 && bet <= bankroll) {
                    return bet;
                }
            } catch (NumberFormatException exception) {
                // Invalid input is handled by the prompt below.
            }
            System.out.println("Enter a bet from 1 to " + bankroll + ".");
        }
    }

    private static String readLine(Scanner scanner) {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private static void animateDeal(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        boolean playerCard,
        boolean dealerRevealed
    ) {
        Animator.animateDeal(
            cardPopulator,
            playerHand,
            dealerHand,
            HIDDEN_CARD,
            playerCard,
            dealerRevealed,
            handValue(playerHand),
            dealerVisibleValue(dealerHand, dealerRevealed),
            animationsEnabled
        );
    }

    private static void showHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        int bankroll,
        int bet,
        boolean dealerRevealed
    ) {
        Animator.printHands(
            cardPopulator,
            playerHand,
            dealerHand,
            HIDDEN_CARD,
            dealerRevealed,
            handValue(playerHand),
            dealerRevealed ? handValue(dealerHand) : dealerVisibleValue(dealerHand, false),
            animationsEnabled
        );
        System.out.println("Bet: " + bet + "   Bankroll: " + bankroll);
    }

    private static int dealerVisibleValue(List<Card> dealerHand, boolean revealed) {
        if (revealed) {
            return handValue(dealerHand);
        }
        return dealerHand.isEmpty() ? 0 : handValue(List.of(dealerHand.get(0)));
    }

    private static boolean hasNoAnimationFlag(String[] args) {
        for (String arg : args) {
            if ("--no-anim".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnimationFlag(String[] args) {
        for (String arg : args) {
            if ("--anim".equals(arg)) {
                return true;
            }
        }
        return false;
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

    private static void printResult(String resultText, int bankroll) {
        System.out.println(resultText + " Bankroll: " + bankroll);
    }

    private static final class RoundResult {
        private final int bankroll;
        private final boolean quit;

        private RoundResult(int bankroll, boolean quit) {
            this.bankroll = bankroll;
            this.quit = quit;
        }
    }
}
