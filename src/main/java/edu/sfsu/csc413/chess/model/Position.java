package edu.sfsu.csc413.chess.model;

public record Position(int file, int rank) {
/* 	private int pFile;
	private int pRank;
	 */
	
	/* public enum RankDigits {
		ONE(1),
		TWO(2),
		THREE(3),
		FOUR(4),
		FIVE(5),
		SIX(6),
		SEVEN(7),
		EIGHT(8)
	} */
	
	/** Files and ranks both run 0..7. */
    public static final int BOARD_SIZE = 8;
	
	public Position {
		if (!isOnBoard(file, rank)) {
			throw new IllegalArgumentException(
					"Position off board: file=" + file + ", rank=" + rank);
		}
		/* pFile = file;
		pRank = rank; */
	}
	
/* 	public int file() {
		return pFile;
	}
	
	public int rank() {
		return pRank;
	}
	 */
    /** True when these raw coordinates name a real square. */
    public static boolean isOnBoard(int file, int rank) {
        return file >= 0 && file < BOARD_SIZE && rank >= 0 && rank < BOARD_SIZE;
    }
	
	public static Position parse(String algebraic) {
		if (algebraic.length() != 2) {
			throw new IllegalArgumentException("String is not formated correctly; it must have one letter a-h followed by one digit 1-8.");
		}
		int f;
		int r;
		try {
			/* Take the first slice of the string,
			 * convert to lowercase, and convert into a
			 * numerical representation, 0..7
			 */
			f = (int)algebraic.substring(0,1)
					.toLowerCase()
					.charAt(0)
					- 97;
			/* Take the second slice, subtract by one.
			 */
			r = Integer.parseInt(algebraic.substring(1,2)) - 1;
		} catch (Exception e) {
			throw new IllegalArgumentException("String is not formated correctly; it must have one letter a-h followed by one digit 1-8.");
		}
		return new Position(f,r);
    }

	public Position offsetOrNull(int fileDelta, int rankDelta) {
		//throw new UnsupportedOperationException("M0b: your turn");
		if (!isOnBoard(file+fileDelta, rank+rankDelta)) {
			return null;
		}
		return new Position(file+fileDelta, rank+rankDelta);
	}
	
    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
	
}