package com.martmists.chess.game

class Board {
    // 10x12 strategy
    val pieces = List(120) { Piece() }

    var lastMove: Move = Move(-1, -1)
    var whiteToMove = true
    var gameEnded = false

    fun getPieces(white: Boolean) : List<Int> {
        return (20 until 100).filter {
            val p = pieces[it]
            p.type != PieceType.INVALID && p.type != PieceType.EMPTY && p.white == white
        }
    }

    private val moveCache = HashMap<Int, Board>()

    fun move(mv: Move) : Board {
        if (gameEnded) {
            return this
        }

        val cached = moveCache[mv.hashCode()]
        if (cached != null) {
            return cached
        }

        if (mv.fromIndex == -1) {
            // Do nothing
            return copy().also {
                it.whiteToMove = !whiteToMove
            }
        }

        return copy().apply {
            whiteToMove = !this@Board.whiteToMove

            val piece = pieces[mv.fromIndex]
            val target = pieces[mv.toIndex]

            if (mv.isCastle) {
                val rook: Piece
                val rookTarget: Piece

                if (mv.fromIndex < mv.toIndex) {
                    // long castle
                    rook = pieces[mv.fromIndex + 4]
                    rookTarget = pieces[mv.fromIndex + 1]
                } else {
                    // short castle
                    rook = pieces[mv.fromIndex - 3]
                    rookTarget = pieces[mv.fromIndex - 1]
                }

                rookTarget.from(rook)
                rookTarget.moved = true
                target.from(piece)
                target.moved = true

                piece.empty()
                rook.empty()
            } else {
                if (mv.enPassant) {
                    val moveDirection = if (piece.white) 10 else -10
                    // captured pawn
                    pieces[mv.toIndex - moveDirection].empty()
                } else if (mv.promoteTo != PieceType.INVALID) {
                    piece.type = mv.promoteTo
                }

                target.from(piece)
                target.moved = true
                piece.empty()
            }

            lastMove = mv

            this@Board.moveCache[mv.hashCode()] = this
        }
    }

    fun checkState() {
        if (MoveGenerator.isMate(this)) {
            gameEnded = true
        } else if (MoveGenerator.isStalemate(this)) {
            gameEnded = true
        } else {
            // check for insufficient material
            val whitePieces = getPieces(true)
            val blackPieces = getPieces(false)
            if (whitePieces.count { pieces[it].type == PieceType.KNIGHT || pieces[it].type == PieceType.BISHOP } <= 1 && whitePieces.count() <= 2 &&
                blackPieces.count { pieces[it].type == PieceType.KNIGHT || pieces[it].type == PieceType.BISHOP } <= 1 && blackPieces.count() <= 2
            ) {
                gameEnded = true
            }
        }
    }

    fun copy() : Board {
        return Board().also {
            pieces.forEachIndexed { index, piece ->
                it.pieces[index].from(piece)
            }
        }
    }

    companion object {
        fun standard() : Board {
            return Board().also {
                it.pieces.forEachIndexed { index, piece ->
                    val row = index / 10
                    val col = index % 10
                    if (col in 1..8 && row in 2..9) {
                        when (row)  {
                            3, 8 -> piece.type = PieceType.PAWN
                            2, 9 -> {
                                when (col) {
                                    1, 8 -> piece.type = PieceType.ROOK
                                    2, 7 -> piece.type = PieceType.KNIGHT
                                    3, 6 -> piece.type = PieceType.BISHOP
                                    4 -> piece.type = PieceType.KING
                                    5 -> piece.type = PieceType.QUEEN
                                }
                            }
                            else -> piece.type = PieceType.EMPTY
                        }
                    }

                    if (row in 2..3) {
                        piece.white = true
                    }
                }
            }
        }
    }
}