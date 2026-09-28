package edu.sfsu.csc413.chess.model;

public class Piece {
    private final Color color;
    private final PieceType type;

    public Piece( Color c, PieceType t) {
        color = c;
        type = t;
    }

    public Color color() {
        return color;
    }

    public PieceType type() {
        return type;
    }

    public char symbol() {
        char letter = type.symbol();
        return color == Color.WHITE ? letter : Character.toLowerCase(letter);
    }

    @Override
    public String toString() {
        return String.valueOf(symbol());
    }
}
