import java.util.List;

public final class Rules {

    public enum Outcome {
        BUST,
        WIN,
        LOSE,
        PUSH
    }

    private Rules() {
    }

    public static boolean isBlackjack(List<Card> hand, int value) {
        return hand.size() == 2 && value == 21;
    }

    public static int blackjackPayout(int bet) {
        return bet + blackjackProfit(bet);
    }

    public static int blackjackProfit(int bet) {
        return bet * 3 / 2;
    }

    public static int settleWin(int bankroll, int wager) {
        return bankroll + wager * 2;
    }

    public static int settlePush(int bankroll, int wager) {
        return bankroll + wager;
    }

    public static Outcome outcome(int playerValue, int dealerValue) {
        if (playerValue > 21) {
            return Outcome.BUST;
        }
        if (dealerValue > 21 || playerValue > dealerValue) {
            return Outcome.WIN;
        }
        if (playerValue < dealerValue) {
            return Outcome.LOSE;
        }
        return Outcome.PUSH;
    }
}
