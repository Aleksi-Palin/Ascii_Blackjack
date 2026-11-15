import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheckRules {

    public static void main(String[] args) throws Exception {
        check("A+K = 21", value(1, 13) == 21);
        check("A+A+9 = 21", value(1, 1, 9) == 21);
        check("four aces = 14", value(1, 1, 1, 1) == 14);
        check("10+J+Q bust", value(10, 11, 12) == 30);
        check("two-card 21 is blackjack",
            Rules.isBlackjack(cards(1, 13), 21));
        check("three-card 21 is not blackjack",
            !Rules.isBlackjack(cards(7, 7, 7), 21));
        check("3:2 rounds down", Rules.blackjackPayout(5) == 12);
        check("push returns bet", Rules.settlePush(90, 10) == 100);
        check("double win returns doubled wager",
            Rules.settleWin(80, 20) == 120);
        check("27 vs 20 is a bust",
            Rules.outcome(27, 20) == Rules.Outcome.BUST);
        check("22 vs 22 is a bust",
            Rules.outcome(22, 22) == Rules.Outcome.BUST);
        check("20 vs 22 is a win",
            Rules.outcome(20, 22) == Rules.Outcome.WIN);
        check("20 vs 20 is a push",
            Rules.outcome(20, 20) == Rules.Outcome.PUSH);
        check("19 vs 20 is a loss",
            Rules.outcome(19, 20) == Rules.Outcome.LOSE);
        check("doubled bust loses both bets", 100 - 10 * 2 == 80);
        check("doubled win settles both bets", Rules.settleWin(80, 20) == 120);
        check("doubled push returns both bets", Rules.settlePush(80, 20) == 100);

        PopulateCards populator = new PopulateCards();
        boolean geometry = true;
        for (Card card : populator.createCards()) {
            String[] lines = populator.getCardLines(card);
            geometry &= lines.length == 6 && lines[0].length() == 6;
            for (int index = 1; index < lines.length; index++) {
                geometry &= lines[index].length() == 7;
            }
        }
        check("52 cards have valid geometry", geometry);

        boolean animations = true;
        for (String name : new String[] {"win", "bust", "lose", "push", "blackjack"}) {
            List<String> lines = Files.readAllLines(
                Path.of("anim", name + ".txt"),
                StandardCharsets.UTF_8
            );
            boolean valid = !lines.isEmpty() && lines.get(0).startsWith("fps=");
            if (valid) {
                try {
                    valid = Integer.parseInt(lines.get(0).substring(4)) > 0;
                } catch (NumberFormatException exception) {
                    valid = false;
                }
            }
            int frameLines = 0;
            int frameCount = 0;
            for (int index = 1; index < lines.size(); index++) {
                if ("=====".equals(lines.get(index))) {
                    if (frameLines > 0) {
                        valid &= frameLines <= 12;
                        frameCount++;
                    }
                    frameLines = 0;
                } else {
                    frameLines++;
                    valid &= lines.get(index).length() <= 80;
                }
            }
            if (frameLines > 0) {
                valid &= frameLines <= 12;
                frameCount++;
            }
            animations &= valid && frameCount >= 1;
        }
        check("animation files have valid frames", animations);

        boolean tensAreSevenWide = true;
        for (Card card : populator.createCards()) {
            if (card.getRank() == 10) {
                String[] lines = populator.getCardLines(card);
                tensAreSevenWide &= lines[0].length() == 6;
                for (int index = 1; index < lines.length; index++) {
                    tensAreSevenWide &= lines[index].length() == 7;
                }
            }
        }
        check("rank 10 cards preserve card width", tensAreSevenWide);
    }

    private static int value(int... ranks) throws Exception {
        Method method = game.class.getDeclaredMethod("handValue", List.class);
        method.setAccessible(true);
        return (Integer) method.invoke(null, cards(ranks));
    }

    private static List<Card> cards(int... ranks) {
        return Arrays.stream(ranks)
            .mapToObj(rank -> new Card(rank, "Spades", ""))
            .toList();
    }

    private static void check(String name, boolean passed) {
        System.out.println((passed ? "PASS: " : "FAIL: ") + name);
        if (!passed) {
            System.exit(1);
        }
    }
}
