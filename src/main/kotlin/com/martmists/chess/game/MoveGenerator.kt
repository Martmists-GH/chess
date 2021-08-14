package com.martmists.chess.game

import java.util.*
import kotlin.math.abs

object MoveGenerator {
    private val cache = WeakHashMap<Board, MutableMap<Int, List<Move>>>()

    /**
     * Does not detect check or pins
     */
    fun findMovesSimple(board: Board, index: Int) : List<Move> {
        val piece = board.pieces[index]
        val possibleMoves = mutableListOf<Move>()

        when (piece.type) {
            PieceType.INVALID, PieceType.EMPTY -> return listOf()
            PieceType.PAWN -> {
                val moveDirection = if (piece.white) 10 else -10
                val nextRankLast = board.pieces[index + 2 * moveDirection].type == PieceType.INVALID

                val PROMOTION = listOf(PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN)

                // moves
                if (board.pieces[index + moveDirection].type == PieceType.EMPTY) {
                    if (nextRankLast) {
                        for (pr in PROMOTION) {
                            possibleMoves.add(Move(index, index + moveDirection, promoteTo = pr))
                        }
                    } else {
                        possibleMoves.add(Move(index, index + moveDirection))
                        if (!piece.moved && board.pieces[index + 2 * moveDirection].type == PieceType.EMPTY) {
                            possibleMoves.add(Move(index, index + 2 * moveDirection))
                        }

                        if (board.lastMove.fromIndex != -1 && board.pieces[board.lastMove.toIndex].type == PieceType.PAWN && abs(board.lastMove.toIndex - board.lastMove.fromIndex) == 20 && abs(board.lastMove.toIndex - index) == 1) {
                            possibleMoves.add(Move(index, board.lastMove.toIndex + moveDirection, enPassant = true))
                        }
                    }
                }

                // captures
                for (offset in listOf(-1, 1)) {
                    val p = board.pieces[index + moveDirection + offset]
                    if (p.type != PieceType.EMPTY && p.type != PieceType.INVALID && p.white != piece.white) {
                        if (nextRankLast) {
                            for (pr in PROMOTION) {
                                possibleMoves.add(Move(index, index + moveDirection + offset, promoteTo = pr))
                            }
                        } else {
                            possibleMoves.add(Move(index, index + moveDirection + offset))
                        }
                    }
                }
            }

            PieceType.KING -> {
                for (offset in listOf(-11, -10, -9, -1, 1, 9, 10, 11)) {
                    val p = board.pieces[index + offset]
                    if (p.type != PieceType.INVALID && (p.type == PieceType.EMPTY || p.white != piece.white)) {
                        possibleMoves.add(Move(index, index + offset))
                    }
                }

                if (!piece.moved && board.pieces[index - 1].type == PieceType.EMPTY && board.pieces[index - 2].type == PieceType.EMPTY && board.pieces[index - 3].let { it.type == PieceType.ROOK && !it.moved }) {
                    // castle short
                    possibleMoves.add(Move(index, index - 2, isCastle = true))
                }

                if (!piece.moved && board.pieces[index + 1].type == PieceType.EMPTY && board.pieces[index + 2].type == PieceType.EMPTY && board.pieces[index + 3].type == PieceType.EMPTY && board.pieces[index + 4].let { it.type == PieceType.ROOK && !it.moved }) {
                    // castle long
                    possibleMoves.add(Move(index, index + 2, isCastle = true))
                }
            }

            PieceType.KNIGHT -> {
                for (offset in listOf(-21, -19, -12, -8, 8, 12, 19, 21)) {
                    val p = board.pieces[index + offset]
                    if (p.type != PieceType.INVALID && (p.type == PieceType.EMPTY || p.white != piece.white)) {
                        possibleMoves.add(Move(index, index + offset))
                    }
                }
            }

            PieceType.BISHOP -> {
                for (offset in listOf(-11, -9, 9, 11)) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }

            PieceType.ROOK -> {
                for (offset in listOf(-10, -1, 1, 10)) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }

            PieceType.QUEEN -> {
                for (offset in listOf(-11, -10, -9, -1, 1, 9, 10, 11)) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }
        }

        return possibleMoves
    }

    fun isMate(board: Board) : Boolean {
        val whiteHasMoves = board.getPieces(board.whiteToMove).map { findMovesSmart(board, it) }.flatten().isNotEmpty()
        val blackHasCheck = board.getPieces(!board.whiteToMove).map { findMovesSimple(board, it) }.flatten()
            .any { board.pieces[it.toIndex].type == PieceType.KING }

        return !whiteHasMoves && blackHasCheck
    }

    fun isStalemate(board: Board) : Boolean {
        val whiteHasMoves = board.getPieces(board.whiteToMove).map { findMovesSmart(board, it) }.flatten().isNotEmpty()
        val blackHasCheck = board.getPieces(!board.whiteToMove).map { findMovesSimple(board, it) }.flatten()
            .any { board.pieces[it.toIndex].type == PieceType.KING }

        return !whiteHasMoves && !blackHasCheck
    }

    fun isCheck(board: Board) : Boolean {
        return board.getPieces(!board.whiteToMove).map { findMovesSimple(board, it) }.flatten().any { board.pieces[it.toIndex].type == PieceType.KING }
    }

    /**
     * Finds moves,
     * then for each move, checks if the opponent can take a king,
     * and discards it if possible
     */
    fun findMovesSmart(board: Board, index: Int) : List<Move> {
        val cached = cache.getOrPut(board) { WeakHashMap() }?.get(index)
        if (cached != null) {
            return cached
        }

        val p = board.pieces[index]

        val moves = findMovesSimple(board, index)
        val filtered = mutableListOf<Move>()

        for (move in moves) {
            if (move.isCastle) {
                val tmp1 = board.move(Move(-1, -1))
                val possibleStep1 = tmp1.getPieces(!p.white).map { findMovesSimple(tmp1, it) }.flatten()
                val tmp2 = board.move(Move(move.fromIndex, move.fromIndex + (move.toIndex - move.fromIndex) / 2))
                val possibleStep2 = tmp2.getPieces(!p.white).map { findMovesSimple(tmp2, it) }.flatten()
                val tmp3 = board.move(move)
                val possibleStep3 = tmp3.getPieces(!p.white).map { findMovesSimple(tmp3, it) }.flatten()

                if (
                    possibleStep1.none { itt -> tmp1.pieces[itt.toIndex].type == PieceType.KING } &&
                    possibleStep2.none { itt -> tmp2.pieces[itt.toIndex].type == PieceType.KING } &&
                    possibleStep3.none { itt -> tmp3.pieces[itt.toIndex].type == PieceType.KING }
                ) {
                    filtered.add(move)
                }

            } else {
                val new = board.move(move)
                val possible = new.getPieces(!p.white).map { findMovesSimple(new, it) }.flatten()
                if (possible.none { itt -> new.pieces[itt.toIndex].type == PieceType.KING }) {
                    filtered.add(move)
                }
            }
        }

        return filtered.also { cache[board]!![index] = filtered }
    }
}