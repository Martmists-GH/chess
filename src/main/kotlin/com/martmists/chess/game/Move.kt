package com.martmists.chess.game

class Move(val fromIndex: Int, val toIndex: Int, val isCastle: Boolean = false, val promoteTo: PieceType = PieceType.INVALID, val enPassant: Boolean = false) {
    // No check/mate or duplicate notations (e.g. Rab1 vs Rcb1)
    fun notation(board: Board) : String {
        if (isCastle) {
            return if (toIndex > fromIndex) "O-O-O" else "O-O"
        }

        val piece = board.pieces[fromIndex]
        val takes = board.pieces[toIndex].type != PieceType.EMPTY
        val duplicates = board.getPieces(piece.white).filter { board.pieces[it].type == piece.type }
            .map { MoveGenerator.findMovesSimple(board, it) }.flatten().filter { it.fromIndex != fromIndex && it.toIndex == toIndex }

        val source = if (duplicates.isNotEmpty()) {
            val dup = duplicates.first()
            if (file(dup.fromIndex) == file(fromIndex)) rank(fromIndex) else file(fromIndex)
        } else {
            ""
        }

        val state = board.move(this)

        val suffix = if (MoveGenerator.isMate(state)) {
            "#"
        } else if (MoveGenerator.isStalemate(state)) {
            "$"
        } else if (MoveGenerator.isCheck(state)) {
            "+"
        } else {
            ""
        }

        return when (piece.type) {
            PieceType.PAWN -> {
                (if (takes || enPassant) "${file(fromIndex)}x" else "") +
                    "${file(toIndex)}${rank(toIndex)}" +
                    (if (promoteTo != PieceType.INVALID) "=${piece(promoteTo)}" else "") +
                    suffix
            }
            else -> {
                "${piece(piece.type)}${source}" +
                    (if (takes) "x" else "") +
                    "${file(toIndex)}${rank(toIndex)}" +
                        suffix
            }
        }
    }

    override fun hashCode(): Int {
        return fromIndex + 100 * toIndex + 10000 * (isCastle.int() + 2 * enPassant.int() + 4*promoteTo.int())
    }

    companion object {
        fun piece(type: PieceType) : Char = when (type) {
            PieceType.BISHOP -> 'B'
            PieceType.KING -> 'K'
            PieceType.KNIGHT -> 'N'
            PieceType.QUEEN -> 'Q'
            PieceType.ROOK -> 'R'
            else -> throw IllegalArgumentException()
        }

        fun rank(index: Int) : Char = ((index / 10) - 1).digitToChar()

        fun file(index: Int) : Char {
            val files = "hgfedcba"
            val i = (index % 10) - 1
            return files[i]
        }
    }
}

private fun Boolean.int() = if (this) 1 else 0
private fun PieceType.int() = PieceType.values().indexOf(this)
