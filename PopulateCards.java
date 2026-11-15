
public class PopulateCards {

    private static final int CARD_WIDTH = 7;
    private static final int CARD_GAP = 3;
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String BLACK = "\u001B[90m";

    private static final String[] SUIT_NAMES = {
        "Hearts",
        "Spades",
        "Clubs",
        "Diamonds"
    };

    private static final String[][] SUIT_ART = {
        Suit.HEARTS,
        Suit.SPADES,
        Suit.CLUBS,
        Suit.DIAMONDS
    };

    public Card[] createCards() {
        Card[] cards = new Card[52];
        int cardIndex = 0;

        for (int suitIndex = 0; suitIndex < SUIT_ART.length; suitIndex++) {
            for (int rank = 1; rank <= 13; rank++) {
                String rankText = getRankText(rank);
                String art = createArt(SUIT_ART[suitIndex], rankText);

                cards[cardIndex] = new Card(
                    rank,
                    SUIT_NAMES[suitIndex],
                    art
                );
                cardIndex++;
            }
        }

        return cards;
    }

    private String getRankText(int rank) {
        if (rank == 1) {
            return "A";
        }
        if (rank == 11) {
            return "J";
        }
        if (rank == 12) {
            return "Q";
        }
        if (rank == 13) {
            return "K";
        }
        return String.valueOf(rank);
    }

    public void printCardsSideBySide(Card... cards) {
        String[][] cardLines = new String[cards.length][];
        int rowCount = 0;

        for (int cardIndex = 0; cardIndex < cards.length; cardIndex++) {
            cardLines[cardIndex] = cards[cardIndex].getArt().split("\\R");
            rowCount = Math.max(rowCount, cardLines[cardIndex].length);
        }

        for (int row = 0; row < rowCount; row++) {
            for (int cardIndex = 0; cardIndex < cards.length; cardIndex++) {
                String[] lines = cardLines[cardIndex];
                String line = row < lines.length ? lines[row] : "";
                System.out.print(colorLine(cards[cardIndex], line));
                printPadding(Math.max(0, CARD_WIDTH + CARD_GAP - line.length()));
            }
            System.out.println();
        }
    }

    public String[] getCardLines(Card card) {
        return card.getArt().split("\\R");
    }

    public String colorLine(Card card, String line) {
        String color = isRedSuit(card.getSuit()) ? RED : BLACK;
        String rank = getRankText(card.getRank());
        if (line.length() < 2
            || line.charAt(0) != '|'
            || line.charAt(line.length() - 1) != '|') {
            return line;
        }

        String inside = line.substring(1, line.length() - 1);
        int rankIndex = inside.indexOf(rank);

        if (rankIndex > 0) {
            return line;
        }

        if (rankIndex < 0) {
            return "|" + color + inside + RESET + "|";
        }

        return "|"
            + RESET
            + rank
            + color
            + inside.substring(rank.length())
            + RESET
            + "|";
    }

    private boolean isRedSuit(String suit) {
        return suit.equals("Hearts") || suit.equals("Diamonds");
    }

    private void printPadding(int count) {
        for (int index = 0; index < count; index++) {
            System.out.print(" ");
        }
    }

    private String createArt(String[] suitArt, String rank) {
        StringBuilder art = new StringBuilder();

        for (String template : suitArt) {
            int placeholderIndex = template.indexOf("{R}");
            String line = template.replace("{R}", rank);

            if (rank.length() > 1 && placeholderIndex >= 0) {
                if (placeholderIndex == 1) {
                    line = removeCharacter(line, line.length() - 2);
                } else {
                    line = removeCharacter(line, placeholderIndex - 1);
                }
            }

            art.append(line).append(System.lineSeparator());
        }

        return art.toString();
    }

    private String removeCharacter(String text, int index) {
        return text.substring(0, index) + text.substring(index + 1);
    }
}
