package edu.sfsu.csc413.chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The pawn — the piece that breaks every rule the others follow.
 *
 * <p>It is the only piece that moves in just one direction, the only one whose
 * capture differs from its move, the only one with a special first move, and
 * the only one that turns into something else. It is worth noticing that all of
 * that awkwardness is contained in this one file. No other class in the engine
 * knows that pawns are strange. That containment is the payoff of polymorphism:
 * the irregular case costs one class, not a special case in every method that
 * touches a piece.
 *
 * <p>En passant is not handled here. Like castling, it depends on the previous
 * move rather than on the current board, so it waits for Week 15 when
 * {@code Game} owns the move history.
 */
public class Pawn extends Piece {

    /**
     * What a pawn may become on reaching the far rank.
     */
    private static final PieceType[] PROMOTION_CHOICES = { PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT };

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moveList = new ArrayList<Move>();
        Position oneForward = from.offsetOrNull(0, this.color().pawnDirection());
        Position startingJump = from.offsetOrNull(0,2*this.color().pawnDirection());
        if (
                (from.rank() == this.color().pawnStartRank()) &&
                (oneForward != null) &&
                (startingJump != null) &&
                (board.pieceAt(startingJump) == null) &&
                (board.pieceAt(oneForward) == null)
        ) {
            moveList.add(Move.quiet(from, startingJump, this));
        }

        Position diagonalLeft = from.offsetOrNull(-1, this.color().pawnDirection());
        if (diagonalLeft != null) {
            Piece pieceAtDiagonalLeft = board.pieceAt(diagonalLeft);
            if (pieceAtDiagonalLeft != null && pieceAtDiagonalLeft.color() != this.color()) {
                if (diagonalLeft.rank() == this.color().promotionRank()) {
                    for (PieceType type : PROMOTION_CHOICES) {
                        moveList.add(Move.promotion(
                                from, diagonalLeft, this, pieceAtDiagonalLeft, type
                        ));
                    }
                } else {
                    moveList.add(Move.capture(from, diagonalLeft, this, pieceAtDiagonalLeft));
                }
            }
        }
        Position diagonalRight = from.offsetOrNull(1, this.color().pawnDirection());
        if (diagonalRight != null) {
            Piece pieceAtDiagonalRight = board.pieceAt(diagonalRight);
            if (pieceAtDiagonalRight != null && pieceAtDiagonalRight.color() != this.color()) {
                if (diagonalRight.rank() == this.color().promotionRank()) {
                    for (PieceType type : PROMOTION_CHOICES) {
                        moveList.add(Move.promotion(
                                from, diagonalRight, this, pieceAtDiagonalRight, type
                        ));
                    }
                } else {
                    moveList.add(Move.capture(from, diagonalRight, this, pieceAtDiagonalRight));
                }
            }
        }

        if (oneForward != null) {
            if (board.pieceAt(oneForward) != null) {
                return moveList;
            }

            if (oneForward.rank() == this.color().promotionRank()) {
                for (PieceType type : PROMOTION_CHOICES) {
                    moveList.add(Move.promotion(from, oneForward, this, null, type));
                }
            } else {
                moveList.add(Move.quiet(from, oneForward, this));
            }

        }
        return moveList;
    }

    /**
     * A pawn attacks the two squares diagonally ahead of it, whether or not
     * anything stands there.
     *
     * <p>This override exists because the inherited version answers "can this
     * piece move to that square", and for a pawn that is the wrong question.
     * An empty square in front of a pawn is a square the pawn can move to but
     * does <em>not</em> attack — which matters enormously for king safety: a
     * king may not be blocked from a square merely because a pawn could advance
     * onto it, but it certainly may not step onto a square a pawn guards.
     */
    @Override
    public boolean attacks(Board board, Position from, Position target) {
        //throw new UnsupportedOperationException("M2: implement Pawn.attacks");
        Position diagonalLeft = from.offsetOrNull(-1, this.color().pawnDirection());
        Position diagonalRight = from.offsetOrNull(1, this.color().pawnDirection());
        return (diagonalLeft != null && diagonalLeft.equals(target))
                || (diagonalRight != null && diagonalRight.equals(target));
    }
}
