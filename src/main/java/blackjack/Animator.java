package blackjack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Animator {

    private static final String ANSI_HOME = "\u001B[H";
    private static final String ANSI_CLEAR = "\u001B[2J";
    private static final String ANSI_ERASE_LINE = "\u001B[K";
    private static final String ANSI_ERASE_BELOW = "\u001B[J";
    private static final String ANSI_HIDE_CURSOR = "\u001B[?25l";
    private static final String ANSI_SHOW_CURSOR = "\u001B[?25h";
    private static final int CARD_WIDTH = 7;
    private static final int CARD_GAP = 3;
    private static final int MAX_FRAME_WIDTH = 80;
    private static final int CARDS_PER_ROW = 6;
    private static final int ANIMATION_FRAMES = 8;

    private Animator() {
    }

    public static boolean supportsAnsi() {
        String term = System.getenv("TERM");
        if (System.console() == null || "dumb".equalsIgnoreCase(term)) {
            return false;
        }
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return hasEnvironment("WT_SESSION")
                || hasEnvironment("TERM_PROGRAM")
                || hasEnvironment("ConEmuANSI");
        }
        return true;
    }

    private static boolean hasEnvironment(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    public static void startRound(boolean animated) {
        if (animated) {
            System.out.print(ANSI_CLEAR + ANSI_HOME);
            System.out.flush();
        }
    }

    public static void printHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        Card hiddenCard,
        boolean dealerRevealed,
        int playerValue,
        int dealerValue,
        boolean animated
    ) {
        List<String> frame = buildHands(
            cardPopulator,
            playerHand,
            dealerHand,
            hiddenCard,
            dealerRevealed,
            false,
            null,
            0,
            dealerRevealed
                ? "Dealer's hand (" + dealerValue + "):"
                : "Dealer's visible card:",
            "Your hand (" + playerValue + "):",
            animated
        );
        printFrame(frame, animated);
    }

    public static void animateDeal(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        Card hiddenCard,
        boolean playerCard,
        boolean dealerRevealed,
        int playerValue,
        int dealerValue,
        boolean animated
    ) {
        if (!animated) {
            return;
        }

        runFrames(() -> {
            for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                int slide = (ANIMATION_FRAMES - 1 - frame) * 2;
                List<String> lines = buildHands(
                    cardPopulator,
                    playerHand,
                    dealerHand,
                    hiddenCard,
                    dealerRevealed,
                    playerCard,
                    playerCard ? playerHand.size() - 1 : dealerHand.size() - 1,
                    slide,
                    dealerRevealed
                        ? "Dealer's hand (" + dealerValue + "):"
                        : "Dealer's visible card:",
                    "Your hand (" + playerValue + "):",
                    true
                );
                printFrame(lines, true);
                pause(50);
            }
        });
    }

    public static void animateReveal(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        Card hiddenCard,
        int playerValue,
        int dealerValue,
        boolean animated
    ) {
        if (!animated) {
            printHands(
                cardPopulator,
                playerHand,
                dealerHand,
                hiddenCard,
                true,
                playerValue,
                dealerValue,
                false
            );
            return;
        }

        runFrames(() -> {
            List<String> hiddenLines = Arrays.asList(cardPopulator.getCardLines(hiddenCard));
            List<String> revealedLines = Arrays.asList(
                cardPopulator.getCardLines(dealerHand.get(1))
            );
            for (int frame = 0; frame < ANIMATION_FRAMES; frame++) {
                int width = frame < ANIMATION_FRAMES / 2
                    ? CARD_WIDTH - frame * 2
                    : (frame - ANIMATION_FRAMES / 2 + 1) * 2 - 1;
                width = Math.max(1, Math.min(CARD_WIDTH, width));
                List<String> holeLines = frame < ANIMATION_FRAMES / 2
                    ? squeeze(hiddenLines, width)
                    : squeeze(revealedLines, width);
                List<String> lines = buildHands(
                    cardPopulator,
                    playerHand,
                    dealerHand,
                    hiddenCard,
                    true,
                    false,
                    null,
                    0,
                    "Dealer's hand (" + dealerValue + "):",
                    "Your hand (" + playerValue + "):",
                    true,
                    holeLines
                );
                printFrame(lines, true);
                pause(50);
            }
        });
    }

    public static void playOutcome(String name, boolean animated) {
        if (!animated) {
            return;
        }
        try {
            List<String> lines = readAnimation(name);
            if (lines.isEmpty() || !lines.get(0).startsWith("fps=")) {
                return;
            }
            int fps = Integer.parseInt(lines.get(0).substring(4));
            if (fps <= 0) {
                return;
            }
            List<List<String>> frames = parseFrames(lines);
            if (frames.isEmpty() || frames.stream().anyMatch(Animator::invalidFrame)) {
                return;
            }
            runFrames(() -> {
                for (List<String> frame : frames) {
                    printFrame(frame, true);
                    pause(Math.max(1, 1000 / fps));
                }
            });
        } catch (IOException | NumberFormatException exception) {
            return;
        }
    }

    private static List<String> readAnimation(String name) throws IOException {
        String resourceName = "/anim/" + name + ".txt";
        InputStream input = Animator.class.getResourceAsStream(resourceName);
        if (input == null) {
            return List.of();
        }
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            return reader.lines().toList();
        }
    }

    public static List<String> squeeze(List<String> cardLines, int newWidth) {
        int width = Math.max(1, Math.min(CARD_WIDTH, newWidth));
        List<String> result = new ArrayList<>();
        int leftPadding = (CARD_WIDTH - width) / 2;
        for (String line : cardLines) {
            String source = padRight(line, CARD_WIDTH);
            int start = (CARD_WIDTH - width) / 2;
            String squeezed = source.substring(start, start + width);
            result.add(spaces(leftPadding) + squeezed + spaces(CARD_WIDTH - width - leftPadding));
        }
        return result;
    }

    private static List<List<String>> parseFrames(List<String> lines) {
        List<List<String>> frames = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            if ("=====".equals(lines.get(index))) {
                if (!current.isEmpty()) {
                    frames.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(lines.get(index));
            }
        }
        if (!current.isEmpty()) {
            frames.add(current);
        }
        return frames;
    }

    private static boolean exceedsFrameWidth(List<String> frame) {
        return frame.stream().anyMatch(line -> line.length() > MAX_FRAME_WIDTH);
    }

    private static boolean invalidFrame(List<String> frame) {
        return frame.isEmpty() || frame.size() > 12 || exceedsFrameWidth(frame);
    }

    private static List<String> buildHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        Card hiddenCard,
        boolean dealerRevealed,
        boolean playerCard,
        Integer animatedCardIndex,
        int slide,
        String dealerHeading,
        String playerHeading,
        boolean color,
        List<String> holeLines
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(dealerHeading);
        List<List<String>> dealerCards = new ArrayList<>();
        List<Card> dealerOwners = new ArrayList<>();
        for (int index = 0; index < dealerHand.size(); index++) {
            if (index == 1 && !dealerRevealed) {
                dealerCards.add(Arrays.asList(cardPopulator.getCardLines(hiddenCard)));
                dealerOwners.add(hiddenCard);
            } else if (index == 1 && holeLines != null) {
                dealerCards.add(holeLines);
                dealerOwners.add(dealerHand.get(index));
            } else {
                dealerCards.add(Arrays.asList(cardPopulator.getCardLines(dealerHand.get(index))));
                dealerOwners.add(dealerHand.get(index));
            }
        }
        addCardRows(
            lines,
            dealerCards,
            dealerOwners,
            playerCard ? null : animatedCardIndex,
            playerCard ? 0 : slide,
            cardPopulator,
            color
        );
        if (!playerHand.isEmpty()) {
            lines.add("");
            lines.add(playerHeading);
            List<List<String>> playerCards = new ArrayList<>();
            List<Card> playerOwners = new ArrayList<>();
            for (Card card : playerHand) {
                playerCards.add(Arrays.asList(cardPopulator.getCardLines(card)));
                playerOwners.add(card);
            }
            addCardRows(
                lines,
                playerCards,
                playerOwners,
                playerCard ? animatedCardIndex : null,
                playerCard ? slide : 0,
                cardPopulator,
                color
            );
        }
        return lines;
    }

    private static List<String> buildHands(
        PopulateCards cardPopulator,
        List<Card> playerHand,
        List<Card> dealerHand,
        Card hiddenCard,
        boolean dealerRevealed,
        boolean playerCard,
        Integer animatedCardIndex,
        int slide,
        String dealerHeading,
        String playerHeading,
        boolean color
    ) {
        return buildHands(
            cardPopulator,
            playerHand,
            dealerHand,
            hiddenCard,
            dealerRevealed,
            playerCard,
            animatedCardIndex,
            slide,
            dealerHeading,
            playerHeading,
            color,
            null
        );
    }

    private static void addCardRows(
        List<String> output,
        List<List<String>> cards,
        List<Card> cardOwners,
        Integer animatedCardIndex,
        int slide,
        PopulateCards cardPopulator,
        boolean color
    ) {
        for (int rowStart = 0; rowStart < cards.size(); rowStart += CARDS_PER_ROW) {
            int rowEnd = Math.min(cards.size(), rowStart + CARDS_PER_ROW);
            for (int lineIndex = 0; lineIndex < 6; lineIndex++) {
                StringBuilder line = new StringBuilder();
                for (int cardIndex = rowStart; cardIndex < rowEnd; cardIndex++) {
                    if (cardIndex > rowStart) {
                        line.append(spaces(CARD_GAP));
                    }
                    List<String> card = cards.get(cardIndex);
                    String cardLine = padRight(
                        lineIndex < card.size() ? card.get(lineIndex) : "",
                        CARD_WIDTH
                    );
                    if (animatedCardIndex != null && cardIndex == animatedCardIndex) {
                        line.append(spaces(slide));
                    }
                    line.append(color ? cardPopulator.colorLine(
                        cardOwners.get(cardIndex),
                        cardLine
                    ) : cardLine);
                }
                output.add(line.toString());
            }
        }
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + spaces(width - text.length());
    }

    private static String spaces(int count) {
        return " ".repeat(Math.max(0, count));
    }

    private static void printFrame(List<String> lines, boolean animated) {
        StringBuilder output = new StringBuilder();
        if (animated) {
            output.append(ANSI_HOME);
        }
        for (String line : lines) {
            output.append(line);
            if (animated) {
                output.append(ANSI_ERASE_LINE);
            }
            output.append(System.lineSeparator());
        }
        if (animated) {
            output.append(ANSI_ERASE_BELOW);
        }
        System.out.print(output);
        System.out.flush();
    }

    private static void runFrames(Runnable animation) {
        System.out.print(ANSI_HIDE_CURSOR);
        System.out.flush();
        try {
            animation.run();
        } finally {
            System.out.print(ANSI_SHOW_CURSOR);
            System.out.flush();
        }
    }

    private static void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
