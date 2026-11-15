
public class Card {

    private int rank;
    
    private String suit;

    private String art;


    public Card(int rank, String suit, String art) {
        this.rank = rank;
        this.suit = suit;
        this.art = art;
    }


    public int getRank() {
        return rank;
    }
    

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }


    public String getArt() {
        return art;
    }


    public void setArt(String art) {
        this.art = art;
    }


    @Override
    public String toString() {
        return "Card [rank=" + rank + ", suit=" + suit + ", art=" + art + "]";
    }

    
    
    
}
