package edu.sfsu.csc413.chess.model;
import java.util.ArrayList;
import java.util.List;

public abstract class Piece {
    private final Color color;
    private final PieceType type;

    protected Piece( Color c, PieceType t) {
        color = c;
        type = t;
    }

    public Color color() {
        return color;
    }

    public PieceType type() {
        return type;
    }


    /** True if this piece could capture an enemy standing on {@code target}. */
    public boolean attacks(Board board, Position from, Position target) {
        for (Move move : pseudoLegalMoves(board, from)) {
            if (move.to().equals(target)) {
                return true;
            }
        }
        return false;
    }

    public abstract List<Move> pseudoLegalMoves(Board board, Position from);

    protected List<Move> slidingMoves(Board board, Position from, int[][] directions) {
        List<Move> moveList = new ArrayList<Move>();
        for (int[] ints : directions) {
            int fileDirection = ints[0];
            int rankDirection = ints[1];
            if (
                    (fileDirection < -1 || fileDirection > 1) ||
                            (rankDirection < -1 || rankDirection > 1)
            ) {
                throw new IllegalArgumentException("Illegal direction, values can only be 1, 0 or -1");
            }

            Move nextMove;
            Position nextPosition = from;
            while (true) {
                nextPosition = nextPosition.offsetOrNull(fileDirection, rankDirection);
                if (nextPosition == null) {
                    break;
                }
                Piece foundPiece = board.pieceAt(nextPosition);
                if (foundPiece != null) {
                    if (foundPiece.color() != this.color) {
                        System.out.print(this.symbol()+" "+foundPiece.color()+" "+this.color);
                        moveList.add(new Move(from, nextPosition, this, foundPiece, null));
                    }
                    break;
                } else {
                    moveList.add(new Move(from, nextPosition, this, null, null));
                }
            }


        }
        return moveList;
    }

    protected List<Move> steppingMoves(Board board, Position from, int[][] offsets) {
        List<Move> moveList = new ArrayList<Move>();
        for (int[] ints : offsets) {
            int fileOffset = ints[0];
            int rankOffset = ints[1];

            Move nextMove;
            Position nextPosition = from.offsetOrNull(fileOffset, rankOffset);
            if (nextPosition == null) {
                continue;
            }
            Piece foundPiece = board.pieceAt(nextPosition);
            if (foundPiece != null) {
                if (board.pieceAt(nextPosition).color() != this.color) {
                    //moveList.add(new Move(from, nextPosition, this, foundPiece, null));
                    moveList.add(Move.capture(from, nextPosition, this, foundPiece));
                }
            } else {
                //moveList.add(new Move(from, nextPosition, this, null, null));
                moveList.add(Move.quiet(from, nextPosition, this));
            }
        }
        return moveList;
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
